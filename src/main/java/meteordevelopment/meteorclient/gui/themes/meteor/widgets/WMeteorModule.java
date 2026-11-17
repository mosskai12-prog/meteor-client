/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WMeteorModule extends WPressable implements MeteorWidget {
    private final Module module;
    private final String title;
    private double titleWidth;
    private double animationProgress1;
    private double animationProgress2;

    public WMeteorModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;
        animationProgress1 = module.isActive() ? 1 : 0;
        animationProgress2 = module.isActive() ? 1 : 0;
    }

    @Override
    public double pad() {
        return theme.scale(7);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        if (titleWidth == 0) titleWidth = theme.textWidth(title);
        width = pad + titleWidth + pad + theme.scale(12);
        height = pad + theme.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        double pad = pad();

        animationProgress1 = MathHelper.clamp(animationProgress1 + delta * 5 * ((module.isActive() || mouseOver) ? 1 : -1), 0, 1);
        animationProgress2 = MathHelper.clamp(animationProgress2 + delta * 7 * (module.isActive() ? 1 : -1), 0, 1);

        if (mouseOver || animationProgress1 > 0) {
            int alpha = (int) (18 + 30 * animationProgress1);
            renderer.quad(x, y, width, height, new meteordevelopment.meteorclient.utils.render.color.Color(111, 103, 255, alpha));
        }

        if (animationProgress2 > 0) {
            renderer.quad(x, y, theme.scale(2), height, theme.accentColor.get());
        }

        double labelX = x + pad;
        double availableWidth = width - pad * 2;
        if (theme.moduleAlignment.get() == AlignmentX.Center) labelX += availableWidth / 2 - titleWidth / 2;
        else if (theme.moduleAlignment.get() == AlignmentX.Right) labelX += availableWidth - titleWidth;

        renderer.text(title, labelX, y + pad, module.isActive() ? theme.textColor.get() : theme.textSecondaryColor.get(), false);

        double dotSize = theme.scale(5);
        double dotX = x + width - pad - dotSize;
        double dotY = y + height / 2 - dotSize / 2;
        renderer.quad(dotX, dotY, dotSize, dotSize, module.isActive() ? theme.accentColor.get() : theme.outlineColor.get());
    }
}
