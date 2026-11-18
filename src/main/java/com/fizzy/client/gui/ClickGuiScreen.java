package com.fizzy.client.gui;

import com.fizzy.client.FizzyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fizzy's Aoba-inspired Click GUI port.
 *
 * The category-window layout, navigation pages, module rows, rounded panels,
 * and settings overlay follow Aoba's GUI design. This implementation is
 * deliberately native to Fizzy's 1.21.11 codebase; Aoba's old event, module,
 * mixin and rendering systems are not imported.
 */
public final class ClickGuiScreen extends Screen {
    private static final int ACCENT = 0xFFB05CFF;
    private static final int ACCENT_DARK = 0xFF7138A8;
    private static final int PANEL = 0xE9161420;
    private static final int PANEL_TOP = 0xFF211D2B;
    private static final int CARD = 0xFF292533;
    private static final int CARD_HOVER = 0xFF383044;
    private static final int BORDER = 0xFF484052;
    private static final int TEXT = 0xFFF4F0FA;
    private static final int MUTED = 0xFFAAA2B8;

    private static final List<Category> CATEGORIES = List.of(
        new Category("COMBAT", "C", List.of()),
        new Category("MOVEMENT", "M", List.of("Toggle Sprint", "Speed Display")),
        new Category("RENDER", "R", List.of("FPS Display", "Coordinates", "Keystrokes",
            "Armor HUD", "Totem Counter", "Durability Warning")),
        new Category("WORLD", "W", List.of("World Time", "Base Hunting", "Storage Finder",
            "Portal Finder", "Spawner Finder", "Block Finder", "Waypoints")),
        new Category("MISC", "⋯", List.of())
    );

    private static final List<String> HUD_MODULES = List.of(
        "FPS Display", "Coordinates", "Keystrokes", "Armor HUD",
        "Totem Counter", "Durability Warning", "World Time", "Speed Display"
    );
    private static final List<String> TOOL_MODULES = List.of(
        "Base Hunting", "Storage Finder", "Portal Finder", "Spawner Finder", "Block Finder", "Waypoints"
    );

    private final Map<String, PanelPosition> panelPositions = new LinkedHashMap<>();
    private final Map<String, Rect> moduleRects = new LinkedHashMap<>();
    private String activePage = "MODULES";
    private String selectedModule;
    private String draggingPanel;
    private double dragOffsetX, dragOffsetY;
    private float openProgress;

