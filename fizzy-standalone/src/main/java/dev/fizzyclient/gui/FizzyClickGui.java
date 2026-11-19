package dev.fizzyclient.gui;

import dev.fizzyclient.core.module.FizzyModule;
import dev.fizzyclient.FizzyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

public final class FizzyClickGui extends Screen {
    private static final int PANEL_W = 310;
    private static final int ROW_H = 30;

    public FizzyClickGui() {
        super(Text.literal("Fizzy Client"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = (width - PANEL_W) / 2;
        int y = Math.max(35, (height - 260) / 2);

        context.fill(0, 0, width, height, 0x99070B14);
        context.fill(x - 1, y - 1, x + PANEL_W + 1, y + 245, 0xFF9DBCEB);
        context.fillGradient(x, y, x + PANEL_W, y + 244, 0xFF182B49, 0xFF101A2D);
        context.fillGradient(x, y, x + PANEL_W, y + 36, 0xFF315A91, 0xFF1C3558);
        context.drawTextWithShadow(textRenderer, "FIZZY CLIENT", x + 13, y + 13, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "INDEPENDENT BUILD", x + 190, y + 14, 0xFFBBD7FF);

        int rowY = y + 48;
        for (FizzyModule module : FizzyClient.MODULES.getModules()) {
            int color = module.isEnabled() ? 0xFF315D91 : 0xFF202D41;
            context.fill(x + 10, rowY, x + PANEL_W - 10, rowY + ROW_H - 3, color);
            context.drawTextWithShadow(textRenderer, module.getName(), x + 20, rowY + 8, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, module.isEnabled() ? "ON" : "OFF",
                x + PANEL_W - 45, rowY + 8, module.isEnabled() ? 0xFF9DFFBD : 0xFFB6C0D0);
            rowY += ROW_H;
        }

        context.drawTextWithShadow(textRenderer, "LEFT CLICK TO TOGGLE  •  RIGHT SHIFT TO CLOSE",
            x + 12, y + 220, 0xFFB7C5D8);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            int x = (width - PANEL_W) / 2;
            int y = Math.max(35, (height - 260) / 2) + 48;
            int index = (int) ((click.y() - y) / ROW_H);
            if (click.x() >= x + 10 && click.x() <= x + PANEL_W - 10 && index >= 0
                && index < FizzyClient.MODULES.getModules().size()) {
                FizzyClient.MODULES.getModules().get(index).toggle();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
