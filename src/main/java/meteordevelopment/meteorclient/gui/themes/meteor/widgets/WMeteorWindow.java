/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

public class WMeteorWindow extends WWindow implements MeteorWidget {
    public WMeteorWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WMeteorHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            renderer.quad(x, y + header.height, width, height - header.height, theme().backgroundColor.get());
            double border = theme().scale(1);
            renderer.quad(x, y + header.height, width, border, theme().outlineColor.get());
            renderer.quad(x, y + height - border, width, border, theme().outlineColor.get());
            renderer.quad(x, y + header.height, border, height - header.height, theme().outlineColor.get());
            renderer.quad(x + width - border, y + header.height, border, height - header.height, theme().outlineColor.get());
        }
    }

    private class WMeteorHeader extends WHeader {
        public WMeteorHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.quad(this, theme().backgroundColor.get());
            renderer.quad(x, y, theme().scale(3), height, theme().accentColor.get());
            renderer.quad(x + theme().scale(3), y + height - theme().scale(1), width - theme().scale(3), theme().scale(1), theme().outlineColor.get());
        }
    }
}
