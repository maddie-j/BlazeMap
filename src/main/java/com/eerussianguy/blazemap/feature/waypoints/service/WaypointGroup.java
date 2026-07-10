package com.eerussianguy.blazemap.feature.waypoints.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.markers.MarkerStorage;

public class WaypointGroup implements MarkerStorage<Waypoint> {
    private static final HashMap<ResourceLocation, Supplier<WaypointGroup>> GROUP_DEFINITIONS = new HashMap<>();

    public static WaypointGroup make(ResourceLocation type) {
        assertDefined(type);
        return GROUP_DEFINITIONS.get(type).get();
    }

    public static void assertDefined(ResourceLocation type) {
        if(!GROUP_DEFINITIONS.containsKey(type)) {
            throw new IllegalStateException("WaypointGroup type "+type+" has not been defined");
        }
    }

    public static void define(ResourceLocation type, Supplier<WaypointGroup> factory) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(factory);

        if(GROUP_DEFINITIONS.containsKey(type)) throw new IllegalStateException("Group "+type+" already defined!");
        GROUP_DEFINITIONS.put(type, factory);
    }

    // =================================================================================================================
    public final ResourceLocation type;
    public final ManagementType management;
    protected final HashMap<ResourceLocation, Waypoint> waypoints = new HashMap<>();
    private final LocalState state = new LocalState();
    private NameType nameType = NameType.USER_GIVEN;
    private Component name;
    private String _name;

    public WaypointGroup(ResourceLocation type) {
        this(type, ManagementType.FULL);
    }

    public WaypointGroup(ResourceLocation type, ManagementType manage) {
        assertDefined(type);
        this.type = type;
        this.management = manage;
    }

    public LocalState getState() {
        return state;
    }

    public boolean isUserNamed() {
        return nameType == NameType.USER_GIVEN;
    }

    public Component getName() {
        return name;
    }

    public String getNameString() {
        return switch(nameType) {
            case USER_GIVEN -> _name;
            case SYSTEM -> name.getString();
        };
    }

    public WaypointGroup setSystemName(Component name) {
        this.name = name;
        this._name = null;
        this.nameType = NameType.SYSTEM;
        return this;
    }

    public WaypointGroup setUserGivenName(String name) {
        if(nameType == NameType.SYSTEM) {
            throw new IllegalStateException("cannot name group with system name");
        }

        this._name = name;
        this.name = new TextComponent(name);
        this.nameType = NameType.USER_GIVEN;
        return this;
    }

    @Override
    public Collection<Waypoint> getAll() {
        return waypoints.values();
    }

    @Override
    public void add(Waypoint marker) {
        var key = marker.getID();

        if(waypoints.containsKey(key)) {
            throw new IllegalArgumentException("Waypoint Group already contains this waypoint");
        }

        waypoints.put(key, marker);
        marker.getState().setParent(this.state);
    }

    @Override
    public void remove(ResourceLocation id) {
        waypoints.remove(id);
    }

    @Override
    public boolean has(ResourceLocation id) {
        return waypoints.containsKey(id);
    }

    private enum NameType {
        USER_GIVEN, SYSTEM
    }
}
