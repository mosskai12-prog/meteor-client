/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

final class FizzyClickGui extends WWidget {
    private static final Color PANEL = new Color(17, 17, 23, 252);
    private static final Color SIDEBAR = new Color(13, 13, 19, 255);
    private static final Color CARD = new Color(24, 24, 32, 255);
    private static final Color CARD_HOVER = new Color(34, 30, 47, 255);
    private static final Color PURPLE = new Color(166, 126, 255, 255);
    private static final Color TEXT = new Color(242, 239, 250, 255);
    private static final Color MUTED = new Color(139, 136, 153, 255);
    private static final Color OFF = new Color(57, 56, 68, 255);
    private static final Color BORDER = new Color(48, 44, 61, 255);

    private final List<Category> categories = new ArrayList<>();
    private Category selected;
    private String search = "";
    private boolean searchFocused;
    private double scroll;
    private double targetScroll;
    private double openProgress;
    private double selectedCategoryY = -1;
    private final Map<Module, Double> toggleProgress = new HashMap<>();
    private final Map<Category, Double> categoryHover = new HashMap<>();
    private final Map<Module, Double> moduleHover = new HashMap<>();

    private static double animate(double current, double target, double speed, double delta) {
        double factor = 1 - Math.exp(-speed * Math.min(delta, 0.05));
        return current + (target - current) * factor;
    }

    FizzyClickGui() {
        for (Category category : Modules.loopCategories()) categories.add(category);
        if (!categories.isEmpty()) selected = categories.get(0);
    }

    @Override
    protected void onCalculateSize() {
        width = getWindowWidth();
        height = getWindowHeight();
    }

    @Override
    protected void onCalculateWidgetPositions() {
        x = 0;
        y = 0;
        width = getWindowWidth();
        height = getWindowHeight();
    }

    private double panelWidth() {
        return Math.min(theme.scale(1040), width - theme.scale(36));
    }

    private double panelHeight() {
        return Math.min(theme.scale(680), height - theme.scale(36));
    }

    private double panelX() { return x + (width - panelWidth()) / 2; }
    private double panelY() { return y + (height - panelHeight()) / 2; }

