package com.eerussianguy.blazemap.feature.mapping;

import java.util.Stack;
import java.util.HashMap;
import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.pipeline.ClientOnlyCollector;
import com.eerussianguy.blazemap.util.Colors;

public class TransparentColorCollector extends ClientOnlyCollector<BlockColorMD> {
    // protected static HashMap<Integer, Float> transparencyPointCache = new HashMap<Integer, Float>();
    protected static final HashMap<Integer, Float> darknessPointCache = new HashMap<Integer, Float>();

    public TransparentColorCollector() {
        super(
            BlazeMapReferences.Collectors.TRANSPARENT_COLOR,
            BlazeMapReferences.MasterData.TRANSPARENT_COLOR
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

                // if (Math.random() > 0.99) {
                //     BlazeMap.LOGGER.info("== {} {} ==", Integer.toHexString(color), color != 0);
                // }
                
                if(color != 0) {
                    colors[x][z] = color;
                }
            }
        }

        return new BlockColorMD(BlazeMapReferences.MasterData.TRANSPARENT_COLOR, colors);
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

    record TransparentBlock(int color, float opacity) {
        public float[] hsbo() {
            int[] argb = Colors.decomposeIntRGBA(color);
            float[] hsbo = new float[] { 0, 0, 0, opacity() };
            return Color.RGBtoHSB(argb[1], argb[2], argb[3], hsbo);
        }
    }

    protected int getColorAtMapPixel(Level level, BlockColors blockColors, MutableBlockPos blockPos, int x, int z) {
        // int color = 0;
        // float[] hsbo = new float[3];
        // Stack<TransparentBlock> transparentBlocks = new Stack<TransparentBlock>();

        // for (int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        //         y > level.getMinBuildHeight();
        //         y--) {
        //     blockPos.set(x, y, z);
        //     final BlockState state = level.getBlockState(blockPos);

        //     if (state.isAir()) {
        //         continue;
        //     }

        //     color = getColorAtPos(level, blockColors, state, blockPos);

        //     if (color <= 0) {
        //         continue;
        //     }

        //     if (isSemiTransparent(state)) {
        //         if (isQuiteTransparent(state)) {
        //             transparentBlocks.push(new TransparentBlock(color, 0.5f));
        //         } else {
        //             transparentBlocks.push(new TransparentBlock(color, 0.75f));
        //         }
        //         continue;
        //     }

        //     // Hasn't met any of the conditions to continue checking the blocks under it, so break
        //     break;
        // }

        // if (transparentBlocks.size() > 0) {
        //     hsbo = transparentBlocks.pop().hsbo();
            
        //     while (transparentBlocks.size() > 0) {
        //         TransparentBlock transparentBlock = transparentBlocks.pop();
        //         hsbo = Colors.filter(hsbo, transparentBlock.hsbo(), transparentBlocks.size());
        //     }

        //     int finalColor = Color.HSBtoRGB(hsbo[0], hsbo[1], hsbo[2]);
        //     int finalOpacity = ((int)(hsbo[3] * 255) << 24) | 0x00FFFFFF;

        //     // if (Math.random() > 0.99) {
        //     //     BlazeMap.LOGGER.info("== {} {} {} ==", Integer.toHexString(finalColor), Integer.toHexString(finalOpacity), Integer.toHexString(finalColor & finalOpacity));
        //     // }

        //     return finalColor & finalOpacity;
        // }

        
        // if (transparentBlocks.size() > 0) {
        //     int depth = transparentBlocks.size();
        //     hsbo = transparentBlocks.pop().hsbo();
            
        //     while (transparentBlocks.size() > 0) {
        //         TransparentBlock transparentBlock = transparentBlocks.pop();
        //         hsbo = Colors.filter(hsbo, transparentBlock.hsbo(), depth);
        //     }

        //     // Adjust bottom brightness for light attenuation
        //     float point = darknessPointCache.computeIfAbsent(depth, (size) -> {
        //         // return Math.min(2.90f, 0.5f * (float)Math.log(size)) / 3f;
        //         return Math.max(0f, Math.min(0.99f, (float)Math.log(Math.log(size)) * 0.5f));
        //     });

        //     // float attenuatedBrightness = Colors.interpolate(hsbo[2], 0.1f, Math.min(depth * 0.05f, 1f));
        //     float attenuatedBrightness = Colors.interpolate(hsbo[2], 0.1f, point);

        //     int finalColor = Color.HSBtoRGB(hsbo[0], hsbo[1], attenuatedBrightness);
        //     // int finalOpacity = ((int)(Math.max(0.75f, hsbo[3]) * 255) << 24) | 0x00FFFFFF;
        //     int finalOpacity = ((int)(0.50f * 255) << 24) | 0x00FFFFFF;

        //     return finalColor & finalOpacity;
        //     // color = Colors.interpolate(color, 0, finalColor, 1, Math.max(0.75f, hsbo[3])) & 0x00FFFFFF;
        // }

        return 0;
    }
}
