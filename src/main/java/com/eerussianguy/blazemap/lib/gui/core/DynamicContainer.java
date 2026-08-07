package com.eerussianguy.blazemap.lib.gui.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * A ontainer that can grow depending on the size of its contents
 */
public class DynamicContainer extends BaseContainer<DynamicContainer> {
    // TODO: Fix overriding/typing/etc
    protected final List<RowContainer> rows = new ArrayList<>();

    protected int componentMargin;
    protected int padding;
    protected int baseWidth;
    protected int baseHeight;

    protected int componentWidth;
    protected int componentHeight;

    public DynamicContainer() {
        this(GuiConst.DEFAULT_WIDGET_PADDING, GuiConst.DEFAULT_MARGIN);
    }

    public DynamicContainer(int padding, int componentMargin) {
        this(padding, componentMargin, 0, 0);
    }

    public DynamicContainer(int padding, int componentMargin, int baseWidth) {
        this(padding, componentMargin, baseWidth, 0);
    }

    public DynamicContainer(int padding, int componentMargin, int baseWidth, int baseHeight) {
        this.componentMargin = componentMargin;
        this.padding = padding;

        this.baseWidth = baseWidth < padding * 2 ? padding * 2 : baseWidth;
        this.baseHeight = baseHeight < padding * 2 ? padding * 2 : baseHeight;

        this.componentWidth = this.baseWidth - (padding * 2);
        this.componentHeight = 0;
    }

    /**
     * Special case override for a RowContainer so that it's added to both arrays
     */
    protected void add(RowContainer row) {
        super.add(row);
        rows.add(row);
    }

    @Override
    public void remove(BaseComponent<?> child) {
        super.remove(child);
        rows.remove(child);
    }

    /**
     * Add a new row to the container containing the provided component(s).
     * 
     * @param component The component(s) to add to the container
     */
    public RowContainer addRow(BaseComponent<?>... components) {
        RowContainer newRow = new RowContainer(this, components);

        this.add(newRow.move(padding, componentHeight + padding));
        growHeight(newRow.getHeight());

        return newRow;
    }

    
    /**
     * Add a new row at the bottom of the container.
     * If componentHeight is less than baseHeight, will leave a gap between the existing components
     * and the start of this row to maintain that base height.
     * 
     * @param component
     */
    public RowContainer addBottomRow(BaseComponent<?>... components) {
        RowContainer newRow = new RowContainer(this, components);

        if (getInnerHeight() > componentHeight + newRow.getHeight()) {
            componentHeight = getInnerHeight() - newRow.getHeight();
        }

        this.add(newRow.move(padding, componentHeight));
        growHeight(newRow.getHeight());

        return newRow;
    }


    // ------
    // TODO: Make height calculations part of the finalisation to allow for expanding rows
    // TODO: Make width calculations for each row happen at finalise time, in case those have been
    //   changed externally
    public DynamicContainer finalise() {
        // Calculate the final width based on absolute width components
        int componentWidth = 0;
        for (RowContainer row: rows) {
            if (row.isRelative()) continue;


            if (row.getIndependentWidth() > componentWidth) {
                componentWidth = row.getIndependentWidth();
            }
        }

        this.componentWidth = componentWidth;

        // Now that we know our total component width, recalculate each row's final values,
        // including any centring or justification if set
        for (RowContainer row: rows) {
            row.calculateRelativeWidths().centerRow().justifyRow();
        }

        // Remove extra margin from container height, since no more rows to be added
        componentHeight -= componentMargin;

        return this;
    }

    // ------
    @Override
    public int getWidth() { return getInnerWidth() + (padding * 2); }
    private int getInnerWidth() { return Math.max(componentWidth, baseWidth - (padding * 2)); }

    @Override
    public int getHeight() { return getInnerHeight() + (padding * 2); }
    private int getInnerHeight() { return Math.max(componentHeight, baseHeight - (padding * 2)); }

    public DynamicContainer setBaseWidth(int width) { 
        this.baseWidth = width;
        return this;
     }

    public DynamicContainer setBaseHeight(int height) { 
        this.baseHeight = height;
        return this;
     }

    /**
     * Grow the height of the container to fit this additional component.
     * @param size The height of the component being added
     */
    private void growHeight(int size) {
        componentHeight = componentHeight + componentMargin + size;
    }

    // ------------------------------------------------------------------------
    public static class RowContainer extends BaseContainer<RowContainer> {
        private final DynamicContainer parent;
        private float[] relativeWidths;
        private boolean isRelative = false;
        private boolean isCentered = false;
        private boolean isJustified = false;

        private int baseWidth; // What width would be with no child component scaling
        private final int height;

        protected RowContainer(DynamicContainer parent, BaseComponent<?>... children) {
            this.parent = parent;

            int nextComponentStart = 0;
            int maxHeight = 0;

            for (BaseComponent<?> child: children) {
                super.add(child.setPosition(nextComponentStart, child.getPositionY()));
                nextComponentStart += child.getIndependentWidth() + parent.componentMargin;

                if (child.getIndependentHeight() > maxHeight) {
                    maxHeight = child.getHeight();
                }
            }

            // Have gone one margin too far
            this.baseWidth = nextComponentStart - parent.componentMargin;
            this.height = maxHeight;
        }

