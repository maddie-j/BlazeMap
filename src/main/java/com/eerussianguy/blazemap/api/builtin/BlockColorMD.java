package com.eerussianguy.blazemap.api.builtin;

import java.util.Arrays;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.pipeline.DataType;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;

public class BlockColorMD extends MasterDatum {
    private final BlazeRegistry.Key<DataType<MasterDatum>> id;
    public final int[][] colors;

    public BlockColorMD(BlazeRegistry.Key<DataType<MasterDatum>> id, int[][] colors) {
        this.id = id;
        this.colors = colors;
    }

    @Override
    public BlazeRegistry.Key<DataType<MasterDatum>> getID() {
        return id;
    }

    @Override
    public boolean equalsMD(MasterDatum md) {
        BlockColorMD other = (BlockColorMD) md;
        return Arrays.equals(this.colors, other.colors, Arrays::compare);
    }
}
