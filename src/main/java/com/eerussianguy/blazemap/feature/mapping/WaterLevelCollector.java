package com.eerussianguy.blazemap.feature.mapping;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.builtin.WaterLevelMD;
import com.eerussianguy.blazemap.api.pipeline.Collector;

public class WaterLevelCollector extends Collector<WaterLevelMD> {

    public WaterLevelCollector() {
        super(
            BlazeMapReferences.Collectors.WATER_LEVEL,
            BlazeMapReferences.MasterData.WATER_LEVEL
        );
    }


    @Override
    public WaterLevelMD collect(Level level, int minX, int minZ, int maxX, int maxZ) {

        final int[][] water = new int[16][16];

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                int surfaceHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, minX + x, minZ + z) - 1;
                int waterDepth = level.getHeight(Heightmap.Types.OCEAN_FLOOR, minX + x, minZ + z) - 1;

                // Make sure we're on the bottom of the wet area and not a non-ground waterlogged block
                while(isWater(level, minX + x, waterDepth, minZ + z) && waterDepth > level.getMinBuildHeight()) {
                    waterDepth--;
                }

                if (surfaceHeight > waterDepth) {
                        water[x][z] = surfaceHeight - waterDepth;
                }
            }
        }

        return new WaterLevelMD(level.getSeaLevel(), water);
    }
}
