package com.flippingai;

import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class RectangleOverlay extends Overlay {

    Widget widget;

    public RectangleOverlay(Widget widget) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
        this.widget = widget;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        graphics.setColor(Color.CYAN);

        if (widget != null) {
            Rectangle bounds = widget.getBounds();
            graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        return null;
    }
}