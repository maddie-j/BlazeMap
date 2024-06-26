package com.eerussianguy.blazemap.feature.mapping;

import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.pipeline.ClientOnlyCollector;

public class BlockColorCollector extends ClientOnlyCollector<BlockColorMD> {

    public BlockColorCollector() {
        super(
            BlazeMapReferences.Collectors.BLOCK_COLOR,
            BlazeMapReferences.MasterData.BLOCK_COLOR
        );
    }

    @Override
    public BlockColorMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {
        final int[][] colors = new int[16][16];
        final BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        final MutableBlockPos blockPos = new MutableBlockPos();

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                int color = getColorAtMapPixel(level, blockColors, blockPos, minX + x, minZ + z);

                if(color != 0) {
                    colors[x][z] = color;
                }
            }
        }

        return new BlockColorMD(BlazeMapReferences.MasterData.BLOCK_COLOR, colors);
    }

    protected int getColorAtPos(Level level, BlockColors blockColors, BlockState state, BlockPos blockPos) {
        int color = blockColors.getColor(state, level, blockPos, 0);

        if(color <= 0) {
            MapColor mapColor = state.getMapColor(level, blockPos);

            if(mapColor != MapColor.NONE) {
                color = mapColor.col;
            }
        }

        return color;
    }

    protected int getColorAtMapPixel(Level level, BlockColors blockColors, MutableBlockPos blockPos, int x, int z) {
        int color = 0;

        for (int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                y > level.getMinBuildHeight();
                y--) {
            blockPos.set(x, y, z);
            final BlockState state = level.getBlockState(blockPos);

            if (state.isAir()) {
                continue;
            }

            color = getColorAtPos(level, blockColors, state, blockPos);

            if (color <= 0) {
                continue;
            }

            if (isSemiTransparent(state)) {
                continue;
            }

            // Hasn't met any of the conditions to continue checking the blocks under it, so break
            break;
        }

        return color;
    }
}
