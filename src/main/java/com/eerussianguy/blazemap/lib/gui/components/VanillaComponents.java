package com.eerussianguy.blazemap.lib.gui.components;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import com.eerussianguy.blazemap.lib.IntHolder;
import com.eerussianguy.blazemap.lib.ObjHolder;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.GuiConst;
import com.eerussianguy.blazemap.lib.gui.core.WrappedComponent;

public class VanillaComponents {
    public static BaseComponent<?> makeTextField(Font font, ObjHolder<String> value) {
        return makeTextField(font, GuiConst.DEFAULT_FIELD_WIDTH, value);
    }
    public static BaseComponent<?> makeTextField(Font font, int w, ObjHolder<String> value) {
        return makeTextField(font, w, GuiConst.DEFAULT_FIELD_HEIGHT, value);
    }
    public static BaseComponent<?> makeTextField(Font font, int w, int h, ObjHolder<String> value) {
        EditBox textBox = makeEditBox(font, w, h);
        textBox.setValue(value.get());
        textBox.setResponder(value::set);
        return ((WrappedComponent.WrappedVanilla) WrappedComponent.of(textBox)).setPadding(1);
    }

    // FIXME: very broken for some unholy reason
    public static BaseComponent<?> makeRGBHexField(Font font, int w, int h, ObjHolder<String> value) {
        EditBox textBox = makeEditBox(font, w, h);
        textBox.setValue(value.get());
        textBox.setResponder(value::set);
        textBox.setMaxLength(6);
        textBox.setFilter(Pattern.compile("^[0-9a-fA-F]$").asPredicate());
        return ((WrappedComponent.WrappedVanilla) WrappedComponent.of(textBox)).setPadding(1);
    }

    public static BaseComponent<?> makeIntField(Font font, IntHolder value) {
        return makeIntField(font, GuiConst.DEFAULT_FIELD_WIDTH, value);
    }
    public static BaseComponent<?> makeIntField(Font font, int w, IntHolder value) {
        return makeIntField(font, w, GuiConst.DEFAULT_FIELD_HEIGHT, value);
    }
    public static BaseComponent<?> makeIntField(Font font, int w, int h, IntHolder value) {
        EditBox textBox = makeEditBox(font, w, h);
        textBox.setValue(String.valueOf(value.get()));
        IntEnforcer enforcer = new IntEnforcer(value::get, value::set);
        enforcer.setSubject(textBox);
        return ((WrappedComponent.WrappedVanilla) WrappedComponent.of(textBox)).setPadding(1);
    }

    private static EditBox makeEditBox(Font font, int w, int h) {
        return new EditBox(
            font,
            0, 0,
            w - GuiConst.BORDER_WIDTH * 2, h,
            TextComponent.EMPTY
        );
    }

    // FIXME: we should be able to do this with just filters
    @Deprecated
    public static class IntEnforcer {
        private final Supplier<Integer> source;
        private final Consumer<Integer> target;

        public IntEnforcer(Supplier<Integer> source, Consumer<Integer> target) {
            this.source = source;
            this.target = target;
        }

        public void setSubject(EditBox subject) {
            subject.setResponder(s -> {
                if(s == null || s.equals("") || s.equals("-")) return;
                try {
                    int v = Integer.parseInt(s);
                    target.accept(v);
                }
                catch(NumberFormatException ex) {
                    subject.setValue(String.valueOf(source.get()));
                }
            });
        }
    }
}
