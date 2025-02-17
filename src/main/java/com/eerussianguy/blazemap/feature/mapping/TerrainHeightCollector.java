package com.eerussianguy.blazemap.feature.mapping;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainHeightMD;
import com.eerussianguy.blazemap.api.pipeline.Collector;

public class TerrainHeightCollector extends Collector<TerrainHeightMD> {

    public TerrainHeightCollector() {
        super(
            BlazeMapReferences.Collectors.TERRAIN_HEIGHT,
            BlazeMapReferences.MasterData.TERRAIN_HEIGHT
        );
    }

    @Override
    public TerrainHeightMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {
        final int[][] heightmapTerrain = new int[16][16];
        // final int[][] heightmapSurface = new int[16][16];
        // final int[][] heightmapOpaque = new int[16][16];
        // final float[][] heightmapAttenuation = new float[16][16];

        final int minBuildHeight = level.getMinBuildHeight();
        // MutableBlockPos blockPos = new MutableBlockPos(minX, 0, minZ);

        int blockX;
        int blockZ;

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                blockX = x + minX;
                blockZ = z + minZ;
                /** 
                 * Collect heights of the highest and lowest block that can be seen.
                 * (Primarily for shadow implementation)
                 */
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ) - 1;

                // // TODO: Commented out until Transformer improvements have been made in BME-198
                // // which the below was designed to take advantage of
                // heightmapSurface[z][x] = height;
                
                // blockPos.set(blockX, height, blockZ);
                // float transparency = 1;

                // BlockState state = level.getBlockState(blockPos);
                // BlockComposition blockComposition = Transparency.getBlockComposition(state, level, blockPos);

                // while (height > minBuildHeight
                //     && (
                //         TransparencyState.isAtLeastAsTransparentAs(
                //             blockComposition.getTransparencyState(),
                //             TransparencyState.QUITE_TRANSPARENT
                //         )
                //         // TODO: Make work better with bamboo
                //         || blockComposition.getBlockCompositionState() == CompositionState.NON_FULL_BLOCK
                //         )
                // ) {
                //     transparency = transparency * blockComposition.getTransparencyState().transparency;

                //     height--;
                //     state = level.getBlockState(blockPos.move(Direction.DOWN));
                //     blockComposition = Transparency.getBlockComposition(state, level, blockPos);
                // }

                // // Like with the BlockColor, the minimum opacity/max transparency should be 0.75/0.25
                // transparency = Math.min(0.25f, transparency);

                // heightmapOpaque[z][x] = height;
                // heightmapAttenuation[z][x] = transparency;


                /** 
                 * Now collect base terrain height.
                 * This ignores non-terrain blocks such as trees and other plantlife
                 */
                height = findSurfaceBelowVegetation(level, blockX, blockZ, false);

                // Note: The + 1 here is for legacy reasons. Will make it somebody else's decision
                // wether or not to remove it and possibly make other visual adjustments instead.
                heightmapTerrain[z][x] = height + 1;
            }
        }


        return new TerrainHeightMD(
            BlazeMapReferences.MasterData.TERRAIN_HEIGHT,
            minBuildHeight,
            level.getMaxBuildHeight(),
            level.getHeight(),
            level.getSeaLevel(),
            heightmapTerrain
            // heightmapSurface,
            // heightmapOpaque,
            // heightmapAttenuation
        );
    }

    protected static boolean isSkippableAfterLeaves(Level level, int x, int y, int z) {
        BlockState state = level.getBlockState(POS.set(x, y, z));
        return state.is(BlockTags.LOGS) || isLeavesOrReplaceable(level, x, y, z);
    }
}
