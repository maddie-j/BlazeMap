package com.eerussianguy.blazemap.lib.gui.components;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * For use as the title pf a FragmentContainer widget. Correctly offsets Y height
 */
public class TitleLabel extends Label {
    public TitleLabel(String text) {
        super(text);
        this.moveY(1);
    }

    public TitleLabel(Component text) {
        super(text);
        this.moveY(1);
    }

    public TitleLabel(FormattedCharSequence text) {
        super(text);
        this.moveY(1);
    }
}
