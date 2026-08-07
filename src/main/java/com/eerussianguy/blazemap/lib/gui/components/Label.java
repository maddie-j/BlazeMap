package com.eerussianguy.blazemap.lib.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.GuiConst;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;

public class Label extends BaseComponent<Label> {
    protected final Font font = Minecraft.getInstance().font;
    protected Either<FormattedCharSequence, String> text;
    protected int color = Colors.WHITE;
    protected int textWidth;

    public Label(String text) {
        this.setText(text);
    }

    public Label(Component text) {
        this(text.getVisualOrderText());
    }

    public Label(FormattedCharSequence text) {
        this.setText(text);
    }

    public int getTextWidth() {
        return textWidth;
    }

    public int getTextHeight() {
        return GuiConst.DEFAULT_FONT_HEIGHT;
    }

    public int getHeight() {
        return getTextHeight();
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        text.map(
            fmt -> font.draw(stack, fmt, this.getPositionX(), this.getPositionY(), getColor()),
            str -> font.draw(stack, str, this.getPositionX(), this.getPositionY(), getColor())
        );
    }

    public int getColor() {
        return color;
    }

    public Label setColor(int color) {
        this.color = color;
        return this;
    }

    public Label setText(String text) {
        this.text = Either.right(text);
        textWidth = font.width(text);
        return setLabelWidth();
    }

    public Label setText(Component text) {
        return setText(text.getVisualOrderText());
    }

    public Label setText(FormattedCharSequence text) {
        this.text = Either.left(text);
        textWidth = font.width(text);
        return setLabelWidth();
    }

    protected Label setLabelWidth() {
        return setSize(getTextWidth(), getTextHeight());
    }

    @Override
    public String toString() {
        return this.text.toString();
    }
}
