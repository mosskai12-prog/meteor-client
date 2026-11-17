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

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

final class FizzyClickGui extends WWidget {
    private static final Color PANEL = new Color(12, 13, 29, 250);
    private static final Color SIDEBAR = new Color(9, 10, 24, 255);
    private static final Color CARD = new Color(19, 21, 42, 255);
    private static final Color CARD_HOVER = new Color(29, 31, 58, 255);
    private static final Color PURPLE = new Color(119, 91, 255, 255);
    private static final Color TEXT = new Color(239, 239, 250, 255);
    private static final Color MUTED = new Color(139, 143, 169, 255);
    private static final Color OFF = new Color(57, 60, 83, 255);

    private final List<Category> categories = new ArrayList<>();
    private Category selected;
    private String search = "";
    private boolean searchFocused;
    private double scroll;

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
        return Math.min(theme.scale(760), width - theme.scale(28));
    }

    private double panelHeight() {
        return Math.min(theme.scale(500), height - theme.scale(28));
    }

    private double panelX() { return x + (width - panelWidth()) / 2; }
    private double panelY() { return y + (height - panelHeight()) / 2; }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double px = panelX(), py = panelY(), pw = panelWidth(), ph = panelHeight();
        double side = theme.scale(154);
        double pad = theme.scale(15);
        double rowH = theme.scale(48);
        double contentX = px + side + pad;
        double contentW = pw - side - pad * 2;
        double listY = py + theme.scale(104);
        double listBottom = py + ph - theme.scale(18);

        renderer.quad(px, py, pw, ph, PANEL);
        renderer.quad(px, py, side, ph, SIDEBAR);
        renderer.quad(px + side - 1, py + theme.scale(12), 1, ph - theme.scale(24), new Color(48, 46, 83, 255));

        renderer.text("FIZZY CLIENT", px + theme.scale(18), py + theme.scale(17), TEXT, true);
        renderer.text("MODULES", px + theme.scale(18), py + theme.scale(38), MUTED, false);

        double categoryY = py + theme.scale(78);
        for (Category category : categories) {
            boolean active = category.equals(selected);
            double bx = px + theme.scale(9);
            double bw = side - theme.scale(18);
            double bh = theme.scale(27);
            boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= categoryY && mouseY <= categoryY + bh;
            if (active || hover) renderer.quad(bx, categoryY, bw, bh, active ? new Color(48, 39, 99, 255) : CARD_HOVER);
            if (active) renderer.quad(bx, categoryY, theme.scale(2), bh, PURPLE);
            renderer.text(category.name, bx + theme.scale(12), categoryY + theme.scale(8), active ? TEXT : MUTED, false);
            categoryY += theme.scale(34);
        }

        String heading = selected == null ? "Modules" : selected.name + " Modules";
        renderer.text(heading, contentX, py + theme.scale(18), TEXT, true);

        int total = selected == null ? 0 : Modules.get().getGroup(selected).size();
        int enabled = 0;
        if (selected != null) for (Module module : Modules.get().getGroup(selected)) if (module.isActive()) enabled++;
        renderer.text(enabled + " enabled  •  " + total + " modules", contentX, py + theme.scale(42), MUTED, false);

        double searchY = py + theme.scale(16);
        double searchW = theme.scale(185);
        double searchX = px + pw - searchW - theme.scale(18);
        renderer.quad(searchX, searchY, searchW, theme.scale(29), new Color(22, 24, 45, 255));
        renderer.text((search.isEmpty() ? "⌕  Search modules" : "⌕  " + search), searchX + theme.scale(10), searchY + theme.scale(9), search.isEmpty() ? MUTED : TEXT, false);
        if (searchFocused) renderer.quad(searchX, searchY + theme.scale(28), searchW, theme.scale(1), PURPLE);

        renderer.text("AVAILABLE MODULES", contentX, py + theme.scale(78), MUTED, false);

        List<Module> modules = selected == null ? List.of() : Modules.get().getGroup(selected);
        double cardY = listY - scroll;
        for (Module module : modules) {
            if (!search.isEmpty() && !(module.title.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                || module.description.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))) continue;

            if (cardY + rowH >= listY && cardY <= listBottom) {
                boolean hover = mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= cardY && mouseY <= cardY + rowH - theme.scale(5);
                renderer.quad(contentX, cardY, contentW, rowH - theme.scale(5), hover ? CARD_HOVER : CARD);

                renderer.text(module.title, contentX + theme.scale(13), cardY + theme.scale(9), TEXT, true);
                String description = module.description == null ? "" : module.description;
                if (description.length() > 78) description = description.substring(0, 75) + "...";
                renderer.text(description, contentX + theme.scale(13), cardY + theme.scale(28), MUTED, false);

                double arrowX = contentX + contentW - theme.scale(63);
                renderer.text("›", arrowX, cardY + theme.scale(14), MUTED, false);

                double toggleX = contentX + contentW - theme.scale(43);
                double toggleY = cardY + theme.scale(15);
                renderer.quad(toggleX, toggleY, theme.scale(25), theme.scale(13), module.isActive() ? PURPLE : OFF);
                double knobX = module.isActive() ? toggleX + theme.scale(14) : toggleX + theme.scale(2);
                renderer.quad(knobX, toggleY + theme.scale(2), theme.scale(9), theme.scale(9), TEXT);
            }
            cardY += rowH;
        }

        if (modules.isEmpty()) renderer.text("No modules in this category.", contentX, listY, MUTED, false);
        else if (cardY <= listY) renderer.text("No modules match your search.", contentX, listY, MUTED, false);
    }

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW_MOUSE_BUTTON_LEFT) return false;
        double mx = click.x(), my = click.y();
        double px = panelX(), py = panelY(), pw = panelWidth(), ph = panelHeight();
        double side = theme.scale(154);

        if (mx < px || mx > px + pw || my < py || my > py + ph) return false;

        double categoryY = py + theme.scale(78);
        for (Category category : categories) {
            double bx = px + theme.scale(9), bw = side - theme.scale(18), bh = theme.scale(27);
            if (mx >= bx && mx <= bx + bw && my >= categoryY && my <= categoryY + bh) {
                selected = category;
                scroll = 0;
                search = "";
                searchFocused = false;
                return true;
            }
            categoryY += theme.scale(34);
        }

        double searchW = theme.scale(185);
        double searchX = px + pw - searchW - theme.scale(18);
        double searchY = py + theme.scale(16);
        searchFocused = mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + theme.scale(30);

        double contentX = px + side + theme.scale(15);
        double contentW = pw - side - theme.scale(30);
        double cardY = py + theme.scale(104) - scroll;
        double rowH = theme.scale(48);
        if (selected != null) {
            for (Module module : Modules.get().getGroup(selected)) {
                if (!search.isEmpty() && !(module.title.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                    || module.description.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))) continue;
                if (mx >= contentX && mx <= contentX + contentW && my >= cardY && my <= cardY + rowH - theme.scale(5)) {
                    double localX = mx - contentX;
                    if (localX >= contentW - theme.scale(58) && localX < contentW - theme.scale(32)) {
                        mc.setScreen(theme.moduleScreen(module));
                    } else if (localX >= contentW - theme.scale(32)) {
                        module.toggle();
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
        scroll = Math.max(0, scroll - amount * theme.scale(24));
        return true;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        if (!searchFocused) return false;
        if (!Character.isISOControl(input.codepoint())) {
            search += input.asString();
            scroll = 0;
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
            return true;
        }
        if (input.key() == GLFW_KEY_ESCAPE) {
            searchFocused = false;
            return true;
        }
        return false;
    }
}
