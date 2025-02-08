package com.eerussianguy.blazemap.feature.waypoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.service.*;
import com.eerussianguy.blazemap.lib.*;
import com.eerussianguy.blazemap.lib.gui.components.IconTabs;
import com.eerussianguy.blazemap.lib.gui.components.Label;
import com.eerussianguy.blazemap.lib.gui.components.Tree;
import com.eerussianguy.blazemap.lib.gui.components.TextButton;
import com.eerussianguy.blazemap.lib.gui.components.selection.DropdownList;
import com.eerussianguy.blazemap.lib.gui.components.selection.SelectionModelSingle;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAnchor;
import com.eerussianguy.blazemap.lib.gui.core.EdgeReference;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.eerussianguy.blazemap.lib.gui.core.VolatileContainer;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentContainer;
import com.eerussianguy.blazemap.lib.gui.trait.BorderedComponent;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.mojang.blaze3d.vertex.PoseStack;

public class WaypointManagerFragment extends BaseFragment {
    private static final int MANAGER_UI_WIDTH = 200;

    public WaypointManagerFragment() {
        super(Helpers.translate("blazemap.gui.waypoint_manager.title"), true, false);
    }

    @Override
    public void compose(FragmentContainer container, VolatileContainer volatiles) {
        WaypointServiceClient client = WaypointServiceClient.instance();
        int y = 0;

        if(container.titleConsumer.isPresent()) {
            container.titleConsumer.get().accept(getTitle());
        } else {
            container.add(new Label(getTitle()), 0, y);
            y = 15;
        }

        DropdownList<ResourceKey<Level>> dimensions = new DropdownList<>(volatiles, d -> new Label(d.location().toString()));
        var model = dimensions.getModel();
        model.setElements(RegistryHelper.getAllDimensions());
        model.setSelected(Helpers.levelOrThrow().dimension());
        container.add(dimensions.setSize(MANAGER_UI_WIDTH, 14), 0, y);

        IconTabs tabs = new IconTabs().setSize(MANAGER_UI_WIDTH, 20).setLine(5, 5);
        container.add(tabs, 0, y += 17);

        for(var pool : client.getPools()) {
            var pc = new PoolContainer(model, container::dismiss, pool);
            container.add(pc, 0, y + 25);
            tabs.add(pc);
        }
    }

    // =================================================================================================================
    private static class PoolContainer extends FragmentContainer implements IconTabs.TabComponent {
        private final SelectionModelSingle<ResourceKey<Level>> dimensions;
        private final List<Component> tooltip = new ArrayList<>();
        private final WaypointPool pool;

        private PoolContainer(SelectionModelSingle<ResourceKey<Level>> dimensions, Runnable dismiss, WaypointPool pool) {
            super(dismiss, 0);
            this.dimensions = dimensions;
            this.pool = pool;
            tooltip.add(pool.getName());
            tooltip.add(new TextComponent("Blaze Map").withStyle(ChatFormatting.BLUE)); // TODO: temporary
            construct();
        }

        private void construct() {
            clear();

            Tree tree = new Tree().setSize(MANAGER_UI_WIDTH, 160);
            add(tree, 0, 0);
            dimensions.addSelectionListener(dimension -> {
                tree.clearItems();
                var groups = pool.getGroups(dimension);
                for(var group : groups) {
                    tree.addItem(new NodeItem(group, () -> groups.remove(group)));
                }
            });

            var addButton = new TextButton(Helpers.translate("blazemap.gui.button.add_waypoint"), button -> {
                pool.getGroups(Helpers.levelOrThrow().dimension()).add(WaypointGroup.make(WaypointChannelLocal.GROUP_DEFAULT));
                PoolContainer.this.construct();
            }).setSize(MANAGER_UI_WIDTH / 2 - 1, 14);
            addButton.setEnabled(pool.canUserCreate());
            add(addButton, 0, 162);
        }

        @Override
        public ResourceLocation getIcon() {
            return pool.icon;
        }

