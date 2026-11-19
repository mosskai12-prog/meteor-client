/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 *
 * Modern single-panel redesign inspired by contemporary client UIs.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Modern Modules screen – single dark panel with:
 *  - Left sidebar (categories / favorites)
 *  - Center "module desk" (search + list)
 *  - Right settings inspector
 */
public class ModulesScreen extends TabScreen {

    // Colors matching the target modern dark aesthetic
    private static final int BG          = 0xFF0D0F14;
    private static final int HEADER      = 0xFF12151C;
    private static final int SIDEBAR     = 0xFF10131A;
    private static final int PANEL       = 0xFF151922;
    private static final int CARD        = 0xFF1A1F2B;
    private static final int CARD_HOVER  = 0xFF222836;
    private static final int CARD_SEL    = 0xFF1E2A3D;
    private static final int LINE        = 0xFF2A3140;
    private static final int ACCENT      = 0xFF3B82F6;   // blue accent
    private static final int ACCENT_DIM  = 0xFF2563EB;
    private static final int TEXT        = 0xFFF1F5F9;
    private static final int MUTED       = 0xFF94A3B8;
    private static final int GREEN       = 0xFF34D399;
    private static final int RED         = 0xFFF87171;

    private String selectedCategory = "All";
    private String search = "";
    private boolean searchFocused;
    private Module selectedModule;
    private boolean showEnabledOnly;

    private float scroll;
    private float targetScroll;
    private float openProgress;

