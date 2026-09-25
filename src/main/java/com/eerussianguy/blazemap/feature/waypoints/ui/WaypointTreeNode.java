package com.eerussianguy.blazemap.feature.waypoints.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.feature.waypoints.service.LocalState;
import com.eerussianguy.blazemap.feature.waypoints.service.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointConfig;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.gui.components.Label;
import com.eerussianguy.blazemap.lib.gui.components.LineContainer;
import com.eerussianguy.blazemap.lib.gui.components.Tree;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.BaseContainer;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAnchor;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAxis;
import com.eerussianguy.blazemap.lib.gui.core.ContainerDirection;
import com.eerussianguy.blazemap.lib.gui.core.EdgeReference;
import com.eerussianguy.blazemap.lib.gui.core.GuiConst;
import com.eerussianguy.blazemap.lib.gui.trait.BorderedComponent;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class WaypointTreeNode extends BaseContainer<WaypointTreeNode> implements Tree.TreeItem, BorderedComponent, ComponentSounds {
    protected static final ResourceLocation ADD = BlazeMap.resource("textures/gui/add.png");
    protected static final ResourceLocation REMOVE = BlazeMap.resource("textures/gui/remove.png");
    protected static final ResourceLocation EDIT = BlazeMap.resource("textures/gui/edit.png");

    // TODO
    private BaseComponent<?> leftIcon;

    protected final Label label;
    protected final WaypointConfig waypoint;
    protected final LocalState state;

    protected final Runnable onDelete;
    protected final String editTooltip;

    protected final LineContainer buttonHolder;

    protected WaypointManagerButton tmpEditButton;

    protected Runnable updater = () -> {};
    private boolean wasDeleted = false;

    protected WaypointTreeNode(BaseComponent<?> parent, WaypointConfig waypoint, Runnable delete, String editTooltip) {
        super.withParent(parent);

        this.waypoint = waypoint;

        this.height = GuiConst.DEFAULT_FIELD_HEIGHT;
        // TODO: Fix setPosition (see leftIcon)
        this.label = new Label(waypoint.getName()).setPosition(16, getHeight() / 2 - GuiConst.FONT_HEIGHT / 2);
        this.state = waypoint.getState();

        this.onDelete = delete;
        this.editTooltip = editTooltip;

        this.buttonHolder = new LineContainer(ContainerAxis.HORIZONTAL, ContainerDirection.NEGATIVE, 3, GuiConst.DEFAULT_PADDING);

        this.add(this.label);
        this.add(this.buttonHolder);
    }

    protected void addButtons(Waypoint waypoint) {
        buttonHolder.add(new WaypointManagerButton.WorldVisibility(waypoint));
        buttonHolder.add(new WaypointManagerButton.MapVisibility(waypoint));

        if (isDeletable()) {
            buttonHolder.add(new WaypointManagerButton.Delete(this, waypoint));
        }

        if (isEditable()) {
            buttonHolder.add(new WaypointManagerButton.Edit(waypoint));
        }

        this.positionButtons();
        
    }

    protected void addButtons(WaypointGroup group) {
        buttonHolder.add(new WaypointManagerButton.WorldVisibility(group));
        buttonHolder.add(new WaypointManagerButton.MapVisibility(group));

        if (isDeletable()) {
            buttonHolder.add(new WaypointManagerButton.Delete(this, group));
        }
        
        if (isEditable()) {
            buttonHolder.add(new WaypointManagerButton.Edit(group));
        }

        this.positionButtons();
    }

    /**
     * Must be called after the child classes have added the appropriate button sets, so we know how big the container is
     */
    protected void positionButtons() {
        EdgeReference buttonEdgeRef = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(buttonHolder.getWidth(), buttonHolder.getHeight());
        this.buttonHolder.setPosition(buttonEdgeRef.getPositionX() - GuiConst.DEFAULT_PADDING, buttonEdgeRef.getPositionY());
    }

    protected boolean isDeletable() {
        return true;
    }

    protected boolean isEditable() {
        return true;
    }

    public int getColor() {
        return state.isMapVisible() ? Colors.WHITE : Colors.DISABLED;
    }

    public void delete() {
        this.onDelete.run();
        this.wasDeleted = true;
    }

    @Override
    public boolean wasDeleted() {
        return wasDeleted;
    }

    protected abstract boolean editNode();

    @Override
    public void setUpdater(Runnable function) {
        this.updater = function;
    }

    @Override
    public int getWidth() {
        return getParent().getWidth();
    }

    public Font getFont() {
        return this.label.getFont();
    }
}
