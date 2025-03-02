package com.eerussianguy.blazemap.api.maps;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.util.DataSource;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.mojang.blaze3d.platform.NativeImage;

public abstract class SyntheticLayer extends Layer {
    public SyntheticLayer(BlazeRegistry.Key<Layer> id, TranslatableComponent name, ResourceLocation icon) {
        super(id, Type.SYNTHETIC, name, icon, false);
    }

    @Override // can't render
    public final boolean renderTile(NativeImage tile, TileResolution resolution, DataSource data, int xGridOffset, int zGridOffset) {
        throw new UnsupportedOperationException("SyntheticLayers do not render tiles: " + getID());
    }

    @Override // can't have a legend
    public final Renderable getLegendWidget() {
        return null;
    }

    public abstract PixelSource getPixelSource(ResourceKey<Level> dimension, RegionPos region, TileResolution resolution);
}
