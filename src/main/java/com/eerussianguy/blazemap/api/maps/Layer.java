package com.eerussianguy.blazemap.api.maps;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.BlazeRegistry.RegistryEntry;
import com.eerussianguy.blazemap.api.pipeline.Consumer;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.IDataSource;
import com.mojang.blaze3d.platform.NativeImage;

/**
 * In Blaze Map, maps are composed of several layers superimposed on each others.
 * Layers read one or more MasterDatum objects previously collected by Collectors and use it
 * to generate a layer image for the respective chunk. These images are later stitched together
 * by the engine to generate a layer image for the whole region (LayerRegionTile).
 *
 * All operations are thread-safe by default (read data, paint image) and are executed in parallel
 * in the engine's background threads. Layers are meant exclusively to generate map tiles, for other
 * forms of data processing and analysis please use a Processor instead.
 *
 * @author LordFokas
 */
public abstract class Layer implements RegistryEntry, Consumer, IClientComponent {
    protected static final int OPAQUE = 0xFF000000;

    private final Key<Layer> id;
    private final Set<Key<DataType<MasterDatum>>> inputs;
    private final Component name;
    private final ResourceLocation icon;
    private final boolean opaque;

    @SafeVarargs
    public Layer(Key<Layer> id, Component name, Key<DataType<MasterDatum>>... inputs) {
        this.id = id;
        this.name = name;
        this.icon = null;
        this.inputs = Arrays.stream(inputs).collect(Collectors.toUnmodifiableSet());
        this.opaque = true;
    }

    @SafeVarargs
    public Layer(Key<Layer> id, Component name, ResourceLocation icon, Key<DataType<MasterDatum>>... inputs) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.inputs = Arrays.stream(inputs).collect(Collectors.toUnmodifiableSet());
        this.opaque = false;
    }

    @Override
    public Key<Layer> getID() {
        return id;
    }

    @Override
    public Set<Key<DataType<MasterDatum>>> getInputIDs() {
        return inputs;
    }

    @Override
    public boolean shouldRenderInDimension(ResourceKey<Level> dimension) {
        return true;
    }

    public final boolean isOpaque() {
        return opaque;
    }

    public abstract boolean renderTile(NativeImage tile, TileResolution resolution, IDataSource data, int xGridOffset, int zGridOffset);

    @Override
    public Component getName() {
        return name;
    }

    @Override
    public ResourceLocation getIcon() {
        return icon;
    }

    /**
     * Used by the World Map (fullscreen map) to display a legend in the bottom right corner.
     * The widget will be asked to render while translated to the corner of the screen,
     * so it must render backwards (towards the left and up) in order to stay inside the screen.
     *
     * The translation to the corner may subtract a small margin to make all legends have a consistent margin with the border.
     *
     * This only applies to opaque (bottom) layers, which are the first layer of the current map type,
     * however not all such layers must have one and returning null is the default action.
     */
    public Renderable getLegendWidget() {
        return null;
    }


    // =================================================================================================================
    // Common helper functions for easier layer rendering

    /**
     * Allows to run code once for each pixel of the chunk tile.
     * Automatically accounts for the fact chunk tile sizes vary with resolution.
     */
    protected static void foreachPixel(TileResolution resolution, PixelConsumer consumer) {
        // Note: It's more efficient overall to run the loops this way around with x as the inner loop.
        // This is for the same reason the data format is expected to be `data[z][x]` down below.
        // Namely, that the PNG tiles are constructed with the whole first row of x values read first,
        // then the whole second row of x values, etc.
        // The CPU cache can better optimise its memory fetches if they're accessed in order.
        for(int z = 0; z < resolution.chunkWidth; z++) {
            for(int x = 0; x < resolution.chunkWidth; x++) {
                consumer.accept(x, z);
            }
        }
    }

    @FunctionalInterface
    protected interface PixelConsumer {
        void accept(int x, int y);
    }

    /**
     * When running at lower resolutions than FULL, this utility allows to collect all data points that will be rendered
     * into a single pixel. Meant to be used with ArrayAggregator or similar utilities, to aggregate the multiple data
     * points into a single value.
     *
     * Note: Data is expected to be in the format `data[z][x]` for CPU cache fetch optimisation reasons.
     */
    protected static int[] relevantData(TileResolution resolution, int x, int z, int[][] data) {
        int[] objects = new int[resolution.pixelWidth * resolution.pixelWidth];
        x *= resolution.pixelWidth;
        z *= resolution.pixelWidth;
        int idx = 0;

        for(int dz = 0; dz < resolution.pixelWidth; dz++) {
            for(int dx = 0; dx < resolution.pixelWidth; dx++) {
                objects[idx++] = data[dz + z][dx + x];
            }
        }

        return objects;
    }

    /**
     * When running at lower resolutions than FULL, this utility allows to collect all data points that will be rendered
     * into a single pixel. Meant to be used with ArrayAggregator or similar utilities, to aggregate the multiple data
     * points into a single value.
     * 
     * Note: Data is expected to be in the format `data[z][x]` for CPU cache fetch optimisation reasons.
     */
    protected static float[] relevantData(TileResolution resolution, int x, int z, float[][] data) {
        float[] objects = new float[resolution.pixelWidth * resolution.pixelWidth];
        x *= resolution.pixelWidth;
        z *= resolution.pixelWidth;
        int idx = 0;

        for(int dz = 0; dz < resolution.pixelWidth; dz++) {
            for(int dx = 0; dx < resolution.pixelWidth; dx++) {
                objects[idx++] = data[dz + z][dx + x];
            }
        }

        return objects;
    }

    /**
     * When running at lower resolutions than FULL, this utility allows to collect all data points that will be rendered
     * into a single pixel. Meant to be used with ArrayAggregator or similar utilities, to aggregate the multiple data
     * points into a single value.
     * 
     * Note: Data is expected to be in the format `data[z][x]` for CPU cache fetch optimisation reasons.
     */
    @SuppressWarnings("unchecked")
    protected static <T> T[] relevantData(TileResolution resolution, int x, int z, T[][] data, Class<T> cls) {
        T[] objects = (T[]) Array.newInstance(cls, resolution.pixelWidth * resolution.pixelWidth);
        x *= resolution.pixelWidth;
        z *= resolution.pixelWidth;
        int idx = 0;

        for(int dz = 0; dz < resolution.pixelWidth; dz++) {
            for(int dx = 0; dx < resolution.pixelWidth; dx++) {
                objects[idx++] = data[dz + z][dx + x];
            }
        }

        return objects;
    }
}
