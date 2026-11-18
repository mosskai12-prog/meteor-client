package com.fizzy.client.gui;

import com.fizzy.client.FizzyClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
    private static final int PURPLE = 0xFF9B72F2;
    private static final int PURPLE_DARK = 0xFF7046C8;
    private static final int PANEL = 0xF0181721;
    private static final int CARD = 0xFF292635;
    private static final int CARD_HOVER = 0xFF343044;
    private static final int MUTED = 0xFF9B96AA;
    private static final int WHITE = 0xFFF4F1FA;

    private static final List<Category> CATEGORIES = List.of(
        new Category("COMBAT", "C", List.of()),
        new Category("MOVEMENT", "M", List.of("Toggle Sprint")),
        new Category("RENDER", "R", List.of("FPS Display", "Coordinates", "Keystrokes")),
        new Category("BASE HUNT", "B", List.of("Base Hunting", "Storage Finder", "Portal Finder", "Spawner Finder")),
        new Category("MISC", "⋯", List.of())
    );

    private final Map<String, Rect> moduleRects = new LinkedHashMap<>();
    private String selectedModule;
    private float openProgress;

    public ClickGuiScreen() {
        super(Component.literal("Fizzy Client"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        openProgress = Math.min(1.0f, openProgress + 0.16f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x9908060D);

        int panelWidth = Math.min(1040, width - 36);
        int panelHeight = Math.min(590, height - 36);
        panelWidth = Math.max(520, panelWidth);
        panelHeight = Math.max(340, panelHeight);

        float eased = 1.0f - (float) Math.pow(1.0f - openProgress, 3);
        int drawWidth = (int) (panelWidth * (0.94f + 0.06f * eased));
        int drawHeight = (int) (panelHeight * (0.94f + 0.06f * eased));
        int left = (width - drawWidth) / 2;
        int top = (height - drawHeight) / 2;

        roundRect(g, left, top, drawWidth, drawHeight, 12, PANEL);
        roundRect(g, left + 1, top + 1, drawWidth - 2, 54, 11, 0xFF211E2B);
        g.fill(left + 1, top + 48, left + drawWidth - 1, top + 55, 0xFF211E2B);

        roundRect(g, left + 18, top + 13, 28, 28, 8, PURPLE_DARK);
        centered(g, "F", left + 18, top + 13, 28, 28, WHITE);
        g.drawString(font, "FIZZY", left + 56, top + 13, WHITE, false);
        g.drawString(font, "CLIENT", left + 56, top + 27, PURPLE, false);
        g.drawString(font, "INDEPENDENT CLIENT  •  1.21.11", left + 142, top + 21, MUTED, false);

        String status = "8 MODULES";
        int statusWidth = font.width(status) + 18;
        roundRect(g, left + drawWidth - statusWidth - 20, top + 17, statusWidth, 22, 7, 0xFF302A3D);
        g.drawString(font, status, left + drawWidth - statusWidth - 11, top + 24, PURPLE, false);

        int contentTop = top + 72;
        int contentBottom = top + drawHeight - 18;
        int gap = 10;
        int innerLeft = left + 18;
        int innerWidth = drawWidth - 36;
        int colWidth = (innerWidth - gap * 4) / 5;
        moduleRects.clear();

        for (int i = 0; i < CATEGORIES.size(); i++) {
            Category category = CATEGORIES.get(i);
            int x = innerLeft + i * (colWidth + gap);

            roundRect(g, x, contentTop, colWidth, 34, 7, 0xFF24212F);
            roundRect(g, x + 7, contentTop + 7, 20, 20, 5, 0xFF393047);
            centered(g, category.icon(), x + 7, contentTop + 7, 20, 20, PURPLE);
            g.drawString(font, category.name(), x + 33, contentTop + 12, WHITE, false);

            int moduleY = contentTop + 43;
            for (String module : category.modules()) {
                int cardHeight = 38;
                Rect rect = new Rect(x, moduleY, colWidth, cardHeight);
                moduleRects.put(module, rect);
                boolean hovered = contains(rect, mouseX, mouseY);
                boolean active = FizzyClient.MODULES.isEnabled(module);

                roundRect(g, x, moduleY, colWidth, cardHeight, 7, hovered ? CARD_HOVER : CARD);
                g.drawString(font, module, x + 8, moduleY + 8,
                    active ? WHITE : 0xFFD4D0DD, false);

                int switchX = x + colWidth - 29;
                int switchY = moduleY + 12;
                roundRect(g, switchX, switchY, 21, 13, 7, active ? PURPLE_DARK : 0xFF4A4655);
                roundRect(g, active ? switchX + 11 : switchX + 2, switchY + 2, 9, 9, 5,
                    active ? 0xFFFFFFFF : 0xFFB8B3C1);

                if (hovered) g.drawString(font, "Right-click: info", x + 8, moduleY + 25, MUTED, false);
                moduleY += cardHeight + 7;
            }

            if (category.modules().isEmpty()) {
                g.drawString(font, "Coming soon", x + 8, moduleY + 8, 0xFF777184, false);
            }
        }

        g.fill(left + 18, contentBottom - 22, left + drawWidth - 18, contentBottom - 21, 0xFF393443);
        g.drawString(font, "RIGHT SHIFT  •  CLOSE", left + 20, contentBottom - 13, MUTED, false);
        String footer = "FIZZY  /  BUILT FROM SCRATCH";
        g.drawString(font, footer, left + drawWidth - 20 - font.width(footer),
            contentBottom - 13, 0xFF777184, false);

        if (selectedModule != null) renderSettings(g, left, top, drawWidth, drawHeight);
    }

    private void renderSettings(GuiGraphics g, int left, int top, int panelWidth, int panelHeight) {
        int w = 310;
        int h = 170;
        int x = left + (panelWidth - w) / 2;
        int y = top + (panelHeight - h) / 2;
        roundRect(g, x - 5, y - 5, w + 10, h + 10, 12, 0xCC08070D);
        roundRect(g, x, y, w, h, 10, 0xFF24212F);
        g.drawString(font, "MODULE DETAILS", x + 16, y + 15, PURPLE, false);
        g.drawString(font, selectedModule, x + 16, y + 39, WHITE, false);
        String description = switch (selectedModule) {
            case "Toggle Sprint" -> "Automatically sprints while moving forward.";
            case "FPS Display" -> "Shows your current frames per second.";
            case "Coordinates" -> "Shows your current block coordinates.";
            case "Keystrokes" -> "Displays live WASD and jump key presses.";
            case "Base Hunting" -> "Scans loaded nearby blocks for storage, portals and spawners.";
            case "Storage Finder" -> "Lists nearby loaded chests, barrels and ender chests.";
            case "Portal Finder" -> "Lists nearby Nether portal blocks in loaded chunks.";
            case "Spawner Finder" -> "Lists nearby visible mob spawner blocks.";
            default -> "This module is not implemented yet.";
        };
        drawWrapped(g, description, x + 16, y + 66, w - 32, MUTED);
        String note = "Scan radius: 16 blocks  •  Refresh: 1 sec";
        g.drawString(font, note, x + 16, y + 126, 0xFFB8AFC7, false);
        roundRect(g, x + 16, y + 140, w - 32, 22, 7, 0xFF393047);
        centered(g, "CLICK ANYWHERE TO CLOSE", x + 16, y + 140, w - 32, 22, WHITE);
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
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (selectedModule != null) {
            selectedModule = null;
            return true;
        }

        for (Map.Entry<String, Rect> entry : moduleRects.entrySet()) {
            if (!contains(entry.getValue(), mouseX, mouseY)) continue;
            if (button == 1) {
                selectedModule = entry.getKey();
                return true;
            }
            if (button == 0) {
                FizzyClient.MODULES.toggle(entry.getKey());
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 || event.key() == 344) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static boolean contains(Rect rect, double x, double y) {
        return x >= rect.x() && x <= rect.x() + rect.width()
            && y >= rect.y() && y <= rect.y() + rect.height();
    }

    private static void centered(GuiGraphics g, String text, int x, int y, int w, int h, int color) {
        g.drawString(net.minecraft.client.Minecraft.getInstance().font, text,
            x + (w - net.minecraft.client.Minecraft.getInstance().font.width(text)) / 2,
            y + (h - 8) / 2, color, false);
    }

    private static void roundRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
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

    private record Category(String name, String icon, List<String> modules) {}
    private record Rect(int x, int y, int width, int height) {}
}