    private void roundedRect(GuiRenderer renderer, double x, double y, double w, double h, double radius, Color color) {
        double diameter = radius * 2;
        renderer.quad(x + radius, y, w - diameter, h, color);
        renderer.quad(x, y + radius, radius, h - diameter, color);
        renderer.quad(x + w - radius, y + radius, radius, h - diameter, color);
        renderer.quad(x, y, diameter, diameter, GuiRenderer.CIRCLE, color);
        renderer.quad(x + w - diameter, y, diameter, diameter, GuiRenderer.CIRCLE, color);
        renderer.quad(x, y + h - diameter, diameter, diameter, GuiRenderer.CIRCLE, color);
        renderer.quad(x + w - diameter, y + h - diameter, diameter, diameter, GuiRenderer.CIRCLE, color);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        openProgress = animate(openProgress, 1, 8.5, delta);
        scroll = animate(scroll, targetScroll, 12, delta);
        double px = panelX(), py = panelY() + (1 - openProgress) * theme.scale(34), pw = panelWidth(), ph = panelHeight();
        double side = theme.scale(204);
        double pad = theme.scale(30);
        double rowH = theme.scale(82);
        double contentX = px + side + pad;
        double contentW = pw - side - pad * 2;
        double listY = py + theme.scale(138);
        double listBottom = py + ph - theme.scale(34);

        roundedRect(renderer, px, py, pw, ph, theme.scale(12), PANEL);
        roundedRect(renderer, px + theme.scale(1), py + theme.scale(1), pw - theme.scale(2), theme.scale(2), theme.scale(1), new Color(112, 83, 170, 150));
        renderer.quad(px + theme.scale(1), py + theme.scale(68), pw - theme.scale(2), theme.scale(1), BORDER);
        roundedRect(renderer, px + theme.scale(1), py + theme.scale(1), side, ph - theme.scale(2), theme.scale(11), SIDEBAR);
        renderer.quad(px + side - 1, py + theme.scale(12), 1, ph - theme.scale(24), new Color(43, 42, 49, 255));

        roundedRect(renderer, px + theme.scale(17), py + theme.scale(17), theme.scale(7), theme.scale(29), theme.scale(3), PURPLE);
        renderer.minecraftText("FIZZY CLIENT", px + theme.scale(34), py + theme.scale(22), TEXT);
         renderer.minecraftText("CLIENT MENU", px + theme.scale(34), py + theme.scale(46), MUTED);

        double categoryY = py + theme.scale(104);
        double selectedTargetY = categoryY;
        for (Category category : categories) {
            if (category.equals(selected)) selectedTargetY = categoryY;
            boolean active = category.equals(selected);
            double bx = px + theme.scale(9);
            double bw = side - theme.scale(18);
            double bh = theme.scale(32);
            boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= categoryY && mouseY <= categoryY + bh;
            double hoverAmount = categoryHover.compute(category, (key, value) -> animate(value == null ? 0 : value, hover ? 1 : 0, 14, delta));
            if (active || hoverAmount > 0.01) roundedRect(renderer, bx, categoryY, bw, bh, theme.scale(5), active ? new Color(43, 34, 64, 255) : new Color(19 + (int)(10 * hoverAmount), 21 + (int)(10 * hoverAmount), 42 + (int)(18 * hoverAmount), 255));
            if (active) renderer.quad(bx, categoryY, theme.scale(3), bh, PURPLE);
            renderer.minecraftText(category.name, bx + theme.scale(12), categoryY + theme.scale(8), active ? TEXT : MUTED);
            categoryY += theme.scale(41);
        }

        selectedCategoryY = animate(selectedCategoryY < 0 ? selectedTargetY : selectedCategoryY, selectedTargetY, 15, delta);
        renderer.quad(px + theme.scale(9), selectedCategoryY, theme.scale(3), theme.scale(32), PURPLE);

        String heading = selected == null ? "Modules" : selected.name + " Modules";
        renderer.minecraftText(heading, contentX, py + theme.scale(24), TEXT);
        renderer.quad(contentX, py + theme.scale(69), contentW, theme.scale(1), BORDER);

        int total = selected == null ? 0 : Modules.get().getGroup(selected).size();
        int enabled = 0;
        if (selected != null) for (Module module : Modules.get().getGroup(selected)) if (module.isActive()) enabled++;
        roundedRect(renderer, contentX, py + theme.scale(43), theme.scale(142), theme.scale(19), theme.scale(5), new Color(31, 27, 43, 255));
        renderer.minecraftText(enabled + " ACTIVE  /  " + total + " TOTAL", contentX + theme.scale(8), py + theme.scale(49), PURPLE);

        double searchY = py + theme.scale(21);
        double searchW = theme.scale(258);
        double searchX = px + pw - searchW - theme.scale(18);
        roundedRect(renderer, searchX, searchY, searchW, theme.scale(36), theme.scale(7), searchFocused ? new Color(34, 29, 47, 255) : new Color(25, 25, 33, 255));
        roundedRect(renderer, searchX, searchY, searchW, theme.scale(36), theme.scale(7), BORDER);
        renderer.minecraftText((search.isEmpty() ? "⌕  Search modules" : "⌕  " + search), searchX + theme.scale(12), searchY + theme.scale(11), search.isEmpty() ? MUTED : TEXT);
        if (searchFocused) renderer.quad(searchX, searchY + theme.scale(33), searchW, theme.scale(1), PURPLE);

        renderer.minecraftText("MODULE LIBRARY", contentX, py + theme.scale(106), MUTED);

        List<Module> modules = selected == null ? List.of() : Modules.get().getGroup(selected);
        double cardY = listY - scroll;
        int visibleIndex = 0;
        renderer.scissorStart(contentX, listY, contentW, listBottom - listY);
        for (Module module : modules) {
            if (!search.isEmpty() && !(module.title.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                || module.description.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))) continue;

            double entrance = Math.min(1, Math.max(0, (openProgress - visibleIndex * 0.035) / 0.72));
            double easedEntrance = 1 - Math.pow(1 - entrance, 3);
            double drawY = cardY + (1 - easedEntrance) * theme.scale(15);
            if (cardY + rowH >= listY && cardY <= listBottom) {
                boolean hover = mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= cardY && mouseY <= cardY + rowH - theme.scale(5);
                double hoverAmount = moduleHover.compute(module, (key, value) -> animate(value == null ? 0 : value, hover ? 1 : 0, 18, delta));
                double toggleAmount = toggleProgress.compute(module, (key, value) -> animate(value == null ? (module.isActive() ? 1 : 0) : value, module.isActive() ? 1 : 0, 16, delta));
                roundedRect(renderer, contentX, drawY, contentW, rowH - theme.scale(7), theme.scale(7), BORDER);
                roundedRect(renderer, contentX + theme.scale(1), drawY + theme.scale(1), contentW - theme.scale(2), rowH - theme.scale(9), theme.scale(6), hoverAmount > 0.02 ? CARD_HOVER : CARD);
                if (module.isActive()) roundedRect(renderer, contentX + theme.scale(1), drawY + theme.scale(10), theme.scale(3), rowH - theme.scale(27), theme.scale(1), PURPLE);

                renderer.minecraftText(module.title, contentX + theme.scale(17), cardY + theme.scale(15), TEXT);
                String description = module.description == null ? "" : module.description;
                if (description.length() > 58) description = description.substring(0, 55) + "...";
                renderer.minecraftText(description, contentX + theme.scale(17), cardY + theme.scale(43), MUTED);

                double arrowX = contentX + contentW - theme.scale(78);
                renderer.minecraftText("›", arrowX, cardY + theme.scale(23), MUTED);

                double toggleX = contentX + contentW - theme.scale(48);
                double toggleY = cardY + theme.scale(30);
                roundedRect(renderer, toggleX, toggleY, theme.scale(29), theme.scale(15), theme.scale(7), module.isActive() ? PURPLE : OFF);
                double knobX = toggleX + theme.scale(2 + 16 * toggleAmount);
                roundedRect(renderer, knobX, toggleY + theme.scale(3), theme.scale(9), theme.scale(9), theme.scale(4), TEXT);
            }
            cardY += rowH;
            visibleIndex++;
        }

