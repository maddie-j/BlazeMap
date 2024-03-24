package com.eerussianguy.blazemap;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.eerussianguy.blazemap.BlazeMap;

// This class solely exists for debugging purposes. It should not be initialised in the
// final build.
public class DebuggingEventHandler {

    public static void init() {
        final IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        forgeEventBus.addListener(DebuggingEventHandler::logLogin);
        forgeEventBus.addListener(DebuggingEventHandler::logRawMouse);
        // forgeEventBus.addListener(DebuggingEventHandler::logMouseInput);
        // forgeEventBus.addListener(DebuggingEventHandler::logKeyboardInput);

        modEventBus.addListener(DebuggingEventHandler::logCommonSetup);
        modEventBus.addListener(DebuggingEventHandler::logClientSetup);
        modEventBus.addListener(DebuggingEventHandler::logDedicatedServerSetup);
        modEventBus.addListener(DebuggingEventHandler::logInterModEnqueueSetup);
        modEventBus.addListener(DebuggingEventHandler::logInterModProcessSetup);
        
        // forgeEventBus.addListener(DebuggingEventHandler::logChunkChange);
        // forgeEventBus.addListener(DebuggingEventHandler::logBlockChange);
    }

    private static void logLogin(ClientPlayerNetworkEvent.LoggedInEvent event) {
        BlazeMap.LOGGER.warn("999 Captured login event: {}", event.getPlayer());
    }


    // Keys and clicks
    private static void logRawMouse(InputEvent.RawMouseEvent event) {
        BlazeMap.LOGGER.warn("000 Captured raw mouse: {} {} {}", event.getButton(), event.getAction(), event.getModifiers());
    }

    private static void logMouseInput(InputEvent.MouseInputEvent event) {
        BlazeMap.LOGGER.warn("000 Captured mouse input: {} {} {}", event.getButton(), event.getAction(), event.getModifiers());
    }
    
    private static void logKeyboardInput(InputEvent.KeyInputEvent event) {
        BlazeMap.LOGGER.warn("000 Captured keyboard input: {} {} {}", event.getKey(), event.getAction(), event.getModifiers());
    }


    // Setup
    private static void logCommonSetup(FMLCommonSetupEvent event) {
        BlazeMap.LOGGER.warn("111 Common Setup");
    }

    private static void logClientSetup(FMLClientSetupEvent event) {
        BlazeMap.LOGGER.warn("222 Client Setup");
    }

    private static void logDedicatedServerSetup(FMLDedicatedServerSetupEvent event) {
        BlazeMap.LOGGER.warn("222 Server Setup");
    }

    private static void logInterModEnqueueSetup(InterModEnqueueEvent event) {
        BlazeMap.LOGGER.warn("333 Inter Mod Enqueue Setup");
    }

    private static void logInterModProcessSetup(InterModProcessEvent event) {
        BlazeMap.LOGGER.warn("444 Inter Mod Process Setup");
    }


    // World loading
    // private static void logChunkChange(ChunkEvent event) {
    //     BlazeMap.LOGGER.warn("555 ChunkEvent {} {}", event.getChunk().getPos(), event.getClass());
    // }

    // private static void logBlockChange(BlockEvent event) {
    //     // var chunk = ServerLevel.getChunkAt(event.getPos()).getPos();
    //     BlazeMap.LOGGER.warn("555 BlockEvent {} {} {}, Chunk: {}", event.getPos(), event.getState(), event.getWorld(), event.getWorld().getChunk(event.getPos()).getPos());
    // }

}