    private final List<String> categories = new ArrayList<>();
    private final List<Module> visible = new ArrayList<>();

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
        rebuildCategories();
        rebuildVisible();
        if (!visible.isEmpty()) selectedModule = visible.get(0);
    }

    @Override
    public void initWidgets() {
        // We draw everything ourselves – no classic widgets needed
    }

    private void rebuildCategories() {
        categories.clear();
        categories.add("All");
        categories.add("Favorites");
        for (Category cat : Modules.loopCategories()) {
            categories.add(cat.name);
        }
    }

    private void rebuildVisible() {
        visible.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();

        for (Module m : Modules.get().getAll()) {
            if (showEnabledOnly && !m.isActive()) continue;

            if (selectedCategory.equals("Favorites")) {
                if (!m.favorite) continue;
            } else if (!selectedCategory.equals("All") && !m.category.name.equals(selectedCategory)) {
                continue;
            }

            if (!q.isEmpty()) {
                boolean match = m.title.toLowerCase(Locale.ROOT).contains(q)
                    || m.name.toLowerCase(Locale.ROOT).contains(q)
                    || m.description.toLowerCase(Locale.ROOT).contains(q);
                if (!match) continue;
            }

            visible.add(m);
        }

        visible.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.title, b.title));
        targetScroll = 0;
        scroll = 0;

        if (selectedModule != null && !visible.contains(selectedModule)) {
            selectedModule = visible.isEmpty() ? null : visible.get(0);
        }
    }

    // Layout helpers
    private int panelW() { return Math.min(1100, width - 32); }
    private int panelH() { return Math.min(640, height - 32); }
    private int panelX() { return (width - panelW()) / 2; }
    private int panelY() { return (height - panelH()) / 2 + Math.round((1f - openProgress) * 16); }

    private int sidebarW() { return 180; }
    private int settingsW() { return 280; }
    private int centerX() { return panelX() + sidebarW() + 16; }
    private int centerW() { return panelW() - sidebarW() - settingsW() - 40; }
    private int settingsX() { return panelX() + panelW() - settingsW() - 12; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        openProgress = Math.min(1f, openProgress + delta * 0.18f);

        // Dim background
        g.fill(0, 0, width, height, 0xCC000000);

        int x = panelX();
        int y = panelY();
        int w = panelW();
        int h = panelH();

        // Main shell
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w, y + 52, HEADER);
        g.fill(x, y + 51, x + w, y + 52, LINE);

        // Accent line on left edge
        g.fill(x, y, x + 3, y + h, ACCENT);

        // ── Header ──────────────────────────────────────────────
        // Logo square
        g.fill(x + 16, y + 12, x + 40, y + 36, ACCENT);
        g.drawCenteredString(font, "M", x + 28, y + 18, 0xFFFFFFFF);

        g.drawString(font, "METEOR CLIENT", x + 50, y + 14, TEXT, false);
        g.drawString(font, "MODULE CONTROL", x + 50, y + 28, MUTED, false);

        // Top tabs (visual only for now)
        String[] tabs = {"Modules", "HUD", "Appearance"};
        int tabX = x + w - 280;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = i == 0;
            int tw = font.width(tabs[i]) + 16;
            if (active) {
                g.fill(tabX, y + 40, tabX + tw, y + 52, ACCENT);
            }
            g.drawString(font, tabs[i], tabX + 8, y + 18, active ? TEXT : MUTED, false);
            tabX += tw + 8;
        }

        // Close hint
        g.drawString(font, "ESC", x + w - 36, y + 18, MUTED, false);

        // ── Left sidebar ────────────────────────────────────────
        int sideY = y + 52;
        g.fill(x, sideY, x + sidebarW(), y + h - 28, SIDEBAR);

        g.drawString(font, "WORKSPACE", x + 16, sideY + 14, MUTED, false);

        int catY = sideY + 36;
        for (String cat : categories) {
            boolean active = selectedCategory.equals(cat);
            boolean hover = mouseX >= x + 8 && mouseX <= x + sidebarW() - 8
                && mouseY >= catY && mouseY <= catY + 28;

            if (active) {
                g.fill(x + 8, catY, x + sidebarW() - 8, catY + 28, 0xFF1A2740);
                g.fill(x + 8, catY + 4, x + 11, catY + 24, ACCENT);
            } else if (hover) {
                g.fill(x + 8, catY, x + sidebarW() - 8, catY + 28, CARD);
            }

            String label = cat;
            if (cat.equals("Favorites")) label = "★  Favorites";
            g.drawString(font, label, x + 20, catY + 9, active ? TEXT : MUTED, false);
            catY += 32;
        }

        // Enabled only toggle at bottom of sidebar
        int enY = y + h - 56;
        g.fill(x + 12, enY, x + sidebarW() - 12, enY + 22, CARD);
        drawToggle(g, x + sidebarW() - 44, enY + 4, showEnabledOnly);
        g.drawString(font, "Enabled only", x + 20, enY + 7, MUTED, false);

        // ── Center – Module Desk ────────────────────────────────
        int cx = centerX();
        int cy = sideY + 12;

        g.drawString(font, "YOUR MODULE DESK", cx, cy, MUTED, false);
        g.drawString(font, selectedCategory, cx, cy + 14, TEXT, false);
        g.drawString(font, "Hover to explore. Select to configure.", cx, cy + 28, 0xFF64748B, false);

        // Search bar
        int searchY = cy + 48;
        int searchH = 28;
        g.fill(cx, searchY, cx + centerW(), searchY + searchH, searchFocused ? 0xFF1E293B : CARD);
        g.fill(cx, searchY, cx + centerW(), searchY + 1, searchFocused ? ACCENT : LINE);
        g.fill(cx, searchY + searchH - 1, cx + centerW(), searchY + searchH, searchFocused ? ACCENT : LINE);

        String searchDisplay = search.isEmpty() ? "Search modules..." : search;
        g.drawString(font, searchDisplay, cx + 10, searchY + 9,
            search.isEmpty() ? MUTED : TEXT, false);

        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caret = cx + 10 + font.width(search);
            g.fill(caret, searchY + 7, caret + 1, searchY + 21, TEXT);
        }

        // Module list
        int listTop = searchY + searchH + 12;
        int listBottom = y + h - 40;
        int rowH = 44;

        // Smooth scroll
        targetScroll = clamp(targetScroll, 0, Math.max(0, visible.size() * rowH - (listBottom - listTop)));
        scroll += (targetScroll - scroll) * 0.25f;

        g.enableScissor(cx, listTop, cx + centerW(), listBottom);

        if (visible.isEmpty()) {
            g.drawString(font, "No modules found", cx + 8, listTop + 20, MUTED, false);
        } else {
            for (int i = 0; i < visible.size(); i++) {
                Module m = visible.get(i);
                int ry = listTop + i * rowH - Math.round(scroll);

                if (ry + rowH < listTop || ry > listBottom) continue;

                boolean sel = m == selectedModule;
                boolean hover = mouseX >= cx && mouseX <= cx + centerW()
                    && mouseY >= ry && mouseY <= ry + rowH - 4;

                int bg = sel ? CARD_SEL : (hover ? CARD_HOVER : CARD);
                g.fill(cx, ry, cx + centerW(), ry + rowH - 4, bg);

                if (sel) g.fill(cx, ry, cx + 3, ry + rowH - 4, ACCENT);

                // Name + status
                g.drawString(font, m.title, cx + 14, ry + 8, TEXT, false);
                String status = m.isActive() ? "ACTIVE" : "INACTIVE";
                g.drawString(font, status, cx + 14, ry + 22, m.isActive() ? GREEN : MUTED, false);

                // Favorite star
                g.drawString(font, m.favorite ? "★" : "☆", cx + centerW() - 70, ry + 14,
                    m.favorite ? 0xFFFBBF24 : MUTED, false);

                // Toggle
                drawToggle(g, cx + centerW() - 42, ry + 12, m.isActive());
            }
        }

        g.disableScissor();

        // ── Right – Module Settings ─────────────────────────────
        int sx = settingsX();
        g.fill(sx, sideY, x + w - 8, y + h - 28, PANEL);
        g.fill(sx, sideY, sx + 1, y + h - 28, LINE);

        g.drawString(font, "MODULE SETTINGS", sx + 14, sideY + 14, MUTED, false);

        if (selectedModule == null) {
            g.drawString(font, "Select a module", sx + 14, sideY + 48, TEXT, false);
            g.drawString(font, "to configure it here.", sx + 14, sideY + 64, MUTED, false);
        } else {
            g.drawString(font, selectedModule.title, sx + 14, sideY + 40, TEXT, false);
            // Description truncated
            String desc = selectedModule.description;
            if (font.width(desc) > settingsW() - 30) {
                while (font.width(desc + "...") > settingsW() - 30 && desc.length() > 3)
                    desc = desc.substring(0, desc.length() - 1);
                desc += "...";
            }
            g.drawString(font, desc, sx + 14, sideY + 56, MUTED, false);

            // Enabled row
            int row = sideY + 88;
            g.fill(sx + 10, row, x + w - 18, row + 32, CARD);
            g.drawString(font, "Enabled", sx + 20, row + 11, TEXT, false);
            drawToggle(g, x + w - 52, row + 8, selectedModule.isActive());

            // Quick info
            row += 44;
            g.drawString(font, "Category", sx + 14, row, MUTED, false);
            g.drawString(font, selectedModule.category.name, sx + 14, row + 14, TEXT, false);

            row += 40;
            g.drawString(font, "Keybind", sx + 14, row, MUTED, false);
            g.drawString(font, selectedModule.keybind.isSet() ? selectedModule.keybind.toString() : "None",
                sx + 14, row + 14, TEXT, false);

            row += 40;
            g.drawString(font, "Right-click a module in the list", sx + 14, row, 0xFF64748B, false);
            g.drawString(font, "or press the settings button", sx + 14, row + 12, 0xFF64748B, false);
            g.drawString(font, "to open full configuration.", sx + 14, row + 24, 0xFF64748B, false);

            // Open full settings button
            row += 52;
            boolean btnHover = mouseX >= sx + 10 && mouseX <= x + w - 18
                && mouseY >= row && mouseY <= row + 28;
            g.fill(sx + 10, row, x + w - 18, row + 28, btnHover ? ACCENT_DIM : ACCENT);
            g.drawCenteredString(font, "Open Settings", sx + (settingsW() / 2), row + 9, 0xFFFFFFFF);
        }

        // ── Footer ──────────────────────────────────────────────
        g.fill(x, y + h - 28, x + w, y + h, HEADER);
        g.fill(x, y + h - 29, x + w, y + h - 28, LINE);

        long activeCount = Modules.get().getAll().stream().filter(Module::isActive).count();
        g.drawString(font, "METEOR  /  YOUR WORKSPACE", x + 14, y + h - 18, MUTED, false);
        g.drawString(font, activeCount + " modules active", x + w - 120, y + h - 18, MUTED, false);

        super.render(g, mouseX, mouseY, delta);
    }

    private void drawToggle(GuiGraphics g, int x, int y, boolean on) {
        g.fill(x, y, x + 30, y + 16, on ? 0xFF166534 : 0xFF334155);
        int knob = on ? x + 16 : x + 2;
        g.fill(knob, y + 2, knob + 12, y + 14, 0xFFFFFFFF);
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    // ── Input ───────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (button != 0 && button != 1) return super.mouseClicked(event, doubled);

        int x = panelX();
        int y = panelY();
        int sideY = y + 52;

        // Categories
        int catY = sideY + 36;
        for (String cat : categories) {
            if (mx >= x + 8 && mx <= x + sidebarW() - 8 && my >= catY && my <= catY + 28) {
                selectedCategory = cat;
                searchFocused = false;
                rebuildVisible();
                return true;
            }
            catY += 32;
        }

        // Enabled only
        int enY = y + panelH() - 56;
        if (mx >= x + 12 && mx <= x + sidebarW() - 12 && my >= enY && my <= enY + 22) {
            showEnabledOnly = !showEnabledOnly;
            rebuildVisible();
            return true;
        }

        // Search focus
        int cx = centerX();
        int searchY = sideY + 12 + 48;
        if (mx >= cx && mx <= cx + centerW() && my >= searchY && my <= searchY + 28) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        // Module rows
        int listTop = searchY + 28 + 12;
        int listBottom = y + panelH() - 40;
        int rowH = 44;

        for (int i = 0; i < visible.size(); i++) {
            Module m = visible.get(i);
            int ry = listTop + i * rowH - Math.round(scroll);
            if (ry + rowH < listTop || ry > listBottom) continue;

            if (mx >= cx && mx <= cx + centerW() && my >= ry && my <= ry + rowH - 4) {
                // Favorite star area
                if (mx >= cx + centerW() - 70 && mx <= cx + centerW() - 50) {
                    m.favorite = !m.favorite;
                    if (selectedCategory.equals("Favorites")) rebuildVisible();
                    return true;
                }
                // Toggle area
                if (mx >= cx + centerW() - 42) {
                    m.toggle();
                    return true;
                }

                if (button == 1) {
                    // Right click → open full module screen
                    mc.setScreen(theme.moduleScreen(m));
                    return true;
                }

                selectedModule = m;
                return true;
            }
        }

        // Open Settings button
        if (selectedModule != null) {
            int sx = settingsX();
            int row = sideY + 88 + 44 + 40 + 40 + 52;
            if (mx >= sx + 10 && mx <= x + panelW() - 18 && my >= row && my <= row + 28) {
                mc.setScreen(theme.moduleScreen(selectedModule));
                return true;
            }

            // Enabled toggle in settings
            int enableRow = sideY + 88;
            if (mx >= sx + 10 && mx <= x + panelW() - 18 && my >= enableRow && my <= enableRow + 32) {
                selectedModule.toggle();
                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScroll -= verticalAmount * 28;
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (searchFocused) {
            if (input.key() == GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                rebuildVisible();
                return true;
            }
            if (input.key() == GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (input.key() == GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent input) {
        if (searchFocused) {
            char c = (char) input.codepoint();
            if (c >= 32 && c != 127) {
                search += c;
                rebuildVisible();
                return true;
            }
        }
        return super.charTyped(input);
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    @Override
    public void reload() {
        rebuildCategories();
        rebuildVisible();
    }
}
