package com.eerussianguy.blazemap.feature;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.event.BlazeRegistriesFrozenEvent;
import com.eerussianguy.blazemap.api.event.ComponentOrderingEvent.OverlayOrderingEvent;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.config.ServerConfig;
import com.eerussianguy.blazemap.engine.render.MapRenderer;
import com.eerussianguy.blazemap.feature.mapping.*;
import com.eerussianguy.blazemap.feature.maps.*;
import com.eerussianguy.blazemap.feature.overlays.EntityOverlay;
import com.eerussianguy.blazemap.feature.overlays.GridOverlay;
import com.eerussianguy.blazemap.feature.waypoints.*;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointService;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointServiceClient;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointServiceServer;
import com.eerussianguy.blazemap.lib.Helpers;
import com.mojang.blaze3d.platform.InputConstants;

public class BlazeMapFeaturesClient {
    private static final LinkedHashSet<BlazeRegistry.Key<Overlay>> MUT_OVERLAYS = new LinkedHashSet<>();
    public static final Set<BlazeRegistry.Key<Overlay>> OVERLAYS = Collections.unmodifiableSet(MUT_OVERLAYS);

    public static final KeyMapping KEY_MAPS = new KeyMapping("blazemap.key.maps", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, BlazeMap.MOD_NAME);
    public static final KeyMapping KEY_ZOOM = new KeyMapping("blazemap.key.zoom", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, BlazeMap.MOD_NAME);
    public static final KeyMapping KEY_WAYPOINTS = new KeyMapping("blazemap.key.waypoints", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, BlazeMap.MOD_NAME);

    private static boolean mapping = false;
    private static boolean maps = false;
    private static boolean waypoints = false;
    private static boolean overlays = false;

    public static boolean hasMapping() {
        return mapping;
    }

    public static boolean hasMaps() {
        return maps;
    }

    public static boolean hasWaypoints() {
        return waypoints &&
            (BlazeMapConfig.CLIENT.clientFeatures.displayWaypointsOnMap.get() ||
                BlazeMapConfig.CLIENT.clientFeatures.renderWaypointsInWorld.get());
    }

    public static boolean hasWaypointsOnMap() {
        return waypoints && BlazeMapConfig.CLIENT.clientFeatures.displayWaypointsOnMap.get();
    }

    public static boolean hasOverlays() {
        return overlays;
    }

    public static void initMapping() {
        BlazeMapAPI.LAYERS.register(new TerrainHeightLayer());
        BlazeMapAPI.LAYERS.register(new TerrainSlopeLayer());
        BlazeMapAPI.LAYERS.register(new WaterLevelLayer());
        BlazeMapAPI.LAYERS.register(new TerrainIsolinesLayer());
        BlazeMapAPI.LAYERS.register(new BlockColorLayer());
        BlazeMapAPI.LAYERS.register(new NetherLayer());

        BlazeMapAPI.MAPTYPES.register(new AerialViewMapType());
        BlazeMapAPI.MAPTYPES.register(new TopographyMapType());
        BlazeMapAPI.MAPTYPES.register(new NetherMapType());

        mapping = true;
    }

    public static void initOverlays() {
        BlazeMapAPI.OVERLAYS.register(new GridOverlay());
        BlazeMapAPI.OVERLAYS.register(new WaypointOverlay());
        BlazeMapAPI.OVERLAYS.register(new EntityOverlay.Players());
        BlazeMapAPI.OVERLAYS.register(new EntityOverlay.NPCs());
        BlazeMapAPI.OVERLAYS.register(new EntityOverlay.Animals());
        BlazeMapAPI.OVERLAYS.register(new EntityOverlay.Enemies());
        overlays = true;
    }

    public static void initMaps() {
        ClientRegistry.registerKeyBinding(KEY_MAPS);
        ClientRegistry.registerKeyBinding(KEY_ZOOM);
        ClientRegistry.registerKeyBinding(KEY_WAYPOINTS);

        BlazeMapAPI.OBJECT_RENDERERS.register(new DefaultObjectRenderer());

        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(MapRenderer::onDimensionChange);
        bus.addListener(MapRenderer::onMapLabelAdded);
        bus.addListener(MapRenderer::onMapLabelRemoved);
        bus.addListener(BlazeMapFeaturesClient::mapOverlays);
        bus.addListener(BlazeMapFeaturesClient::mapKeybinds);
        bus.addListener(BlazeMapFeaturesClient::mapMenu);

        maps = true;
    }

    private static void mapOverlays(BlazeRegistriesFrozenEvent evt) {
        OverlayOrderingEvent event = new OverlayOrderingEvent(MUT_OVERLAYS);
        event.add(BlazeMapReferences.Overlays.GRID);
        if(hasWaypointsOnMap()) {
            event.add(BlazeMapReferences.Overlays.WAYPOINTS);
        }
        event.add(
            BlazeMapReferences.Overlays.PLAYERS,
            BlazeMapReferences.Overlays.NPCS,
            BlazeMapReferences.Overlays.ANIMALS,
            BlazeMapReferences.Overlays.ENEMIES
        );
        MinecraftForge.EVENT_BUS.post(event);
        event.finish();
        overlays = MUT_OVERLAYS.size() > 0;
    }

    private static void mapKeybinds(InputEvent.KeyInputEvent evt) {
        if(KEY_MAPS.isDown()) {
            if(Screen.hasShiftDown()) {
                executeOrNotify(ServerConfig.MapAccess.READ_LIVE, MinimapOptionsGui::open);
            }
            else {
                executeOrNotify(ServerConfig.MapAccess.READ_STATIC, WorldMapGui::open);
            }
        }
        if(KEY_WAYPOINTS.isDown() && hasWaypoints()) {
            if(Screen.hasShiftDown()) {
                executeOrNotify(ServerConfig.MapAccess.READ_LIVE, () -> new WaypointManagerFragment().open());
            }
            else {
                executeOrNotify(ServerConfig.MapAccess.READ_LIVE, () -> new WaypointEditorFragment().open());
            }
        }
        if(KEY_ZOOM.isDown()) {
            if(BlazeMapConfig.SERVER.mapItemRequirement.canPlayerAccessMap(Helpers.getPlayer(), ServerConfig.MapAccess.READ_LIVE)) {
                if(Screen.hasShiftDown()) {
                    MinimapRenderer.INSTANCE.synchronizer.zoomOut();
                }
                else {
                    MinimapRenderer.INSTANCE.synchronizer.zoomIn();
                }
            }
        }
    }

    /** If the player has enough map access level, executes provided action. Otherwise notifies the player. */
    private static void executeOrNotify(ServerConfig.MapAccess access, Runnable action) {
        if(BlazeMapConfig.SERVER.mapItemRequirement.canPlayerAccessMap(Helpers.getPlayer(), access)) {
            action.run();
        } else {
            Helpers.getPlayer().displayClientMessage(Helpers.translate("blazemap.gui.notification.denied"), true);
        }
    }

    private static void mapMenu(MapMenuSetupEvent evt) {
        if(hasWaypoints()){
            evt.root.add(WorldMapMenu.waypoints(evt.blockPosX, evt.blockPosZ));
        }
        if(BlazeMapConfig.CLIENT.enableDebug.get()) {
            evt.root.add(WorldMapMenu.debug(evt.blockPosX, evt.blockPosZ, evt.chunkPosX, evt.chunkPosZ, evt.regionPosX, evt.regionPosZ));
        }
    }

    public static void initWaypoints() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.register(WaypointServiceClient.class);
        bus.register(WaypointServiceServer.class);

        WaypointRenderer.init();
        WaypointService.init();

        waypoints = true;
    }
}
