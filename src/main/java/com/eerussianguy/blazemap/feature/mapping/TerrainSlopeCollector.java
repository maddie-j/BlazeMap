package com.eerussianguy.blazemap.feature.mapping;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.TerrainSlopeMD;
import com.eerussianguy.blazemap.api.pipeline.Collector;
import com.eerussianguy.blazemap.util.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class TerrainSlopeCollector extends Collector<TerrainSlopeMD> {

    public TerrainSlopeCollector() {
        super(
            BlazeMapReferences.Collectors.TERRAIN_SLOPE,
            BlazeMapReferences.MasterData.TERRAIN_SLOPE
        );
    }

    @Override
    public TerrainSlopeMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {
        final float[][] slopemap = new float[16][16];

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                slopemap[x][z] = getSlopeGradient(level, minX + x, minZ + z);
            }
        }

        return new TerrainSlopeMD(slopemap);
    }

    protected static int getNonTransparentHeight(Level level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;

        MutableBlockPos blockPos = new MutableBlockPos(x, y, z);
        BlockState state = level.getBlockState(blockPos);

        // blocksMotion() may be deprecated, but is directly taken from Heightmap.Types.MOTION_BLOCKING.
        // When the method is gone, can replace with whatever Heightmap.Types.MOTION_BLOCKING swaps to.
        while (y > level.getMinBuildHeight() && (isQuiteTransparent(state) || !(state.blocksMotion() || !state.getFluidState().isEmpty()))) {
            y--;
            state = level.getBlockState(blockPos.move(Direction.DOWN));
        }

        return y;
    }

    protected static float getSlopeGradient(Level level, int x, int z) {
        int highestHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        int lowestHeight = getNonTransparentHeight(level, x, z);

        float nearSlopeTotal = 0;
        float nearSlopeCount = 0;
        float farSlopeTotal = 0;
        float farSlopeCount = 0;

        // Slope direction is relative to North West/top left of the map as that's the direction our
        // "sunlight" is going to be coming from. +x == East, +z == South.
        // Positive values means in shadow, negative values means in light.
        for (int dx = -2; dx <= 0; dx++) {
            for (int dz = -2; dz <= 0; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                } else if (dx == -2 && dz == -2) {
                    continue;
                }

                if (dx >= -1 && dz >= -1) {
                    // Adjacent block
                    int nearSlope = getRelativeSlope(level, x, z, lowestHeight, dx, dz, true);

                    if (nearSlope < 0) {
                        nearSlopeTotal += nearSlope;
                        nearSlopeCount += 1 - (0.4 * nearSlopeCount);
                    } else if (nearSlope > 0) {
                        // Shadows are weighted more heavily than sunlight
                        nearSlopeTotal += 4 * nearSlope;
                        nearSlopeCount += 4 - (0.5 * nearSlopeCount);
                    }

                } else if (dx >= -2 && dz >= -2) {
                    // Two blocks away
                    int farSlope = getRelativeSlope(level, x, z, lowestHeight, dx, dz, false);

                    if (farSlope < -2) {
                        farSlopeTotal += farSlope;
                        farSlopeCount += 1 - (0.4 * farSlopeCount);
                    } else if (farSlope > 2) {
                        // Shadows are weighted more heavily than sunlight
                        farSlopeTotal += 4 * farSlope;
                        farSlopeCount += 4 - (0.5 * farSlopeCount);
                    }
                }
            }
        }

        float totalSlope = (nearSlopeCount != 0 ? nearSlopeTotal / nearSlopeCount : 0) +
            (farSlopeCount != 0 ? farSlopeTotal / farSlopeCount : 0) / 2;

        if (highestHeight != lowestHeight) {
            // There's transparent blocks between the opaque surface and the sky
            float point = (1 - Colors.getDarknessPoint(highestHeight - lowestHeight)) * 0.5f;
            float point2 = point * point;

            totalSlope = Math.max(Math.min(totalSlope * point2, point2), -point2);
        }

        return totalSlope;
    }

    protected static int getRelativeSlope(Level level, int x, int z, int height, int dx, int dz, boolean isPrimaryShadow) {
        int adjacentBlockHeight = getNonTransparentHeight(level, x + dx, z + dz);

        int relativeSlope = adjacentBlockHeight - height;

        if (adjacentBlockHeight <= level.getMinBuildHeight() && level.getBlockState(new BlockPos(x + dx, adjacentBlockHeight, z + dz)).isAir()) {
            // This block is in an unloaded chunk and can't be processed until BME-47 is dealt with.
            // (Alternatively, somebody's broken through to the void, but that's their own fault!)
            return 0;

        } else if (relativeSlope == 0) {
            // No shading changes
            return 0;

        } else if (relativeSlope > 0) {
            // Add shadow
            return relativeSlope;

        } else {
            int oppositeBlockHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x - dx, z - dz);

            if (
                (isPrimaryShadow && oppositeBlockHeight < height) ||
                (!isPrimaryShadow && oppositeBlockHeight <= height)) {
                // At the top of a slope
                return 0;
            }

            // Add sunlight
            return relativeSlope;
        }
    }
}
