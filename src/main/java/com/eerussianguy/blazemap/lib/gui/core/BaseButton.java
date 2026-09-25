package com.eerussianguy.blazemap.lib.gui.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class BaseButton<T extends BaseButton<T>> extends BaseComponent<T> implements ComponentSounds, GuiEventListener {
    private final IntConsumer function;
    protected final ArrayList<Component> tooltips = new ArrayList<>();

    public BaseButton(IntConsumer function) {
        this.function = function;
    }

    protected boolean onClick(int button) {
        playOkSound();
        function.accept(button);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!isEnabled()){
            playDeniedSound();
            return true;
        }
        return onClick(button);
    }

    public void addTooltip(Component ... components) {
        tooltips.addAll(Arrays.asList(components));
    }

    public void replaceTooltip(Component ... components) {
        tooltips.clear();
        tooltips.addAll(Arrays.asList(components));
    }

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        if(tooltips.size() == 0) return;
        service.drawTooltip(stack, mouseX, mouseY, tooltips);
    }
}