        @Override
        public int getIconTint() {
            return pool.tint;
        }

        @Override
        public Component getName() {
            return pool.getName();
        }

        @Override
        public List<Component> getTooltip() {
            return tooltip;
        }
    }

    // =================================================================================================================
    private static class NodeItem extends TreeNode {
        private static final ResourceLocation ADD = BlazeMap.resource("textures/gui/add.png");

        private final WaypointGroup group;
        private final ArrayList<? extends Tree.TreeItem> children;
        private final EdgeReference add = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(22, 2);
        private boolean open = true;

        private NodeItem(WaypointGroup group, Runnable delete) {
            super(group.getName(), group.getState(), delete);
            this.group = group;
            this.children = new ArrayList<>(group.getAll().stream().map(this::makeChild).toList());
        }

        private LeafItem makeChild(Waypoint waypoint) {
            ResourceLocation id = waypoint.getID();
            return new LeafItem(
                waypoint,
                group,
                () -> {
                    group.remove(id);
                }
            );
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            renderFlatBackground(stack, 0xFF444444);
            if(group.management.canCreateChild) {
                RenderHelper.drawTexturedQuad(ADD, Colors.NO_TINT, stack, add.getPositionX(), add.getPositionY(), add.getWidth(), add.getHeight());
            }
            stack.pushPose();
            stack.translate(getHeight(), getHeight() / 2F - 4, 0);
            font.draw(stack, open ? "v" : ">", -9, 1, Colors.BLACK);
            stack.popPose();
            super.render(stack, hasMouse, mouseX, mouseY);
        }

        @Override @SuppressWarnings("unchecked")
        public List<Tree.TreeItem> getChildren() {
            if(open) {
                children.removeIf(Tree.TreeItem::wasDeleted);
                return (List<Tree.TreeItem>) children;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        @Override
        public int getWidth() {
            return getParent().getWidth();
        }

        @Override
        public int getTextHeight() {
            return 12;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(super.mouseClicked(mouseX, mouseY, button)) return true;

            if(group.management.canCreateChild && add.mouseIntercepts(mouseX, mouseY)) {
                boolean opened = new WaypointEditorFragment(Helpers.getPlayer().blockPosition(), group).push(updater);
                if(opened) {
                    playOkSound();
                } else {
                    playDeniedSound();
                }
                return true;
            }

            this.open = !this.open;
            updater.run();

            if(open) playUpSound();
            else playDownSound();

            return true;
        }

        @Override
        protected boolean isDeletable() {
            return group.management.canDelete;
        }

        @Override
        protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
            if(group.management.canCreateChild && add.mouseIntercepts(mouseX, mouseY)) {
                service.drawTooltip(stack, mouseX, mouseY, Helpers.translate("blazemap.gui.button.add_waypoint"));
                return;
            }
            super.renderTooltip(stack, mouseX, mouseY, service);
        }
    }

    // =================================================================================================================
    private static class LeafItem extends TreeNode {
        private static final ResourceLocation EDIT = BlazeMap.resource("textures/gui/edit.png");
        private static final int ICON_SIZE = 8;
        private final EdgeReference edit = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(22, 2);
        private final Waypoint waypoint;
        private final WaypointGroup group;

        private LeafItem(Waypoint waypoint, WaypointGroup group, Runnable delete) {
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
                } else {
                    playDeniedSound();
                }
            }

            return true;
        }

        @Override
        public int getWidth() {
            return getParent().getWidth();
        }

