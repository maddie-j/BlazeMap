package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;

import com.eerussianguy.blazemap.api.BlazeRegistry.Key;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;

public class LayerButton extends ImageButton {
    private final Key<Layer> key;
    private final MapType parent;
    private final IMapHost host;

    public LayerButton(int px, int py, int w, int h, Key<Layer> key, MapType parent, IMapHost host) {
        super(px, py, w, h, 0, 0, 0, key.value().getIcon(), w, h, button -> {
            host.toggleLayer(key);
        }, key.value().getName());

        this.key = key;
        this.parent = parent;
        this.host = host;

        checkVisible();
        setTooltip(Tooltip.create(key.value().getName()));
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float partial) {
        if(host.isLayerVisible(key))
            RenderHelper.setShaderColor(0xFFFFDD00);
        else
            RenderHelper.setShaderColor(Colors.NO_TINT);
        super.render(graphics, mx, my, partial);
        RenderHelper.setShaderColor(Colors.NO_TINT);
    }

    public void checkVisible() {
        this.visible = host.getMapType() == parent;
    }

    public void forceVisible() {
        this.visible = true;
    }
}
