package com.eerussianguy.blazemap.feature;

import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.feature.mapping.*;

public class BlazeMapFeaturesCommon {
    public static void initMapping() {
        BlazeMapAPI.MASTER_DATA.register(new TerrainHeightSerializer(BlazeMapReferences.MasterData.TERRAIN_HEIGHT));
        BlazeMapAPI.MASTER_DATA.register(new TerrainSlopeSerializer());
        BlazeMapAPI.MASTER_DATA.register(new WaterLevelSerializer());
        BlazeMapAPI.MASTER_DATA.register(new BlockColorSerializer(BlazeMapReferences.MasterData.TRANSPARENT_COLOR));
        BlazeMapAPI.MASTER_DATA.register(new BlockColorSerializer(BlazeMapReferences.MasterData.BLOCK_COLOR));
        BlazeMapAPI.MASTER_DATA.register(new TerrainHeightSerializer(BlazeMapReferences.MasterData.NETHER));

        BlazeMapAPI.COLLECTORS.register(new TerrainHeightCollector());
        BlazeMapAPI.COLLECTORS.register(new TerrainSlopeCollector());
        BlazeMapAPI.COLLECTORS.register(new WaterLevelCollector());
        BlazeMapAPI.COLLECTORS.register(new TransparentColorCollector());
        BlazeMapAPI.COLLECTORS.register(new BlockColorCollector());
        BlazeMapAPI.COLLECTORS.register(new NetherCollector());
    }
}
