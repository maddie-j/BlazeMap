package com.eerussianguy.blazemap.feature.maps;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.mapping.Layer;
import com.eerussianguy.blazemap.api.mapping.MapType;
import com.eerussianguy.blazemap.api.util.IScreenSkipsMinimap;
import com.eerussianguy.blazemap.api.waypoint.Waypoint;
import com.eerussianguy.blazemap.feature.BlazeMapFeatures;
import com.eerussianguy.blazemap.gui.Image;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

public class WorldMapGui extends Screen implements IScreenSkipsMinimap, IMapHost {
    private static final TextComponent EMPTY = new TextComponent("");
    private static final ResourceLocation ICON = Helpers.identifier("textures/mod_icon.png");
    private static final ResourceLocation NAME = Helpers.identifier("textures/mod_name.png");
    private static final double MIN_ZOOM = 0.25, MAX_ZOOM = 16;
    private static boolean showWidgets = true;

    public static void open() {
        Minecraft.getInstance().setScreen(new WorldMapGui());
    }


    // =================================================================================================================


    private double zoom = 1;
    private final MapRenderer mapRenderer;

    public WorldMapGui() {
        super(EMPTY);
        mapRenderer = new MapRenderer(-1, -1, Helpers.identifier("dynamic/map/worldmap"), MIN_ZOOM, MAX_ZOOM);
    }

    @Override
    public boolean isLayerVisible(BlazeRegistry.Key<Layer> layerID) {
        return mapRenderer.isLayerVisible(layerID);
    }

    @Override
    public void toggleLayer(BlazeRegistry.Key<Layer> layerID) {
        mapRenderer.toggleLayer(layerID);
    }

    @Override
    public MapType getMapType() {
        return mapRenderer.getMapType();
    }

    @Override
    public void setMapType(MapType map) {
        mapRenderer.setMapType(map);
    }

    @Override
    protected void init() {
        double scale = getMinecraft().getWindow().getGuiScale();
        mapRenderer.resize((int) (width * scale), (int) (height * scale));

        ResourceKey<Level> dim = Minecraft.getInstance().level.dimension();
        addRenderableOnly(new Image(ICON, 5, 5, 20, 20));
        addRenderableOnly(new Image(NAME, 30, 5, 110, 20));
        int y = 20;
        for(BlazeRegistry.Key<MapType> key : BlazeMapAPI.MAPTYPES.keys()) {
            if(!key.value().shouldRenderInDimension(dim)) continue;
            int px = 7, py = (y+=20);
            addRenderableWidget(new MapTypeButton(px, py, 16, 16, key, this));
            MapType map = key.value();
            int layerX = px + 20;
            for(BlazeRegistry.Key<Layer> layer : map.getLayers()) {
                if(layer.value().isOpaque()) continue;
                LayerButton lb = new LayerButton(layerX, py, 16, 16, layer, map, this);
                layerX += 20;
                lb.checkVisible();
                addRenderableWidget(lb);
            }
        }
    }

    @Override
    public boolean mouseDragged(double cx, double cy, int button, double dx, double dy) {
        double scale = getMinecraft().getWindow().getGuiScale();
        mapRenderer.moveCenter(- (int) (dx * scale / zoom), - (int) (dy * scale / zoom));
        return super.mouseDragged(cx, cy, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        // zoom in or out by a factor of 2, and clamp the value between min and max zoom.
        double prevZoom = zoom;
        zoom = Helpers.clamp(MIN_ZOOM, zoom * (scroll > 0 ? 2 : 0.5), MAX_ZOOM);
        if(prevZoom == zoom) return false;
        return mapRenderer.setZoom(zoom);
    }

    @Override
    public void render(PoseStack stack, int i0, int i1, float f0) {
        float scale = (float) getMinecraft().getWindow().getGuiScale();
        fillGradient(stack, 0, 0, this.width, this.height, 0xFF333333, 0xFF333333);

        stack.pushPose();
        stack.scale(1F / scale, 1F / scale, 1);
        var buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        mapRenderer.render(stack, buffers);
        buffers.endBatch();
        stack.popPose();

        if(showWidgets) {
            stack.pushPose();
            super.render(stack, i0, i1, f0);
            stack.popPose();
        }
    }

    @Override
    public void onClose() {
        // MinimapRenderer.INSTANCE.setMapType(mapRenderer.getMapType());
        // List<String> layers = disabled.stream().map(BlazeRegistry.Key::toString).collect(Collectors.toList());
        // BlazeMapConfig.CLIENT.disabledLayers.set(layers);
        mapRenderer.close();
        super.onClose();
    }

    @Override
    public boolean keyPressed(int key, int x, int y) {
        if(key == BlazeMapFeatures.KEY_MAPS.getKey().getValue()) {
            this.onClose();
            return true;
        }

        if(key == GLFW.GLFW_KEY_F1) {
            showWidgets = !showWidgets;
            return true;
        }

        int dx = 0;
        int dz = 0;
        if(key == GLFW.GLFW_KEY_W) {
            dz -= 16;
        }
        if(key == GLFW.GLFW_KEY_S) {
            dz += 16;
        }
        if(key == GLFW.GLFW_KEY_D) {
            dx += 16;
        }
        if(key == GLFW.GLFW_KEY_A) {
            dx -= 16;
        }
        if(dx != 0 || dz != 0) {
            mapRenderer.moveCenter(dx, dz);
            return true;
        }
        return super.keyPressed(key, x, y);
    }

    @Override // TODO: this is debug code. Remove later.
    public boolean mouseClicked(double x, double y, int button) {
        if(button == GLFW.GLFW_MOUSE_BUTTON_3) {
            float scale = (float) getMinecraft().getWindow().getGuiScale();
            BlazeMapAPI.getWaypointStore().addWaypoint(new Waypoint(
                Helpers.identifier("waypoint-" + System.currentTimeMillis()),
                getMinecraft().level.dimension(),
                mapRenderer.fromBegin((int) (scale * x / zoom), 0, (int) (scale * y / zoom)),
                "Test"
            ).randomizeColor());
            mapRenderer.updateWaypoints();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }
}
