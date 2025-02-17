package com.eerussianguy.blazemap.feature.mapping;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.ArrayAggregator;
import com.eerussianguy.blazemap.api.util.IDataSource;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.platform.NativeImage;

public class BlockColorLayer extends Layer {

    public BlockColorLayer() {
        super(
            BlazeMapReferences.Layers.BLOCK_COLOR,
            Helpers.translate("blazemap.block_color"),
            // Helpers.identifier("textures/map_icons/layer_water.png"),

            BlazeMapReferences.MasterData.BLOCK_COLOR
        );
    }

    @Override
    public boolean renderTile(NativeImage tile, TileResolution resolution, IDataSource data, int xGridOffset, int zGridOffset) {
        BlockColorMD blocks = (BlockColorMD) data.get(BlazeMapReferences.MasterData.BLOCK_COLOR);
        if(blocks == null) return false;

        int[][] blockColors = blocks.colors;

        foreachPixel(resolution, (x, z) -> {
            int color = ArrayAggregator.avgColor(relevantData(resolution, x, z, blockColors));
            tile.setPixelRGBA(x, z, Colors.abgr(OPAQUE | color));
        });

        return true;
    }
}
