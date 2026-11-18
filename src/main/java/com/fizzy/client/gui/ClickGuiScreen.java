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

public final class ClickGuiScreen extends Screen {
    private static final int PURPLE = 0xFF36BFFF, PURPLE_DARK = 0xFF087DCE;
    private static final int PANEL = 0xF00A1424, CARD = 0xFF12243A, CARD_HOVER = 0xFF193451;
    private static final int MUTED = 0xFF8FAAC7, WHITE = 0xFFF4F9FF;
    private static final List<Category> CATEGORIES = List.of(
        new Category("COMBAT", "C", List.of()),
        new Category("MOVEMENT", "M", List.of("Toggle Sprint", "Speed Display")),
        new Category("RENDER", "R", List.of("FPS Display", "Coordinates", "Keystrokes", "Armor HUD", "Totem Counter", "Durability Warning")),
        new Category("WORLD", "W", List.of("World Time", "Base Hunting", "Storage Finder", "Portal Finder", "Spawner Finder", "Block Finder", "Waypoints")),
        new Category("MISC", "⋯", List.of())
    );
    private final Map<String, Rect> moduleRects = new LinkedHashMap<>();
    private String selectedModule;
    private float openProgress;

    public ClickGuiScreen() { super(Component.literal("Fizzy Client")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() { openProgress = Math.min(1.0f, openProgress + 0.16f); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x55030A18);
        int panelWidth = Math.min(1120, width - 36), panelHeight = Math.min(620, height - 36);
        panelWidth = Math.max(520, panelWidth); panelHeight = Math.max(340, panelHeight);
        float eased = 1.0f - (float)Math.pow(1.0f - openProgress, 3);
        int dw = (int)(panelWidth * (0.94f + 0.06f * eased));
        int dh = (int)(panelHeight * (0.94f + 0.06f * eased));
        int left = (width - dw) / 2, top = (height - dh) / 2;
        roundRect(g, left, top, dw, dh, 12, PANEL);
        roundRect(g, left + 1, top + 1, dw - 2, 54, 11, 0xFF0D1C30);
        g.fill(left + 1, top + 48, left + dw - 1, top + 55, 0xFF0D1C30);
        roundRect(g, left + 18, top + 13, 28, 28, 8, PURPLE_DARK);
        centered(g, "F", left + 18, top + 13, 28, 28, WHITE);
        g.drawString(font, "FIZZY", left + 56, top + 13, WHITE, false);
        g.drawString(font, "CLIENT", left + 56, top + 27, PURPLE, false);
        g.drawString(font, "INDEPENDENT CLIENT  •  1.21.11", left + 142, top + 21, MUTED, false);
        String status = FizzyClient.MODULES.getStates().size() + " MODULES";
        int sw = font.width(status) + 18;
        roundRect(g, left + dw - sw - 20, top + 17, sw, 22, 7, 0xFF102E4C);
        g.drawString(font, status, left + dw - sw - 11, top + 24, PURPLE, false);

        int contentTop = top + 72, contentBottom = top + dh - 18;
        int gap = 10, innerLeft = left + 18, innerWidth = dw - 36;
        int colWidth = (innerWidth - gap * 4) / 5;
        moduleRects.clear();
        for (int i = 0; i < CATEGORIES.size(); i++) {
            Category category = CATEGORIES.get(i);
            int x = innerLeft + i * (colWidth + gap);
            roundRect(g, x, contentTop, colWidth, 34, 7, 0xFF10243B);
            roundRect(g, x + 7, contentTop + 7, 20, 20, 5, 0xFF123B63);
            centered(g, category.icon, x + 7, contentTop + 7, 20, 20, PURPLE);
            g.drawString(font, category.name, x + 33, contentTop + 12, WHITE, false);
            int moduleY = contentTop + 43;
            for (String module : category.modules) {
                int cardHeight = 34;
                Rect rect = new Rect(x, moduleY, colWidth, cardHeight);
                moduleRects.put(module, rect);
                boolean hovered = contains(rect, mouseX, mouseY), active = FizzyClient.MODULES.isEnabled(module);
                roundRect(g, x, moduleY, colWidth, cardHeight, 6, hovered ? CARD_HOVER : CARD);
                g.drawString(font, module, x + 8, moduleY + 7, active ? WHITE : 0xFFD4D0DD, false);
                int sx = x + colWidth - 29, sy = moduleY + 10;
                roundRect(g, sx, sy, 21, 13, 7, active ? PURPLE_DARK : 0xFF3A526D);
                roundRect(g, active ? sx + 11 : sx + 2, sy + 2, 9, 9, 5, 0xFFFFFFFF);
                moduleY += cardHeight + 5;
            }
            if (category.modules.isEmpty()) g.drawString(font, "No modules", x + 8, moduleY + 8, 0xFF777184, false);
        }
        g.fill(left + 18, contentBottom - 22, left + dw - 18, contentBottom - 21, 0xFF1B456A);
        g.drawString(font, "LEFT CLICK  •  TOGGLE       RIGHT CLICK  •  SETTINGS       RIGHT SHIFT  •  CLOSE",
            left + 20, contentBottom - 13, MUTED, false);
        if (selectedModule != null) renderSettings(g, left, top, dw, dh);
    }

    private void renderSettings(GuiGraphics g, int left, int top, int panelWidth, int panelHeight) {
        int w = 340, h = 210, x = left + (panelWidth - w) / 2, y = top + (panelHeight - h) / 2;
        roundRect(g, x - 5, y - 5, w + 10, h + 10, 12, 0xCC06111F);
        roundRect(g, x, y, w, h, 10, 0xFF10243B);
        g.drawString(font, "MODULE SETTINGS", x + 16, y + 15, PURPLE, false);
        g.drawString(font, selectedModule, x + 16, y + 38, WHITE, false);
        String description = switch (selectedModule) {
            case "Toggle Sprint" -> "Sprint while moving forward, unless sneaking.";
            case "Speed Display" -> "Show horizontal movement speed in blocks per second.";
            case "FPS Display" -> "Show your current frames per second.";
            case "Coordinates" -> "Show your current block position.";
            case "Keystrokes" -> "Show live movement and jump key presses.";
            case "Armor HUD" -> "Show equipped armor and remaining durability.";
            case "Totem Counter" -> "Count Totems of Undying in your inventory.";
            case "Durability Warning" -> "Warn when an inventory item falls below 20% durability.";
            case "World Time" -> "Show the in-game world clock.";
            case "Base Hunting" -> "Scan nearby loaded chunks for selected base-related blocks.";
            case "Storage Finder" -> "Find loaded chests, ender chests and barrels nearby.";
            case "Portal Finder" -> "Find loaded Nether portal blocks nearby.";
            case "Spawner Finder" -> "Find loaded mob spawners nearby.";
            case "Block Finder" -> "Find selected utility and building blocks nearby.";
            case "Waypoints" -> "Save your current position and show saved coordinates.";
            default -> "Module settings.";
        };
        drawWrapped(g, description, x + 16, y + 61, w - 32, MUTED);
        if (selectedModule.equals("Base Hunting") || selectedModule.equals("Storage Finder")
            || selectedModule.equals("Portal Finder") || selectedModule.equals("Spawner Finder")
            || selectedModule.equals("Block Finder")) {
            g.drawString(font, "Scan radius: " + FizzyClient.MODULES.getScanRange() + " blocks", x + 16, y + 111, WHITE, false);
            settingButton(g, x + 16, y + 128, 58, "-8");
            settingButton(g, x + 80, y + 128, 58, "-1");
            settingButton(g, x + 144, y + 128, 58, "+1");
            settingButton(g, x + 208, y + 128, 58, "+8");
            g.drawString(font, "Range is limited to loaded client chunks.", x + 16, y + 160, MUTED, false);
        } else if (selectedModule.equals("Waypoints")) {
            settingButton(g, x + 16, y + 124, w - 32, "SAVE CURRENT POSITION");
            g.drawString(font, "Saved: " + FizzyClient.MODULES.getWaypoints().size(), x + 16, y + 158, MUTED, false);
        } else {
            g.drawString(font, "No extra settings for this module yet.", x + 16, y + 122, MUTED, false);
        }
        settingButton(g, x + 16, y + h - 34, w - 32, "CLOSE SETTINGS");
    }

    private void settingButton(GuiGraphics g, int x, int y, int w, String label) {
        roundRect(g, x, y, w, 22, 6, 0xFF17466E);
        centered(g, label, x, y, w, 22, WHITE);
    }
    private void drawWrapped(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        StringBuilder line = new StringBuilder(); int lineY = y;
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(font, line.toString(), x, lineY, color, false); lineY += 12; line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' '); line.append(word);
        }
        if (!line.isEmpty()) g.drawString(font, line.toString(), x, lineY, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y(); int button = click.button();
        if (selectedModule != null) {
            int w = 340, h = 210, x = (width - w) / 2, y = (height - h) / 2;
            if (selectedModule.equals("Base Hunting") || selectedModule.equals("Storage Finder")
                || selectedModule.equals("Portal Finder") || selectedModule.equals("Spawner Finder")
                || selectedModule.equals("Block Finder")) {
                if (inside(mx,my,x+16,y+128,58,22)) FizzyClient.MODULES.adjustScanRange(-8);
                else if (inside(mx,my,x+80,y+128,58,22)) FizzyClient.MODULES.adjustScanRange(-1);
                else if (inside(mx,my,x+144,y+128,58,22)) FizzyClient.MODULES.adjustScanRange(1);
                else if (inside(mx,my,x+208,y+128,58,22)) FizzyClient.MODULES.adjustScanRange(8);
            } else if (selectedModule.equals("Waypoints") && inside(mx,my,x+16,y+124,w-32,22)) {
                FizzyClient.MODULES.saveWaypoint(Minecraft.getInstance());
            }
            selectedModule = null;
            return true;
        }
        for (var entry : moduleRects.entrySet()) {
            Rect r = entry.getValue();
            if (!contains(r, mx, my)) continue;
            if (button == 1) { selectedModule = entry.getKey(); return true; }
            if (button == 0) { FizzyClient.MODULES.toggle(entry.getKey()); return true; }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 || event.key() == 344) { onClose(); return true; }
        return super.keyPressed(event);
    }
    private static boolean inside(double mx,double my,int x,int y,int w,int h) {
        return mx >= x && mx <= x+w && my >= y && my <= y+h;
    }
    private static boolean contains(Rect r,double x,double y) { return inside(x,y,r.x,r.y,r.w,r.h); }
    private static void centered(GuiGraphics g,String s,int x,int y,int w,int h,int color) {
        var f = Minecraft.getInstance().font;
        g.drawString(f,s,x+(w-f.width(s))/2,y+(h-8)/2,color,false);
    }
    private static void roundRect(GuiGraphics g,int x,int y,int w,int h,int radius,int color) {
        int r=Math.min(radius,Math.min(w/2,h/2));
        for(int row=0;row<h;row++){
            int inset=0;
            if(row<r){double dy=r-row-0.5;inset=r-(int)Math.floor(Math.sqrt(Math.max(0,r*r-dy*dy)));}
            else if(row>=h-r){double dy=row-(h-r)+0.5;inset=r-(int)Math.floor(Math.sqrt(Math.max(0,r*r-dy*dy)));}
            g.fill(x+inset,y+row,x+w-inset,y+row+1,color);
        }
    }
    private record Category(String name,String icon,List<String> modules) {}
    private record Rect(int x,int y,int w,int h) {}
}
