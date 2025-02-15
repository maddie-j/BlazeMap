package com.eerussianguy.blazemap.feature.waypoints.ui;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorFragment;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAnchor;
import com.eerussianguy.blazemap.lib.gui.core.EdgeReference;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.mojang.blaze3d.vertex.PoseStack;

public class WaypointLeaf extends WaypointTreeNode {
    private static final ResourceLocation EDIT = BlazeMap.resource("textures/gui/edit.png");
    private static final int ICON_SIZE = 8;
    private final EdgeReference edit = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(22, 2);
    private final Waypoint waypoint;
    private final WaypointGroup group;

    public WaypointLeaf(Waypoint waypoint, WaypointGroup group, Runnable delete) {
        super(waypoint.getName(), group.getState(waypoint.getID()), delete);
        this.waypoint = waypoint;
        this.group = group;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        int offset = (getHeight() - ICON_SIZE) / 2;
        if(hasMouse && mouseIntercepts(mouseX, mouseY)) {
            renderFlatBackground(stack, 0xFF222222); // render hover
        }
        if(group.management.canEditChild) {
            RenderHelper.drawTexturedQuad(EDIT, Colors.NO_TINT, stack, edit.getPositionX(), edit.getPositionY(), edit.getWidth(), edit.getHeight());
        }
        RenderHelper.drawTexturedQuad(waypoint.getIcon(), waypoint.getColor(), stack, offset, offset, ICON_SIZE, ICON_SIZE);
        super.render(stack, hasMouse, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
        if(group.management.canEditChild && edit.mouseIntercepts(mouseX, mouseY)) {
            service.drawTooltip(stack, mouseX, mouseY, Helpers.translate("blazemap.gui.button.edit_waypoint"));
            return;
        }
        super.renderTooltip(stack, mouseX, mouseY, service);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(super.mouseClicked(mouseX, mouseY, button)) return true;

        if(group.management.canEditChild && edit.mouseIntercepts(mouseX, mouseY)) {
            boolean opened = new WaypointEditorFragment(waypoint).push();
            if(opened) {
                playOkSound();
            }
            else {
                playDeniedSound();
            }
        }

        return true;
    }
}
