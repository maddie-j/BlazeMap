package com.eerussianguy.blazemap.lib.gui.components;

import java.util.function.IntConsumer;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import com.eerussianguy.blazemap.lib.gui.core.BaseButton;
import com.eerussianguy.blazemap.lib.gui.trait.FocusableComponent;
import com.eerussianguy.blazemap.lib.gui.trait.KeyboardControls;
import com.mojang.blaze3d.vertex.PoseStack;

public class TextButton extends BaseButton<TextButton> implements FocusableComponent, KeyboardControls {
    protected final Font font = Minecraft.getInstance().font;

    protected final Component text;

    public TextButton(Component text, IntConsumer function) {
        super(function);
        this.text = text;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderFocusableBackground(stack, hasMouse);

        float x = (float)(getWidth() - font.width(text)) / 2F;
        float y = (float)(getHeight() - 7) / 2F;
        font.draw(stack, text, x, y, getFocusColor(hasMouse));
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if(!isEnabled()) {
            playDeniedSound();
            return true;
        }

        if(isKeySubmit(key)) {
            onClick(GLFW.GLFW_MOUSE_BUTTON_1);
        }

        return false;
    }
}
