package com.eerussianguy.blazemap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eerussianguy.blazemap.engine.MDSources;
import com.eerussianguy.blazemap.engine.client.ClientEngine;
import com.eerussianguy.blazemap.profiling.Profilers;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.client.multiplayer.ClientLevel;

public class SodiumCompatMixin {
    
    @Mixin(value = SodiumWorldRenderer.class, remap = false)
    public static interface SodiumWorldRendererMixin {
        @Accessor("world")
        public ClientLevel getWorld();
    }

    
    @Mixin(value = RenderSection.class, remap = false)
    public static interface RenderSectionMixin {
        @Accessor("worldRenderer")
        public SodiumWorldRenderer getWorld();
    }


    @Mixin(value = ChunkRenderList.class, remap = false)
    public static class ChunkRenderListMixin {

        @Inject(method = "add", at = @At("HEAD"), remap = false)
        void onAdd(RenderSection render, CallbackInfo ci) {
            Profilers.Client.Mixin.SODIUM_LOAD_PROFILER.hit();
            Profilers.Client.Mixin.SODIUM_TIME_PROFILER.begin();

            ClientEngine.onChunkChanged(
                ((SodiumWorldRendererMixin)((RenderSectionMixin)render).getWorld()).getWorld().dimension(), 
                render.getChunkPos().chunk(), 
                MDSources.Client.SODIUM
            );

            Profilers.Client.Mixin.SODIUM_TIME_PROFILER.end();
        }
    }

}
