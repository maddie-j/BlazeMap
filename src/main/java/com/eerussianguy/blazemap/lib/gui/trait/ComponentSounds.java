package com.eerussianguy.blazemap.lib.gui.trait;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public interface ComponentSounds {
    default void playOkSound() {
        playPitchedClick(1.0F);
    }

    default void playDeniedSound() {
        playPitchedClick(0.8F);
    }

    default void playUpSound() {
        playPitchedClick(1.05F);
    }

    default void playDownSound() {
        playPitchedClick(0.95F);
    }

    default void playPitchedClick(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    // Static copies, for use in lambdas passed to super constructors,
    // since `this` or `super` aren't allowed there for some reason
    public static void playOkSoundStatic() {
        playPitchedClickStatic(1.0F);
    }

    public static void playDeniedSoundStatic() {
            playPitchedClickStatic(0.8F);
        }

    public static void playUpSoundStatic() {
            playPitchedClickStatic(1.05F);
        }

    public static void playDownSoundStatic() {
            playPitchedClickStatic(0.95F);
        }

    public static void playPitchedClickStatic(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

}