        public RowContainer setWidths(int... widths) {
            this.isRelative = false;

            if (this.size() != widths.length) {
                throw new IllegalArgumentException("The number of sizes must equal the number of components in the row");
            }

            int nextComponentStart = 0;

            for (int i = 0; i < widths.length; i++) {
                renderables.get(i).setWidth(widths[i]).setPosition(nextComponentStart, renderables.get(i).getPositionY());
                nextComponentStart += widths[i] + parent.componentMargin;
            }

            this.baseWidth = nextComponentStart - parent.componentMargin;

            return this;
        }

        public RowContainer setRelativeWidths(float... percents) {
            this.isRelative = true;

            if (this.size() != percents.length) {
                throw new IllegalArgumentException("The number of percentages must equal the number of components in the row");
            }
            
            float totalPercent = 0;

            for (int i = 0; i < percents.length; i++) {
                if (percents[i] < 0 || percents[i] > 1) {
                    throw new IllegalArgumentException("Percentage values must be between 0.0 and 1.0");
                }

                totalPercent += percents[i];
            }

            if (totalPercent > 1) {
                throw new IllegalArgumentException("Total percentage must be < 1.0");
            }

            this.relativeWidths = percents;

            return this;
        }

        /**
         * Syntactic sugar for setRelativeWidths(1)
         */
        public RowContainer fill() {
            if (this.size() != 1) {
                throw new IllegalArgumentException("fill() can only be used on rows containing a single element");
            }

            return this.setRelativeWidths(1);
        }

        
        /**
         * When the parent container's width grows upon adding a new, bigger absolute-width row,
         * call this on any previous rows to update those with relatively-defined widths
         */
        public RowContainer calculateRelativeWidths() {
            if (!this.isRelative) return this;

            int widthMinusMargins = parent.getInnerWidth() - parent.componentMargin * (this.size() - 1);
            int nextComponentStart = 0;

            for (int i = 0; i < relativeWidths.length; i++) {
                int newWidth = (int)(widthMinusMargins * relativeWidths[i]);
                renderables.get(i).setWidth(newWidth).setPosition(nextComponentStart, renderables.get(i).getPositionY());
                
                nextComponentStart += newWidth + parent.componentMargin;
            }

            this.baseWidth = nextComponentStart - parent.componentMargin;
            return this;
        }


        public RowContainer shouldCenter() { 
            this.isCentered = true;
            this.isJustified = false;
            return this;
        }

        public RowContainer centerRow() {
            if (!isCentered) return this;
            if (baseWidth >= parent.getInnerWidth()) return this;

            int originalWidth = -parent.componentMargin;
            for (BaseComponent<?> child: renderables) {
                originalWidth += child.getIndependentWidth() + parent.componentMargin;
            }

            int nextComponentStart = (parent.getInnerWidth() - originalWidth) / 2;

            for (BaseComponent<?> child: renderables) {
                child.setPosition(nextComponentStart, child.getPositionY());
                nextComponentStart += child.getIndependentWidth() + parent.componentMargin;
            }

            // New baseWidth includes the margin on either side. So will just be parent's inner width
            this.baseWidth = parent.getInnerWidth();
            return this;
        }

        
        public RowContainer shouldJustify() { 
            this.isJustified = true;
            this.isCentered = false;
            return this;
        }

        // Note: This does not yet account for the remainder if the division isn't clean
        public RowContainer justifyRow() {
            if (!isJustified) return this;
            if (baseWidth >= parent.getInnerWidth()) return this;

            int componentWidth = 0;
            for (BaseComponent<?> child: renderables) {
                componentWidth += child.getIndependentWidth();
            }

            int justifyMargin = (parent.getInnerWidth() - componentWidth) / (this.size() - 1);
            int nextComponentStart = 0;

            for (BaseComponent<?> child: renderables) {
                child.setPosition(nextComponentStart, child.getPositionY());
                nextComponentStart += child.getIndependentWidth() + justifyMargin;
            }

            this.baseWidth = nextComponentStart - justifyMargin;
            return this;
        }

        protected boolean isRelative() { return this.isRelative; }

        @Override
        public int getWidth() { 
            if (this.isRelative) { return parent.getInnerWidth(); }

            return Math.max(this.baseWidth, parent.getInnerWidth());
        }

        @Override
        public int getIndependentWidth() { return baseWidth; }

        @Override
        public int getHeight() { return height; }

        /**
         * Do not allow for the modification of the RowContainer children after initialisation
         */
        @Override
        protected void add(BaseComponent<?> child) {}
        
        /**
         * Do not allow for the modification of the RowContainer children after initialisation
         */
        @Override
        public void remove(BaseComponent<?> child) {}

    }

}