        @Override
        public int getTextHeight() {
            return 12;
        }
    }

    // =================================================================================================================
    private static class TreeNode extends Label implements Tree.TreeItem, BorderedComponent, ComponentSounds, GuiEventListener {
        private static final ResourceLocation REMOVE = BlazeMap.resource("textures/gui/remove.png");
        private static final ResourceLocation ON_OVERRIDE   = BlazeMap.resource("textures/gui/on.png");
        private static final ResourceLocation ON_INHERITED  = BlazeMap.resource("textures/gui/on_inherited.png");
        private static final ResourceLocation OFF_OVERRIDE  = BlazeMap.resource("textures/gui/off.png");
        private static final ResourceLocation OFF_INHERITED = BlazeMap.resource("textures/gui/off_inherited.png");

        protected final EdgeReference visibility = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(2, 2);
        protected final EdgeReference delete = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(12, 2);
        protected final LocalState state;
        protected final Runnable onDelete;
        protected Runnable updater = () -> {};
        private boolean wasDeleted = false;

        private TreeNode(Component text, LocalState state, Runnable delete) {
            super(text);
            this.state = state;
            this.onDelete = delete;
        }

        private TreeNode(String text, LocalState state, Runnable delete) {
            super(text);
            this.state = state;
            this.onDelete = delete;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            ResourceLocation texture = switch(state.getVisibility()) {
                case TRUE -> ON_OVERRIDE;
                case FALSE -> OFF_OVERRIDE;
                case DEFAULT -> state.isVisible() ? ON_INHERITED : OFF_INHERITED;
            };
            RenderHelper.drawTexturedQuad(texture, Colors.NO_TINT, stack, visibility.getPositionX(), visibility.getPositionY(), visibility.getWidth(), visibility.getHeight());
            if(isDeletable()) {
                RenderHelper.drawTexturedQuad(REMOVE, Screen.hasShiftDown() ? Colors.NO_TINT : Colors.DISABLED, stack, delete.getPositionX(), delete.getPositionY(), delete.getWidth(), delete.getHeight());
            }

            stack.translate(getHeight(), getHeight() / 2F - 4, 0);
            super.render(stack, hasMouse, mouseX, mouseY);
        }

        @Override
        protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
            if(visibility.mouseIntercepts(mouseX, mouseY)) {
                InheritedBoolean visible = state.getVisibility();
                var tooltip = Helpers.translate(switch(visible) {
                    case TRUE -> "blazemap.gui.button.visibility_show";
                    case FALSE -> "blazemap.gui.button.visibility_hide";
                    case DEFAULT -> "blazemap.gui.button.visibility_default";
                });
                service.drawTooltip(stack, mouseX, mouseY, tooltip);
                return;
            }

            if(isDeletable() && delete.mouseIntercepts(mouseX, mouseY)) {
                var tooltip = Helpers.translate("blazemap.gui.button.delete");
                if(!Screen.hasShiftDown()) {
                    service.drawTooltip(stack, mouseX, mouseY, tooltip, Helpers.translate("blazemap.gui.tooltip.confirm_delete").withStyle(ChatFormatting.YELLOW));
                } else {
                    service.drawTooltip(stack, mouseX, mouseY, tooltip.withStyle(ChatFormatting.RED));
                }
            }
        }

        protected boolean isDeletable() {
            return true;
        }

        @Override
        public int getColor() {
            return state.isVisible() ? Colors.WHITE : Colors.DISABLED;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(visibility.mouseIntercepts(mouseX, mouseY)) {
                int direction = switch(button) {
                    case GLFW.GLFW_MOUSE_BUTTON_1 -> 1;
                    case GLFW.GLFW_MOUSE_BUTTON_2 -> -1;
                    default -> 0;
                };
                if(direction == 0) {
                    playDeniedSound();
                } else {
                    playOkSound();
                    state.setVisibility(Helpers.cycle(state.getVisibility(), direction));
                }
                return true;
            }
            if(isDeletable() && delete.mouseIntercepts(mouseX, mouseY)) {
                if(Screen.hasShiftDown()) {
                    playOkSound();
                    onDelete.run();
                    wasDeleted = true;
                    updater.run();
                } else {
                    playDeniedSound();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean wasDeleted() {
            return wasDeleted;
        }

        @Override
        public void setUpdater(Runnable function) {
            this.updater = function;
        }

        @Override
        public int getWidth() {
            return getParent().getWidth();
        }

        @Override
        public int getTextHeight() {
            return 12;
        }
    }
}