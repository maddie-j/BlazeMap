package com.eerussianguy.blazemap.feature.waypoints;

import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.ObjHolder;
import com.eerussianguy.blazemap.lib.gui.components.TextButton;
import com.eerussianguy.blazemap.lib.gui.components.TitleLabel;
import com.eerussianguy.blazemap.lib.gui.components.VanillaComponents;
import com.eerussianguy.blazemap.lib.gui.core.VolatileContainer;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentContainer;

public class WaypointGroupEditorFragment extends BaseFragment {
    private final WaypointGroup group;

    public WaypointGroupEditorFragment(WaypointGroup group) {
        super(Helpers.translate("blazemap.gui.waypoint_group_editor.title"), true, false);
        this.group = group;
    }

    @Override
    public void compose(FragmentContainer container, VolatileContainer volatiles) {
        container.setBaseWidth(160);

        if(container.titleConsumer.isPresent()) {
            container.titleConsumer.get().accept(getTitle());
        } else {
            container.addRow(new TitleLabel(getTitle()));
        }

        ObjHolder<String> name = new ObjHolder<>(group.getNameString());
        container.addRow(VanillaComponents.makeTextField(font, 160, name)).fill();

        TextButton submit = new TextButton(Helpers.translate("blazemap.gui.button.save"), button -> {
            group.setUserGivenName(name.get());
            container.dismiss();
        });
        container.addRow(submit.setSize(80, 20)).shouldCenter();

        container.finalise();
    }
}