    public ClickGuiScreen() {
        super(Component.literal("Fizzy Client"));
        int x = 24;
        for (Category category : CATEGORIES) {
            panelPositions.put(category.name, new PanelPosition(x, 105));
            x += 194;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        openProgress = Math.min(1.0f, openProgress + 0.16f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA6080610);
        float eased = 1.0f - (float) Math.pow(1.0f - openProgress, 3);
        int panelWidth = Math.min(1180, width - 32);
        int panelHeight = Math.min(690, height - 32);
        panelWidth = Math.max(500, panelWidth);
        panelHeight = Math.max(340, panelHeight);
        int w = (int) (panelWidth * (0.965f + 0.035f * eased));
        int h = (int) (panelHeight * (0.965f + 0.035f * eased));
        int left = (width - w) / 2;
        int top = (height - h) / 2;

        rounded(g, left, top, w, h, 10, PANEL);
        outline(g, left, top, w, h, BORDER);
        rounded(g, left + 1, top + 1, w - 2, 58, 9, PANEL_TOP);
        g.fill(left + 1, top + 52, left + w - 1, top + 59, PANEL_TOP);

        rounded(g, left + 17, top + 15, 30, 30, 7, ACCENT_DARK);
        centered(g, "F", left + 17, top + 15, 30, 30, TEXT);
        g.drawString(font, "FIZZY CLIENT", left + 58, top + 16, TEXT, false);
        g.drawString(font, "1.21.11  •  INDEPENDENT CLIENT", left + 58, top + 32, MUTED, false);

        String status = FizzyClient.MODULES.getStates().size() + " MODULES";
        int statusWidth = font.width(status) + 20;
        rounded(g, left + w - statusWidth - 18, top + 18, statusWidth, 23, 7, 0xFF302A3D);
        centered(g, status, left + w - statusWidth - 18, top + 18, statusWidth, 23, ACCENT);

        drawNavigation(g, left, top, w, mouseX, mouseY);
        g.fill(left + 17, top + 91, left + w - 17, top + 92, BORDER);

        if (activePage.equals("MODULES")) renderModuleWindows(g, left, top, w, h, mouseX, mouseY);
        else if (activePage.equals("HUD")) renderHudWindow(g, left, top, w, h, mouseX, mouseY);
        else renderToolsWindow(g, left, top, w, h, mouseX, mouseY);

        g.drawString(font, "LEFT CLICK  •  TOGGLE     RIGHT CLICK  •  SETTINGS     DRAG HEADERS  •  MOVE WINDOWS",
            left + 20, top + h - 17, MUTED, false);

        if (selectedModule != null) renderSettings(g, left, top, w, h);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawNavigation(GuiGraphics g, int left, int top, int panelWidth, int mx, int my) {
        String[] tabs = {"MODULES", "TOOLS", "HUD"};
        int x = left + 18;
        for (String tab : tabs) {
            int tw = tab.equals("MODULES") ? 88 : 70;
            boolean active = activePage.equals(tab);
            boolean hovered = inside(mx, my, x, top + 65, tw, 23);
            if (active) rounded(g, x, top + 65, tw, 23, 6, ACCENT_DARK);
            else if (hovered) rounded(g, x, top + 65, tw, 23, 6, CARD_HOVER);
            centered(g, tab, x, top + 65, tw, 23, active ? TEXT : MUTED);
            x += tw + 7;
        }
    }

    private void renderModuleWindows(GuiGraphics g, int left, int top, int panelWidth,
                                     int panelHeight, int mx, int my) {
        moduleRects.clear();
        int availableWidth = panelWidth - 40;
        int panelW = Math.min(184, Math.max(150, (availableWidth - 40) / 5));
        int panelTop = top + 105;
        for (Category category : CATEGORIES) {
            PanelPosition p = panelPositions.get(category.name);
            int x = left + p.x;
            int y = top + p.y - 105 + panelTop;
            int panelH = Math.min(panelHeight - (y - top) - 54,
                43 + category.modules.size() * 34);
            panelH = Math.max(88, panelH);

            rounded(g, x, y, panelW, panelH, 7, PANEL_TOP);
            outline(g, x, y, panelW, panelH, BORDER);
            rounded(g, x + 8, y + 8, 22, 22, 5, 0xFF393044);
            centered(g, category.icon, x + 8, y + 8, 22, 22, ACCENT);
            g.drawString(font, category.name, x + 37, y + 15, TEXT, false);
            g.fill(x + 8, y + 37, x + panelW - 8, y + 38, BORDER);

            int moduleY = y + 44;
            if (category.modules.isEmpty()) {
                g.drawString(font, "No modules yet", x + 10, moduleY + 5, MUTED, false);
            }
            for (String module : category.modules) {
                Rect r = new Rect(x + 7, moduleY, panelW - 14, 29);
                moduleRects.put(module, r);
                boolean hovered = contains(r, mx, my);
                boolean enabled = FizzyClient.MODULES.isEnabled(module);
                rounded(g, r.x, r.y, r.w, r.h, 5, hovered ? CARD_HOVER : CARD);
                g.drawString(font, module, r.x + 8, r.y + 10, enabled ? TEXT : 0xFFD4CEDD, false);
                int sx = r.x + r.w - 29;
                rounded(g, sx, r.y + 8, 21, 13, 7, enabled ? ACCENT_DARK : 0xFF4A4554);
                rounded(g, enabled ? sx + 11 : sx + 2, r.y + 10, 9, 9, 5, 0xFFFFFFFF);
                moduleY += 33;
            }
        }
    }

    private void renderHudWindow(GuiGraphics g, int left, int top, int w, int h, int mx, int my) {
        renderSimpleList(g, left + 22, top + 105, w - 44, "HUD ELEMENTS",
            HUD_MODULES, mx, my);
    }

    private void renderToolsWindow(GuiGraphics g, int left, int top, int w, int h, int mx, int my) {
        renderSimpleList(g, left + 22, top + 105, w - 44, "WORLD TOOLS",
            TOOL_MODULES, mx, my);
    }

    private void renderSimpleList(GuiGraphics g, int x, int y, int w, String title,
                                  List<String> modules, int mx, int my) {
        int h = 46 + modules.size() * 36;
        rounded(g, x, y, w, h, 8, PANEL_TOP);
        outline(g, x, y, w, h, BORDER);
        g.drawString(font, title, x + 14, y + 15, ACCENT, false);
        g.fill(x + 12, y + 36, x + w - 12, y + 37, BORDER);
        moduleRects.clear();
        int rowY = y + 43;
        for (String module : modules) {
            Rect r = new Rect(x + 10, rowY, w - 20, 30);
            moduleRects.put(module, r);
            boolean hovered = contains(r, mx, my);
            boolean enabled = FizzyClient.MODULES.isEnabled(module);
            rounded(g, r.x, r.y, r.w, r.h, 5, hovered ? CARD_HOVER : CARD);
            g.drawString(font, module, r.x + 10, r.y + 11, TEXT, false);
            int sx = r.x + r.w - 30;
            rounded(g, sx, r.y + 8, 21, 13, 7, enabled ? ACCENT_DARK : 0xFF4A4554);
            rounded(g, enabled ? sx + 11 : sx + 2, r.y + 10, 9, 9, 5, 0xFFFFFFFF);
            rowY += 35;
        }
    }

    private void renderSettings(GuiGraphics g, int left, int top, int w, int h) {
        int sw = 350, sh = 220;
        int x = left + (w - sw) / 2, y = top + (h - sh) / 2;
        rounded(g, x - 6, y - 6, sw + 12, sh + 12, 11, 0xDD08070D);
        rounded(g, x, y, sw, sh, 9, PANEL_TOP);
        outline(g, x, y, sw, sh, ACCENT_DARK);
        g.drawString(font, "MODULE SETTINGS", x + 16, y + 16, ACCENT, false);
        g.drawString(font, selectedModule, x + 16, y + 39, TEXT, false);
        String description = switch (selectedModule) {
            case "Toggle Sprint" -> "Automatically sprint while moving forward.";
            case "Speed Display" -> "Display horizontal movement speed.";
            case "FPS Display" -> "Display your current frames per second.";
            case "Coordinates" -> "Display your current block coordinates.";
            case "Keystrokes" -> "Display movement and jump key presses.";
            case "Armor HUD" -> "Display equipped armor durability.";
            case "Totem Counter" -> "Count Totems of Undying in your inventory.";
            case "Durability Warning" -> "Warn when an item has low durability.";
            case "World Time" -> "Display the current in-game time.";
            case "Base Hunting" -> "Scan nearby loaded chunks for selected blocks.";
            case "Storage Finder" -> "Find loaded chests, ender chests and barrels.";
            case "Portal Finder" -> "Find loaded Nether portal blocks.";
            case "Spawner Finder" -> "Find loaded mob spawners.";
            case "Block Finder" -> "Find selected utility and building blocks.";
            case "Waypoints" -> "Save and display your current coordinates.";
            default -> "Module settings.";
        };
        drawWrapped(g, description, x + 16, y + 65, sw - 32, MUTED);
        if (isScanner(selectedModule)) {
            g.drawString(font, "Scan radius: " + FizzyClient.MODULES.getScanRange() + " blocks",
                x + 16, y + 112, TEXT, false);
            String[] labels = {"-8", "-1", "+1", "+8"};
            for (int i = 0; i < labels.length; i++) settingButton(g, x + 16 + i * 65, y + 132, 58, labels[i]);
            g.drawString(font, "Only loaded client chunks are scanned.", x + 16, y + 165, MUTED, false);
        } else if (selectedModule.equals("Waypoints")) {
            settingButton(g, x + 16, y + 126, sw - 32, "SAVE CURRENT POSITION");
            g.drawString(font, "Saved: " + FizzyClient.MODULES.getWaypoints().size(), x + 16, y + 160, MUTED, false);
        } else {
            g.drawString(font, "No extra settings for this module yet.", x + 16, y + 122, MUTED, false);
        }
        settingButton(g, x + 16, y + sh - 34, sw - 32, "CLOSE SETTINGS");
    }

    private void settingButton(GuiGraphics g, int x, int y, int w, String label) {
        rounded(g, x, y, w, 22, 5, 0xFF393044);
        centered(g, label, x, y, w, 22, TEXT);
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        StringBuilder line = new StringBuilder();
        int lineY = y;
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(font, line.toString(), x, lineY, color, false);
                lineY += 12;
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) g.drawString(font, line.toString(), x, lineY, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        int left = (width - Math.min(1180, width - 32)) / 2;
        int top = (height - Math.min(690, height - 32)) / 2;
        int w = Math.min(1180, width - 32);

        if (selectedModule != null) {
            int sw = 350, sh = 220, x = left + (w - sw) / 2, y = top + (Math.min(690, height - 32) - sh) / 2;
            if (isScanner(selectedModule)) {
                if (inside(mx, my, x + 16, y + 132, 58, 22)) FizzyClient.MODULES.adjustScanRange(-8);
                else if (inside(mx, my, x + 81, y + 132, 58, 22)) FizzyClient.MODULES.adjustScanRange(-1);
                else if (inside(mx, my, x + 146, y + 132, 58, 22)) FizzyClient.MODULES.adjustScanRange(1);
                else if (inside(mx, my, x + 211, y + 132, 58, 22)) FizzyClient.MODULES.adjustScanRange(8);
            } else if (selectedModule.equals("Waypoints") && inside(mx, my, x + 16, y + 126, sw - 32, 22)) {
                FizzyClient.MODULES.saveWaypoint(Minecraft.getInstance());
            }
            selectedModule = null;
            return true;
        }

        int tabX = left + 18;
        for (String tab : new String[]{"MODULES", "TOOLS", "HUD"}) {
            int tw = tab.equals("MODULES") ? 88 : 70;
            if (inside(mx, my, tabX, top + 65, tw, 23)) {
                activePage = tab;
                return true;
            }
            tabX += tw + 7;
        }

        if (button == 1) {
            for (var entry : moduleRects.entrySet()) {
                if (contains(entry.getValue(), mx, my)) {
                    selectedModule = entry.getKey();
                    return true;
                }
            }
        }
        if (button == 0) {
            for (var entry : moduleRects.entrySet()) {
                if (contains(entry.getValue(), mx, my)) {
                    FizzyClient.MODULES.toggle(entry.getKey());
                    return true;
                }
            }
            if (activePage.equals("MODULES")) {
                int panelW = Math.min(184, Math.max(150, (w - 80) / 5));
                for (Category category : CATEGORIES) {
                    PanelPosition p = panelPositions.get(category.name);
                    int x = left + p.x, y = top + p.y;
                    if (inside(mx, my, x, y, panelW, 38)) {
                        draggingPanel = category.name;
                        dragOffsetX = mx - x;
                        dragOffsetY = my - y;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (draggingPanel != null) {
            PanelPosition p = panelPositions.get(draggingPanel);
            int left = (width - Math.min(1180, width - 32)) / 2;
            int top = (height - Math.min(690, height - 32)) / 2;
            p.x = Math.max(0, Math.min(width - left - 190, (int) (click.x() + dragX - dragOffsetX - left)));
            p.y = Math.max(105, Math.min(height - top - 90, (int) (click.y() + dragY - dragOffsetY - top)));
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingPanel = null;
        return super.mouseReleased(click);
    }

    private static boolean isScanner(String module) {
        return module.equals("Base Hunting") || module.equals("Storage Finder")
            || module.equals("Portal Finder") || module.equals("Spawner Finder") || module.equals("Block Finder");
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 || event.key() == 344) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    private static boolean contains(Rect r, double x, double y) {
        return inside(x, y, r.x, r.y, r.w, r.h);
    }
    private void centered(GuiGraphics g, String s, int x, int y, int w, int h, int color) {
        g.drawString(font, s, x + (w - font.width(s)) / 2, y + (h - 8) / 2, color, false);
    }
    private static void rounded(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        int r = Math.min(radius, Math.min(w / 2, h / 2));
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < r) {
                double dy = r - row - 0.5;
                inset = r - (int) Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
            } else if (row >= h - r) {
                double dy = row - (h - r) + 0.5;
                inset = r - (int) Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
            }
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }
    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private record Category(String name, String icon, List<String> modules) {}
    private record Rect(int x, int y, int w, int h) {}
    private static final class PanelPosition {
        int x, y;
        PanelPosition(int x, int y) { this.x = x; this.y = y; }
    }
}
