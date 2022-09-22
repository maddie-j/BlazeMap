package com.eerussianguy.blazemap.feature.waypoints;

import java.io.*;
import java.util.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.event.WaypointEvent;
import com.eerussianguy.blazemap.api.markers.IMarkerStorage;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.api.util.IOSupplier;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;

public class WaypointStore implements IMarkerStorage<Waypoint> {
    private final Map<ResourceLocation, Waypoint> store = new HashMap<>();
    private final Collection<Waypoint> view = Collections.unmodifiableCollection(store.values());

    @Override
    public Collection<Waypoint> getAll() {
        return view;
    }

    @Override
    public void add(Waypoint waypoint) {
        if(store.containsKey(waypoint.getID()))
            throw new IllegalStateException("The waypoint is already registered!");
        store.put(waypoint.getID(), waypoint);
        MinecraftForge.EVENT_BUS.post(new WaypointEvent.Created(waypoint));
        save();
    }

    @Override
    public void remove(ResourceLocation id) {
        if(store.containsKey(id)) {
            Waypoint waypoint = store.remove(id);
            MinecraftForge.EVENT_BUS.post(new WaypointEvent.Removed(waypoint));
            save();
        }
    }

    @Override
    public boolean has(ResourceLocation id) {
        return store.containsKey(id);
    }


    // =================================================================================================================
    private final IOSupplier<MinecraftStreams.Output> outputSupplier;
    private final IOSupplier<MinecraftStreams.Input> inputSupplier;

    public WaypointStore(IOSupplier<MinecraftStreams.Input> inputSupplier, IOSupplier<MinecraftStreams.Output> outputSupplier){
        this.outputSupplier = outputSupplier;
        this.inputSupplier = inputSupplier;
        load();
    }

    public void save() {
        if(outputSupplier == null) {
            BlazeMap.LOGGER.warn("Waypoint store storage supplier is null, ignoring save request");
            return;
        }

        if(store.size() > 0) {
            try(MinecraftStreams.Output output = outputSupplier.get()) {
                output.writeInt(store.size());
                for(Waypoint waypoint : store.values()) {
                    output.writeResourceLocation(waypoint.getID());
                    output.writeDimensionKey(waypoint.getDimension());
                    output.writeBlockPos(waypoint.getPosition());
                    output.writeUTF(waypoint.getLabel());
                    output.writeResourceLocation(waypoint.getIcon());
                    output.writeInt(waypoint.getColor());
                    output.writeFloat(waypoint.getRotation());
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        try(MinecraftStreams.Input input = inputSupplier.get()) {
            int count = input.readInt();
            for(int i = 0; i < count; i++) {
                Waypoint waypoint = new Waypoint(
                    input.readResourceLocation(),
                    input.readDimensionKey(),
                    input.readBlockPos(),
                    input.readUTF(),
                    input.readResourceLocation(),
                    input.readInt(),
                    input.readFloat()
                );
                store.put(waypoint.getID(), waypoint);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
