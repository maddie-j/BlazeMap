package com.eerussianguy.blazemap.lib.gui.components;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * For use as the title pf a FragmentContainer widget. Correctly offsets Y height
 */
public class TitleLabel extends Label {
    private boolean needsBottomPadding = true;

    public TitleLabel(String text) {
        super(text);
        this.moveY(2);
    }

    public TitleLabel(Component text) {
        super(text);
        this.moveY(2);
    }

    public TitleLabel(FormattedCharSequence text) {
        super(text);
        this.moveY(2);
    }

    /**
     * If the element below this one is a `SectionLabel`, don't add in the extra padding.
     * The extra padding is alreadu included on the `SectionLabel`.
     */
    public TitleLabel preceedsSectionLabel(boolean doesPreceed) {
        this.needsBottomPadding = !doesPreceed;
        return this;
    }

    @Override
    public int getHeight() {
        if (needsBottomPadding) {
            return super.getHeight() + 6;
        }

        return super.getHeight() + 2;
    }
}
