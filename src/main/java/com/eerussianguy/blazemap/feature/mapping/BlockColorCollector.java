package com.eerussianguy.blazemap.feature.mapping;

import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.pipeline.ClientOnlyCollector;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Transparency;
import com.eerussianguy.blazemap.util.Transparency.TransparencyState;
import com.eerussianguy.blazemap.util.Transparency.BlockComposition;

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
    protected static int handleSpecialCases(BlockState state) {
        var block = state.getBlock();

        // By default, the colour returned for seagrass is purple, so replacing with a green picked from
        // its texture 
        if (block instanceof SeagrassBlock || block instanceof TallSeagrassBlock || block instanceof KelpPlantBlock || block instanceof KelpBlock) {
            return 0x215800;
        }

        return 0;
    }

    protected static int getColorAtPos(Level level, BlockColors blockColors, BlockState state, BlockPos blockPos) {
        int color = handleSpecialCases(state);

        if (color <= 0) {
            // The blocks/fluids that change depending on location (?)
            color = blockColors.getColor(state, level, blockPos, 0);
        }

        if(color <= 0) {
            // All other blocks/fluids
            MapColor mapColor = state.getMapColor(level, blockPos);

            if(mapColor != MapColor.NONE) {
                color = mapColor.col;
            }
        }

        return color;
    }

    protected int getColorAtMapPixel(Level level, BlockColors blockColors, MutableBlockPos blockPos, int x, int z) {
        int color = 0;
        float[] argb = new float[4];
        Queue<BlockColor> transparentBlocks = new LinkedList<BlockColor>();

        for (int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                y > level.getMinBuildHeight();
                y--) {
            blockPos.set(x, y, z);
            final BlockState state = level.getBlockState(blockPos);

            if (state.isAir()) {
                continue;
            }

            // // TODO: For debugger reasons:
            // Block thisBlockType = state.getBlock();
            // String thisBlockName = state.getBlock().getDescriptionId();

            BlockColor processedBlock = new BlockColor(state, level, blockPos, blockColors, transparentBlocks.isEmpty());

            // TODO: See if this inequality is the cause of the transparency bug
            if (processedBlock.totalColor <= 0) {
                continue;
            }

            if (processedBlock.getTransparencyState() != TransparencyState.OPAQUE) {
                transparentBlocks.add(processedBlock);
                continue;
            }

            // Hasn't met any of the conditions to continue checking the blocks under it, so finalise and break
            color = processedBlock.totalColor;
            break;
        }

        if (transparentBlocks.size() > 0) {
            int depth = transparentBlocks.size();
            argb = transparentBlocks.poll().argb();

            // Top layer must be minimum this colour as it should be the easiest to see.
            // Represents extra sunlight reflecting off the surface
            argb[0] = Math.max(0.5f, argb[0]);

            while (transparentBlocks.size() > 0) {
                BlockColor transparentBlock = transparentBlocks.poll();
                argb = Colors.filterARGB(argb, transparentBlock.argb(), depth);
            }

            // The shade on the solid block at the bottom of the ocean
            color = Colors.recomposeRGBA(Colors.filterARGB(new float[] {0,0,0,0}, Colors.decomposeRGBA(color), depth));

            int finalColor = Colors.recomposeRGBA(argb);
            color = Colors.interpolate(
                color, 0, 
                finalColor, 1, 
                Math.max(0.75f, argb[0]) // It looks silly on thinner layers if the final opacity < 0.75
            ) & 0x00FFFFFF;
        }

        return color;
    }

    public static class BlockColor {
        private final BlockComposition blockComposition;
        protected final int totalColor;

        private BlockColor(BlockState state, Level level, BlockPos pos, BlockColors blockColors, boolean isSurfaceBlock) {
            this.blockComposition = Transparency.getBlockComposition(state, level, pos);

            // // TODO: For debugger reasons:
            // Block thisBlockType = state.getBlock();
            // String thisBlockName = state.getBlock().getDescriptionId();

            // Get and mix the colours based on the appropriate mixing scheme
            switch (blockComposition.compositionState) {
                case BLOCK:
                case NON_SOLID_BLOCK:
                    // Normal block conditions
                    this.totalColor = getColorAtPos(level, blockColors, state, pos);
                    break;

                case FLUID:
                    // Just a fluid
                    this.totalColor = getColorAtPos(level, blockColors, state, pos);
                    break;

                case FLUIDLOGGED:
                    // Fluidlogged block
                    int blockColor = getColorAtPos(level, blockColors, state, pos);

                    BlockState equivalentFluidBlock = state.getFluidState().createLegacyBlock();
                    int fluidColor = getColorAtPos(level, blockColors, equivalentFluidBlock, pos);

                    float[] blockArgb = argb(blockColor, blockComposition.blockTransparencyLevel.opacity);
                    float[] fluidArgb = argb(fluidColor, blockComposition.fluidTransparencyLevel.opacity);

                    if (isSurfaceBlock) {
                        // Baseline opacity so thin fluids can still be seen
                        // Represents extra sunlight reflecting off the surface
                        fluidArgb[0] = Math.max(0.5f, fluidArgb[0]);
                    }

                    float[] totalArgb = Colors.filterARGB(fluidArgb, blockArgb, 0);
                    this.totalColor = Colors.recomposeRGBA(totalArgb) & 0x00FFFFFF;
                    break;

                default:
                    // Zero opacity
                    this.totalColor = 0x00000000;
            }
        }

        public float[] argb() {
            return argb(totalColor, blockComposition.totalTransparencyLevel.opacity);
        }

        protected float[] argb(int color, float opacity) {
            float[] argb = Colors.decomposeRGBA(color);
            argb[0] = opacity;
            return argb;
        }

        public TransparencyState getTransparencyState() {
            return blockComposition.getTransparencyState();
        }
    }
}
