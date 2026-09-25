package com.eerussianguy.blazemap.feature.waypoints.ui;

import java.util.function.IntConsumer;

import org.lwjgl.glfw.GLFW;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorFragment;
import com.eerussianguy.blazemap.feature.waypoints.WaypointGroupEditorFragment;
import com.eerussianguy.blazemap.feature.waypoints.service.LocalState;
import com.eerussianguy.blazemap.feature.waypoints.service.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointConfig;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.InheritedBoolean;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.components.ImageButton;
import com.eerussianguy.blazemap.lib.gui.core.GuiConst;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public class WaypointManagerButton extends ImageButton {
    protected WaypointConfig waypoint;
    // protected TranslatableComponent tooltip;

    public WaypointManagerButton(WaypointConfig waypoint, ResourceLocation background, String tooltip, IntConsumer function) {
        super(background, 8, 8, function);
        // super(background, GuiConst.DEFAULT_FONT_HEIGHT, GuiConst.DEFAULT_FONT_HEIGHT, function);
        this.waypoint = waypoint;
        // this.tooltip = Helpers.translate(tooltip);
        this.addTooltip(Helpers.translate(tooltip));
    }

    // TODO: Remove in favour of super
    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        RenderHelper.drawTexturedQuad(background, getTint(), stack, 0, 0, getWidth(), getHeight());
        // RenderHelper.drawTexturedQuad(background, getTint(), stack, getPositionX(), getPositionY(), getWidth(), getHeight());
    }

    // @Override
    // protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
    //     // if(visibility.mouseIntercepts(mouseX, mouseY)) {
    //     //     InheritedBoolean visible = state.getMapVisibility();
    //     //     var tooltip = Helpers.translate(switch(visible) {
    //     //         case TRUE -> "blazemap.gui.button.visibility_show";
    //     //         case FALSE -> "blazemap.gui.button.visibility_hide";
    //     //         case DEFAULT -> "blazemap.gui.button.visibility_default";
    //     //     });
    //     //     service.drawTooltip(stack, mouseX, mouseY, tooltip);
    //     //     return;
    //     // }

    //     // if(isDeletable() && delete.mouseIntercepts(mouseX, mouseY)) {
    //     //     var tooltip = Helpers.translate("blazemap.gui.button.delete");
    //     //     if(!Screen.hasShiftDown()) {
    //     //         service.drawTooltip(stack, mouseX, mouseY, tooltip, Helpers.translate("blazemap.gui.tooltip.confirm_delete").withStyle(ChatFormatting.YELLOW));
    //     //     }
    //     //     else {
    //     //         service.drawTooltip(stack, mouseX, mouseY, tooltip.withStyle(ChatFormatting.RED));
    //     //     }
    //     //     return;
    //     // }

    //     // if(isEditable() && edit.mouseIntercepts(mouseX, mouseY)) {
    //     //     service.drawTooltip(stack, mouseX, mouseY, Helpers.translate(editTooltip));
    //     // }

    //     if(this.mouseIntercepts(mouseX, mouseY)) {
    //         service.drawTooltip(stack, mouseX, mouseY, this.tooltip);
    //     }

    // }


    // TODO: Fix doubled up sounds

    public static class Edit extends WaypointManagerButton {
        protected static final ResourceLocation EDIT = BlazeMap.resource("textures/gui/edit.png");

        public Edit(Waypoint waypoint) {
            super(waypoint, EDIT, "blazemap.gui.button.edit_waypoint",
                button -> {
                boolean editing = openWaypointEditor(waypoint);

                if (editing) {
                    ComponentSounds.playOkSoundStatic();
                } else {
                    ComponentSounds.playDeniedSoundStatic();
                }
            });
        }

        public Edit(WaypointGroup group) {
            super(group, EDIT, "blazemap.gui.button.edit_group",
            button -> {
                boolean editing = openGroupEditor(group);

                if (editing) {
                    ComponentSounds.playOkSoundStatic();
                } else {
                    ComponentSounds.playDeniedSoundStatic();
                }
            });
        }

        private static boolean openWaypointEditor(Waypoint waypoint) {
            return new WaypointEditorFragment(waypoint).push();
        }

        private static boolean openGroupEditor(WaypointGroup waypointGroup) {
            return new WaypointGroupEditorFragment(waypointGroup).push();
        }
    }

    public static class Delete extends WaypointManagerButton {
        protected static final ResourceLocation REMOVE = BlazeMap.resource("textures/gui/remove.png");

        public Delete(WaypointTreeNode treeNode, WaypointConfig waypoint) {
            super(waypoint, REMOVE, "blazemap.gui.tooltip.confirm_delete", 
                button -> {
                    if(Screen.hasShiftDown()) {
                        ComponentSounds.playOkSoundStatic();
                        treeNode.delete();
                        treeNode.updater.run();
                    }
                    else {
                        ComponentSounds.playDeniedSoundStatic();
                    }
                }
            );
        }

        // TODO: Override tooltip for the shift confirmation message
    }

    public static class MapVisibility extends WaypointManagerButton {
        protected static final ResourceLocation ON_OVERRIDE = BlazeMap.resource("textures/gui/on.png");
        protected static final ResourceLocation ON_INHERITED = BlazeMap.resource("textures/gui/on_inherited.png");
        protected static final ResourceLocation OFF_OVERRIDE = BlazeMap.resource("textures/gui/off.png");
        protected static final ResourceLocation OFF_INHERITED = BlazeMap.resource("textures/gui/off_inherited.png");

        public MapVisibility(WaypointConfig waypoint) {
            // super(waypoint, switch (waypoint.getState().getMapVisibility()) {
            //     case TRUE -> Visibility.ON_OVERRIDE;
            //     case FALSE -> Visibility.OFF_OVERRIDE;
            //     case DEFAULT -> Visibility.ON_INHERITED;
            // }, null, null));

            super(waypoint, ON_INHERITED, "blazemap.gui.button.map_visibility_show", button -> {
                int direction = switch(button) {
                    case GLFW.GLFW_MOUSE_BUTTON_1 -> 1;
                    case GLFW.GLFW_MOUSE_BUTTON_2 -> -1;
                    default -> 0;
                };

                if(direction == 0) {
                    ComponentSounds.playDeniedSoundStatic();
                } else {
                    ComponentSounds.playOkSoundStatic();
                    LocalState state = waypoint.getState();
                    state.setMapVisibility(Helpers.cycle(state.getMapVisibility(), direction));
                }
            });

            setBgFromState();
            setTooltipFromState();
        }

        // TODO: Deal with updating when override state changes
        @Override
        public boolean onClick(int button) {
            boolean result = super.onClick(button);

            // Update button state to match new LocalState
            setBgFromState();
            setTooltipFromState();

            return result;
        }

        private void setBgFromState() {
            LocalState state = this.waypoint.getState();

            switch (state.getMapVisibility()) {
                case TRUE -> background = MapVisibility.ON_OVERRIDE;
                case FALSE -> background = MapVisibility.OFF_OVERRIDE;
                case DEFAULT -> {
                    if (state.isMapVisible()) {
                        background = MapVisibility.ON_INHERITED;
                    } else {
                        background = MapVisibility.OFF_INHERITED;
                    };
                }
            };
        }

        private void setTooltipFromState() {
            LocalState state = this.waypoint.getState();

            switch (state.getMapVisibility()) {
                case TRUE -> replaceTooltip(Helpers.translate("blazemap.gui.button.map_visibility_show"));
                case FALSE -> replaceTooltip(Helpers.translate("blazemap.gui.button.map_visibility_hide"));
                case DEFAULT -> replaceTooltip(Helpers.translate("blazemap.gui.button.map_visibility_default"));
            }
        }

    }

    // TODO: Make more distinct from the above MapVisibility
    public static class WorldVisibility extends WaypointManagerButton {
        protected static final ResourceLocation ON_OVERRIDE = BlazeMap.resource("textures/gui/on.png");
        protected static final ResourceLocation ON_INHERITED = BlazeMap.resource("textures/gui/on_inherited.png");
        protected static final ResourceLocation OFF_OVERRIDE = BlazeMap.resource("textures/gui/off.png");
        protected static final ResourceLocation OFF_INHERITED = BlazeMap.resource("textures/gui/off_inherited.png");

        public WorldVisibility(WaypointConfig waypoint) {

            super(waypoint, ON_INHERITED, "blazemap.gui.button.world_visibility_show", button -> {
                int direction = switch(button) {
                    case GLFW.GLFW_MOUSE_BUTTON_1 -> 1;
                    case GLFW.GLFW_MOUSE_BUTTON_2 -> -1;
                    default -> 0;
                };

                if(direction == 0) {
                    ComponentSounds.playDeniedSoundStatic();
                } else {
                    ComponentSounds.playOkSoundStatic();
                    LocalState state = waypoint.getState();
                    state.setInWorldVisibility(Helpers.cycle(state.getInWorldVisibility(), direction));
                }
            });

            setBgFromState();
            setTooltipFromState();
        }

        // TODO: Deal with updating when override state changes
        @Override
        public boolean onClick(int button) {
            boolean result = super.onClick(button);

            // Update button state to match new LocalState
            setBgFromState();
            setTooltipFromState();

            return result;
        }

        private void setBgFromState() {
            LocalState state = this.waypoint.getState();

            switch (state.getInWorldVisibility()) {
                case TRUE -> background = MapVisibility.ON_OVERRIDE;
                case FALSE -> background = MapVisibility.OFF_OVERRIDE;
                case DEFAULT -> {
                    if (state.isInWorldVisible()) {
                        background = MapVisibility.ON_INHERITED;
                    } else {
                        background = MapVisibility.OFF_INHERITED;
                    };
                }
            };
        }

        private void setTooltipFromState() {
            LocalState state = this.waypoint.getState();

            switch (state.getInWorldVisibility()) {
                case TRUE -> replaceTooltip(Helpers.translate("blazemap.gui.button.world_visibility_show"));
                case FALSE -> replaceTooltip(Helpers.translate("blazemap.gui.button.world_visibility_hide"));
                case DEFAULT -> replaceTooltip(Helpers.translate("blazemap.gui.button.world_visibility_default"));
            }
        }

    }
}
