package com.eerussianguy.blazemap.api.builtin;

import java.util.Arrays;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;

public class TerrainHeightMD extends MasterDatum {
    private final BlazeRegistry.Key<DataType<MasterDatum>> id;
    public final int minY, maxY, height, sea;

    public final int[][] heightmapTerrain;
    // public final int[][] heightmapSurface;
    // // TODO: Better name as not _fully_ opaque, just "opaque enough" for shadows
    // public final int[][] heightmapOpaque;
    // public final float[][] heightmapAttenuation;

    // public TerrainHeightMD(BlazeRegistry.Key<DataType<MasterDatum>> id, int minY, int maxY, int height, int sea, int[][] heightmapTerrain, int[][] heightmapSurface, int[][] heightmapOpaque, float[][] heightmapAttenuation) {
    public TerrainHeightMD(BlazeRegistry.Key<DataType<MasterDatum>> id, int minY, int maxY, int height, int sea, int[][] heightmapTerrain) {
        this.id = id;
        this.minY = minY;
        this.maxY = maxY;
        this.height = height;
        this.sea = sea;

        this.heightmapTerrain = heightmapTerrain;
        // this.heightmapSurface = heightmapSurface;
        // this.heightmapOpaque = heightmapOpaque;
        // this.heightmapAttenuation = heightmapAttenuation;
    }

    @Override
    public BlazeRegistry.Key<DataType<MasterDatum>> getID() {
        return id;
    }

    @Override
    public boolean equalsMD(MasterDatum md) {
        TerrainHeightMD other = (TerrainHeightMD) md;
        return Arrays.equals(this.heightmapTerrain, other.heightmapTerrain, Arrays::compare);
    }
}
