package com.eerussianguy.blazemap.feature.mapping;

import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.pipeline.ClientOnlyCollector;
import com.eerussianguy.blazemap.util.Colors;

public class BlockColorCollector extends ClientOnlyCollector<BlockColorMD> {
    protected static final HashMap<Integer, Float> darknessPointCache = new HashMap<Integer, Float>();

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

                if(color > 0) {
                    colors[x][z] = color;
                }
            }
        }

        return new BlockColorMD(BlazeMapReferences.MasterData.BLOCK_COLOR, colors);
    }

    /**
     * These blocks don't return accurate colours using the other methods,
     * so unfortunately need to set a colour manually
     * 
     * @param state
     * @return
     */
    protected int handleSpecialCases(BlockState state) {
        var block = state.getBlock();

        // By default, the colour returned for seagrass is purple, so replacing with a green picked from
        // its texture 
        if (block instanceof SeagrassBlock || block instanceof TallSeagrassBlock || block instanceof KelpPlantBlock) {
            return 0x215800;
            // return 0x2f8200;
            // return 0x509529;
            // return 0x009500;
        }

        // if (block instanceof FlowerBlock) {
        //     state;
        //     // if (block instanceof )

        // }

        return 0;
    }

    protected int getColorAtPos(Level level, BlockColors blockColors, BlockState state, BlockPos blockPos) {
        // if (state.getBlock() instanceof BushBlock && Math.random() > 0.98) {
        //     BlazeMap.LOGGER.info("=== {}  \t{}  \t{}  \t{} ===", state.getBlock().getClass().getName(), Integer.toHexString(blockColors.getColor(state, level, blockPos, 0)), Integer.toHexString(state.getMapColor(level, blockPos).col), Integer.toHexString(handleSpecialCases(state)));
        // }

        int color = handleSpecialCases(state);

        if (color <= 0) {
            color = blockColors.getColor(state, level, blockPos, 0);
        }

        if(color <= 0) {
            MapColor mapColor = state.getMapColor(level, blockPos);

            if(mapColor != MapColor.NONE) {
                color = mapColor.col;
            }
        }

        return color;
    }

    record TransparentBlock(int color, float opacity) {
        public float[] argb() {
            float[] argb = Colors.decomposeRGBA(color);
            argb[0] = opacity();
            return argb;
        }
    }

    protected int getColorAtMapPixel(Level level, BlockColors blockColors, MutableBlockPos blockPos, int x, int z) {
        int color = 0;
        // float[] hsbo = new float[4];
        float[] argb = new float[4];
        Queue<TransparentBlock> transparentBlocks = new LinkedList<TransparentBlock>();

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
                if (isQuiteTransparent(state)) {
                    transparentBlocks.add(new TransparentBlock(color, Colors.OPACITY_LOW));
                } else {
                    transparentBlocks.add(new TransparentBlock(color, Colors.OPACITY_HIGH));
                }
                continue;
            }

            // Hasn't met any of the conditions to continue checking the blocks under it, so break
            break;
        }

        if (transparentBlocks.size() > 0) {
            int depth = transparentBlocks.size();
            argb = transparentBlocks.poll().argb();

            while (transparentBlocks.size() > 0) {
                TransparentBlock transparentBlock = transparentBlocks.poll();
                argb = Colors.filterARGB(argb, transparentBlock.argb(), depth);
            }

            color = Colors.recomposeRGBA(Colors.filterARGB(Colors.decomposeRGBA(color), new float[] {0,0,0,0}, depth));
            int finalColor = Colors.recomposeRGBA(argb);
            color = Colors.interpolate(
                color, 0, 
                finalColor, 1, 
                Math.max(0.75f, argb[0]) // It looks silly on thinner layers if the final opacity < 0.75
            ) & 0x00FFFFFF;
        }

        return color;
    }
}
