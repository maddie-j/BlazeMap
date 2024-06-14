package com.eerussianguy.blazemap.feature.mapping;

import java.io.IOException;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.builtin.BlockColorMD;
import com.eerussianguy.blazemap.api.debug.MDInspectionController;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class BlockColorSerializer implements DataType<BlockColorMD> {
    private final BlazeRegistry.Key<?> id;

    public BlockColorSerializer(BlazeRegistry.Key<DataType<MasterDatum>> id) {
        this.id = id;
    }

    @Override
    public BlazeRegistry.Key<?> getID() {
        return this.id;
    }

    @Override
    public void serialize(MinecraftStreams.Output stream, BlockColorMD datum) throws IOException {
        stream.writeKey(datum.getID());

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                stream.writeInt(datum.colors[x][z]);
            }
        }
    }

    @Override
    public BlockColorMD deserialize(MinecraftStreams.Input stream) throws IOException {
        BlazeRegistry.Key<DataType<MasterDatum>> id = stream.readKey(BlazeMapAPI.MASTER_DATA);
        int[][] colors = new int[16][16];

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                colors[x][z] = stream.readInt();
            }
        }

        return new BlockColorMD(id, colors);
    }

    @Override
    public MDInspectionController<BlockColorMD> getInspectionController() {
        return new MDInspectionController<BlockColorMD>() {
            @Override
            public int getNumLines(BlockColorMD datum) {
                return 0;
            }

            @Override
            public String getLine(BlockColorMD datum, int line) {
                return null;
            }

            @Override
            public int getNumGrids(BlockColorMD datum) {
                return 1;
            }

            @Override
            public String getGridName(BlockColorMD datum, int grid) {
                return "colors";
            }

            @Override
            public ResourceLocation getIcon(BlockColorMD datum, int grid, int x, int z) {
                return null;
            }

            @Override
            public int getTint(BlockColorMD datum, int grid, int x, int z) {
                return 0xFF000000 | datum.colors[x][z];
            }

            @Override
            public String getTooltip(BlockColorMD datum, int grid, int x, int z) {
                return Integer.toHexString(getTint(datum, grid, x, z));
            }
        };
    }
}
