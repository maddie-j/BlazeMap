package com.eerussianguy.blazemap.feature.waypoints.service;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.markers.Marker;

public final class Waypoint extends Marker<Waypoint> {
    private LocalState state;

    public Waypoint(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, String name) {
        this(id, dimension, position, name, BlazeMapReferences.Icons.WAYPOINT, -1);
        this.randomizeColor();
    }

    public Waypoint(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, String name, ResourceLocation icon) {
        this(id, dimension, position, name, icon, -1);
        this.randomizeColor();
    }

    public Waypoint(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, String name, ResourceLocation icon, int color) {
        // TODO: Consider if there's a better default state value, especially considering remote waypoints
        this(id, dimension, position, name, icon, color, 0F, new LocalState());
    }

    public Waypoint(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, String name, ResourceLocation icon, int color, float rotation, LocalState state) {
        super(id, dimension, position, icon);
        setName(name);
        setColor(color);
        setNameVisible(true);
        setRotation(rotation);

        setState(state);
    }

    public LocalState getState() {
        return state;
    }

    public void setState(LocalState state) {
        this.state = state;
    }

    public void setStateFromParent(WaypointGroup parentGroup) {
        this.state = new LocalState(parentGroup.getState());
    }

    public boolean isMapVisible() {
        return state.isMapVisible();
    }

    public boolean isInWorldVisible() {
        return state.isInWorldVisible();
    }

    public boolean isBeamVisible() {
        return state.isBeamVisible();
    }

    public boolean isLabelVisible() {
        return state.isLabelVisible();
    }
}
