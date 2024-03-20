package com.eerussianguy.blazemap.engine.client;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.*;
import com.eerussianguy.blazemap.api.pipeline.*;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.*;
import com.eerussianguy.blazemap.engine.async.*;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.platform.NativeImage;

import static com.eerussianguy.blazemap.util.Profilers.Client.*;

class ClientPipeline extends Pipeline {
    private static final PipelineProfiler CLIENT_PIPELINE_PROFILER = new PipelineProfiler(
        COLLECTOR_TIME_PROFILER,
        COLLECTOR_LOAD_PROFILER,
        TRANSFORMER_TIME_PROFILER,
        TRANSFORMER_LOAD_PROFILER,
        PROCESSOR_TIME_PROFILER,
        PROCESSOR_LOAD_PROFILER
    );


    private final StorageAccess.Internal storage;
    public final StorageAccess addonStorage;
    public final Set<Key<MapType>> availableMapTypes;
    public final Set<Key<Layer>> availableLayers;
    private final Layer[] layers;
    public final int numLayers;
    private final Map<TileResolution, Map<Key<Layer>, LoadingCache<RegionPos, LayerRegionTile>>> tiles = new EnumMap<>(TileResolution.class);
    private final DebouncingDomain<LayerRegionTile> dirtyTiles;
    private final PriorityLock lock = new PriorityLock();
    private boolean active, cold;

    public ClientPipeline(AsyncChainRoot async, DebouncingThread debouncer, ResourceKey<Level> dimension, StorageAccess.Internal storage, PipelineType type) {
        super(
            async, debouncer, CLIENT_PIPELINE_PROFILER, dimension, Helpers::levelOrThrow,
            computeCollectorSet(dimension, type),
            BlazeMapAPI.TRANSFORMERS.keys().stream().filter(k -> k.value().shouldExecuteIn(dimension, type)).collect(Collectors.toUnmodifiableSet()),
            BlazeMapAPI.PROCESSORS.keys().stream().filter(k -> k.value().shouldExecuteIn(dimension, type)).collect(Collectors.toUnmodifiableSet())
        );
        this.storage = storage;
        this.addonStorage = storage.addon();

        // Set up views (immutable sets) for the available maps and layers.
        this.availableMapTypes = BlazeMapAPI.MAPTYPES.keys().stream().filter(m -> m.value().shouldRenderInDimension(dimension)).collect(Collectors.toUnmodifiableSet());
        this.availableLayers = availableMapTypes.stream().map(k -> k.value().getLayers()).flatMap(Set::stream).filter(l -> l.value().shouldRenderInDimension(dimension)).collect(Collectors.toUnmodifiableSet());
        this.layers = availableLayers.stream().map(Key::value).filter(l -> !(l instanceof FakeLayer)).toArray(Layer[]::new);
        this.numLayers = layers.length;

        // Set up debouncing mechanisms
        this.dirtyTiles = new DebouncingDomain<>(debouncer, region -> async.runOnDataThread(() -> {
            TILE_LOAD_PROFILER.hit();
            TILE_TIME_PROFILER.begin();
            region.save();
            TILE_TIME_PROFILER.end();
        }), 2500, 30000);
    }

    private static Set<Key<Collector<MasterDatum>>> computeCollectorSet(ResourceKey<Level> dimension, PipelineType type) {
        Stream<Key<Collector<MasterDatum>>> collectors = BlazeMapAPI.MAPTYPES.keys().stream().map(k -> k.value().getLayers()).flatMap(Set::stream)
            .map(k -> k.value().getInputIDs()).map(ids -> BlazeMapAPI.COLLECTORS.keys().stream().filter(k -> ids.contains(k.value().getOutputID()))
                .collect(Collectors.toUnmodifiableSet())).flatMap(Set::stream).filter(k -> k.value().shouldExecuteIn(dimension, type));

        if(!BlazeMapClientEngine.isClientSource()) {
            collectors = collectors.filter(k -> k.value() instanceof ClientOnlyCollector);
        }

        return collectors.collect(Collectors.toUnmodifiableSet());
    }

    public void setHot() {
        cold = false;
    }

    @Override
    public void insertMasterData(ChunkPos pos, List<MasterDatum> data) {
        AsyncChainItem<Void, Void> chain = async.begin();
        if(cold) chain = chain.thenDelay((int) (500 + ((System.nanoTime() / 1000) % 1000)));
        chain
            .thenOnGameThread($ -> {
                if(!active){ // When the pipeline is shut down, abort processing.
                    return Collections.EMPTY_LIST;
                }
                if(level.get().getChunkSource().hasChunk(pos.x, pos.z)) {
                    data.addAll(runCollectors(pos));
                }
                return data;
            })
            .thenOnDataThread(d -> processMasterData(pos, d))
            .execute();
    }

