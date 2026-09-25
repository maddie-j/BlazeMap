package com.eerussianguy.blazemap.feature.waypoints.service;

import net.minecraft.network.chat.Component;

public interface WaypointConfig {
    public LocalState getState();

    public Component getName();
}