        renderer.scissorEnd();
        if (modules.isEmpty()) renderer.minecraftText("No modules in this category.", contentX, listY, MUTED);
        else if (cardY <= listY) renderer.minecraftText("No modules match your search.", contentX, listY, MUTED);
    }

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW_MOUSE_BUTTON_LEFT && click.button() != GLFW_MOUSE_BUTTON_RIGHT) return false;
        boolean rightClick = click.button() == GLFW_MOUSE_BUTTON_RIGHT;
        double mx = click.x(), my = click.y();
        double px = panelX(), py = panelY(), pw = panelWidth(), ph = panelHeight();
        double side = theme.scale(190);

        if (mx < px || mx > px + pw || my < py || my > py + ph) return false;

        double categoryY = py + theme.scale(104);
        for (Category category : categories) {
            double bx = px + theme.scale(9), bw = side - theme.scale(18), bh = theme.scale(30);
            if (!rightClick && mx >= bx && mx <= bx + bw && my >= categoryY && my <= categoryY + bh) {
                selected = category;
                scroll = 0;
                targetScroll = 0;
                search = "";
                searchFocused = false;
                return true;
            }
            categoryY += theme.scale(41);
        }

        double searchW = theme.scale(258);
        double searchX = px + pw - searchW - theme.scale(18);
        double searchY = py + theme.scale(21);
        searchFocused = !rightClick && mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + theme.scale(30);

        double contentX = px + side + theme.scale(30);
        double contentW = pw - side - theme.scale(60);
        double cardY = py + theme.scale(138) - scroll;
        double rowH = theme.scale(82);
        if (selected != null) {
            for (Module module : Modules.get().getGroup(selected)) {
                if (!search.isEmpty() && !(module.title.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                    || module.description.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))) continue;
                if (mx >= contentX && mx <= contentX + contentW && my >= cardY && my <= cardY + rowH - theme.scale(7)) {
                    if (rightClick) {
                        mc.setScreen(theme.moduleScreen(module));
                    } else {
                        module.toggle();
                    }
                    return true;
                }
                cardY += rowH;
            }
        }
        return true;
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        targetScroll = Math.max(0, targetScroll - amount * theme.scale(54));
        return true;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        if (!searchFocused) return false;
        if (!Character.isISOControl(input.codepoint())) {
            search += input.asString();
            scroll = 0;
            targetScroll = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyPressed(KeyInput input) {
        if (!searchFocused) return false;
        if (input.key() == GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            scroll = 0;
            targetScroll = 0;
            return true;
        }
        if (input.key() == GLFW_KEY_ESCAPE) {
            searchFocused = false;
            return true;
        }
        return false;
    }
}
