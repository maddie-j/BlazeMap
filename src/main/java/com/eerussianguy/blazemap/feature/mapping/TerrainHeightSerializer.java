package com.eerussianguy.blazemap.feature.mapping;

import java.io.IOException;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.builtin.TerrainHeightMD;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class TerrainHeightSerializer implements DataType<TerrainHeightMD> {
    private final BlazeRegistry.Key<?> id;

    public TerrainHeightSerializer(BlazeRegistry.Key<DataType<MasterDatum>> id) {
        this.id = id;
    }

    @Override
    public void serialize(MinecraftStreams.Output stream, TerrainHeightMD terrain) throws IOException {
        stream.writeKey(terrain.getID());
        stream.writeShort(terrain.minY);
        stream.writeShort(terrain.maxY);
        stream.writeShort(terrain.height);
        stream.writeShort(terrain.sea);

        // TODO: Serialise other heightmaps
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                stream.writeShort(terrain.heightmapTerrain[z][x]);
            }
        }
    }

    @Override
    public TerrainHeightMD deserialize(MinecraftStreams.Input stream) throws IOException {
        BlazeRegistry.Key<DataType<MasterDatum>> id = stream.readKey(BlazeMapAPI.MASTER_DATA);
        short minY = stream.readShort();
        short maxY = stream.readShort();
        short height = stream.readShort();
        short sea = stream.readShort();

        int[][] heightmapTerrain = new int[16][16];
        // int[][] heightmapSurface = new int[16][16];
        // int[][] heightmapOpaque = new int[16][16];
        // float[][] heightmapAttenuation = new float[16][16];

        // TODO: Deserialise other heightmaps
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                heightmapTerrain[z][x] = stream.readShort();
            }
        }

        return new TerrainHeightMD(id, minY, maxY, height, sea, heightmapTerrain);
    }

    @Override
    public BlazeRegistry.Key<?> getID() {
        return id;
    }
}