    // Redraw tiles based on MD changes
    // Check what MDs changed, mark dependent layers and processors as dirty
    // Ask layers to redraw tiles, if applicable:
    // - if tile was redrawn:
    // -  - mark dependent map types as changed
    // -  - update map files with new tile
    // -  - add LayerRegion to the list of updated images to send a notification for
    @Override
    @SuppressWarnings("rawtypes")
    protected void onPipelineOutput(ChunkPos chunkPos, Set<Key<DataType>> diff, MapView view, ChunkMDCache cache) {
        try {
            RegionPos regionPos = new RegionPos(chunkPos);
            Set<LayerRegion> updates = new HashSet<>();
            LAYER_LOAD_PROFILER.hit();
            LAYER_TIME_PROFILER.begin();

            for(Layer layer : layers) {
                if(Collections.disjoint(layer.getInputIDs(), diff)) continue;
                Key<Layer> layerID = layer.getID();

                for(TileResolution resolution : TileResolution.values()) {
                    NativeImage layerChunkTile = new NativeImage(NativeImage.Format.RGBA, resolution.chunkWidth, resolution.chunkWidth, true);
                    view.setFilter(UnsafeGenerics.stripKeys(layer.getInputIDs())); // the layer should only access declared collectors

                    // Calculate chunk grid offsets. Don't let negatives pass.
                    int xOff = chunkPos.x % resolution.pixelWidth;
                    int zOff = chunkPos.z % resolution.pixelWidth;
                    if(xOff < 0) xOff += resolution.pixelWidth;
                    if(zOff < 0) zOff += resolution.pixelWidth;

                    // only generate updates if the renderer populates the tile
                    // this is determined by the return value of renderTile being true
                    if(layer.renderTile(layerChunkTile, resolution, view, xOff, zOff)) {

                        // update this chunk of the region
                        LayerRegionTile layerRegionTile = getLayerRegionTile(layerID, regionPos, resolution, false);
                        layerRegionTile.updateTile(layerChunkTile, chunkPos);

                        // asynchronously save this region later
                        if(layerRegionTile.isDirty()) {
                            dirtyTiles.push(layerRegionTile);
                        }

                        // updates for the listeners
                        updates.add(new LayerRegion(layerID, regionPos));
                    }
                }
            }

            if(updates.size() > 0) {
                async.runOnGameThread(() -> sendMapUpdates(updates));
            }
        }
        finally {
            LAYER_TIME_PROFILER.end();
        }
    }

    private LayerRegionTile getLayerRegionTile(Key<Layer> layer, RegionPos region, TileResolution resolution, boolean priority) {
        if(priority) lock.lockPriority();
        else lock.lock();
        try {
            return tiles
                .computeIfAbsent(resolution, $ -> new HashMap<>())
                .computeIfAbsent(layer, $ -> CacheBuilder.newBuilder()
                    .maximumSize(256 * 1024 / resolution.regionSizeKb)
                    .expireAfterAccess(resolution.cacheTime, TimeUnit.SECONDS)
                    .removalListener(lrt -> ((LayerRegionTile) lrt.getValue()).destroy())
                    .build(new CacheLoader<>() {
                        @Override
                        public LayerRegionTile load(RegionPos pos) {
                            LayerRegionTile layerRegionTile = new LayerRegionTile(storage, layer, pos, resolution);
                            layerRegionTile.tryLoad();
                            return layerRegionTile;
                        }
                    })
                ).get(region);
        }
        catch(ExecutionException e) {
            // Should never happen as the loader code does not throw exceptions.
            throw new RuntimeException(e);
        }
        finally {
            lock.unlock();
        }
    }

    private void sendMapUpdates(Set<LayerRegion> updates) {
        if(active) {
            for(LayerRegion update : updates) {
                BlazeMapClientEngine.notifyLayerRegionChange(update);
            }
        }
    }

    public int getDirtyTiles() {
        return dirtyTiles.size();
    }

    public void shutdown() {
        active = false;
        dirtyChunks.clear();
        dirtyTiles.finish();
        tiles.values().forEach(r -> r.forEach((lr, c) -> c.invalidateAll()));
        tiles.clear();
    }

    public ClientPipeline activate() {
        active = true;
        cold = true;
        return this;
    }

    public void consumeTile(Key<Layer> layer, RegionPos region, TileResolution resolution, Consumer<NativeImage> consumer) {
        if(!availableLayers.contains(layer))
            throw new IllegalArgumentException("Layer " + layer + " not available for dimension " + dimension);
        getLayerRegionTile(layer, region, resolution, true).consume(consumer);
    }
}
