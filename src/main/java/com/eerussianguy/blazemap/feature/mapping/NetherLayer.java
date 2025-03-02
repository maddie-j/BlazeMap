package com.eerussianguy.blazemap.feature.mapping;

import java.awt.*;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainHeightMD;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.ArrayAggregator;
import com.eerussianguy.blazemap.api.util.DataSource;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.mojang.blaze3d.platform.NativeImage;

public class NetherLayer extends Layer {
    public NetherLayer() {
        super(
            BlazeMapReferences.Layers.NETHER,
            Helpers.translate("blazemap.nether_terrain"),
            BlazeMap.resource("textures/map_icons/layer_nether.png"),
            true,

            BlazeMapReferences.MasterData.NETHER
        );
    }

    private enum Gradient {
        CEILING(0.5f, new Color(0x9E9E9E)),
        HIGH_LEVEL(0.3f, new Color(0xFF6666)),
        MID_LEVEL(.5F, new Color(0X9C3A3A)),
        SHORE_LEVEL(0, new Color(0x7A672F)),
        LAVA_LEVEL(-0.05f, new Color(0xED6A28)),
        BEDROCK(-1F, new Color(0x784617));

        public static final Gradient[] VALUES = values();

        final float keypoint;
        final int color;

        Gradient(float keypoint, Color color) {
            this.keypoint = keypoint;
            // NativeImage colors are ABGR. Mojang has no standards. I blame Microsoft.
            this.color = Colors.abgr(color);
        }
    }

    @Override
    public boolean shouldRenderInDimension(ResourceKey<Level> dimension) {
        return dimension.equals(Level.NETHER);
    }

    @Override
    public boolean renderTile(NativeImage tile, TileResolution resolution, DataSource data, int xGridOffset, int zGridOffset) {
        TerrainHeightMD terrain = (TerrainHeightMD) data.get(BlazeMapReferences.MasterData.NETHER);
        float down = -1.0F / ((float) terrain.sea - terrain.minY);
        float up = 1.0F / ((float) terrain.maxY - terrain.sea);

        foreachPixel(resolution, (x, z) -> {
            int h = ArrayAggregator.avg(relevantData(resolution, x, z, terrain.heightmap));
            int height = h - terrain.sea;
            int depth = terrain.sea - h;
            float point = h == terrain.sea ? 0 : h < terrain.sea ? down * (depth) : up * (height);
            Gradient top = Gradient.CEILING;
            for(Gradient bottom : Gradient.VALUES) {
                float epsilon = bottom.keypoint - point;
                if(epsilon < 0.005F && epsilon > -0.005F) {
                    tile.setPixelRGBA(x, z, bottom.color);
                    return;
                }
                if(point > bottom.keypoint) {
                    tile.setPixelRGBA(x, z, Colors.interpolate(bottom.color, bottom.keypoint, top.color, top.keypoint, point));
                    break;
                }
                else {
                    top = bottom;
                }
            }
        });

        return true;
    }
}
