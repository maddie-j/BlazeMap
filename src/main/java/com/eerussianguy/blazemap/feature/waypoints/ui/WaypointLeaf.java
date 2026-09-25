package com.eerussianguy.blazemap.feature.waypoints.ui;

import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorFragment;
import com.eerussianguy.blazemap.feature.waypoints.service.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.mojang.blaze3d.vertex.PoseStack;

public class WaypointLeaf extends WaypointTreeNode {
    private static final int ICON_SIZE = 8;

    private final Waypoint waypoint;
    private final WaypointGroup group;

    public WaypointLeaf(BaseComponent<?> parent, Waypoint waypoint, WaypointGroup group, Runnable delete) {
        super(parent, waypoint, delete, "blazemap.gui.button.edit_waypoint");
        this.waypoint = waypoint;
        this.group = group;

        addButtons(waypoint);
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        int offset = (getHeight() - ICON_SIZE) / 2;
        if(hasMouse && mouseIntercepts(mouseX, mouseY)) {
            renderFlatBackground(stack, 0xFF222222); // render hover
        }
        RenderHelper.drawTexturedQuad(waypoint.getIcon(), waypoint.getColor(), stack, offset, offset, ICON_SIZE, ICON_SIZE);
        super.render(stack, hasMouse, mouseX, mouseY);
    }

    @Override
    protected boolean editNode() {
        return new WaypointEditorFragment(waypoint).push();
    }

    @Override
    protected boolean isEditable() {
        return group.management.canEditChild;
    }
}
