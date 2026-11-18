package com.fizzy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class ModuleManager {
    private static final int PURPLE = 0xFFB18AFF;
    private static final int BACKGROUND = 0xAA171420;
    private final Map<String, Boolean> states = new LinkedHashMap<>();
    private final BaseHuntingScanner scanner = new BaseHuntingScanner();
    private final Map<String, BlockPos> waypoints = new LinkedHashMap<>();
    private int scanRange = 24;
    private boolean showDistances = true;

    public ModuleManager() {
        for (String name : new String[] {
            "Toggle Sprint", "Speed Display",
            "FPS Display", "Coordinates", "Keystrokes", "Armor HUD",
            "Totem Counter", "Durability Warning", "World Time",
            "Base Hunting", "Storage Finder", "Portal Finder", "Spawner Finder",
            "Block Finder", "Waypoints"
        }) states.put(name, false);
        load();
    }

    public void toggle(String name) {
        if (!states.containsKey(name)) return;
        states.put(name, !states.get(name));
        save();
    }

    public boolean isEnabled(String name) { return states.getOrDefault(name, false); }
    public Map<String, Boolean> getStates() { return Map.copyOf(states); }
    public int getScanRange() { return scanRange; }
    public boolean isShowDistances() { return showDistances; }

    public void adjustScanRange(int delta) {
        scanRange = Math.max(8, Math.min(64, scanRange + delta));
        save();
    }

    public void toggleDistances() {
        showDistances = !showDistances;
        save();
    }

    public void saveWaypoint(Minecraft client) {
        if (client.player == null) return;
        BlockPos pos = client.player.blockPosition();
        waypoints.put("Waypoint " + (waypoints.size() + 1), pos.immutable());
        save();
    }

    public Map<String, BlockPos> getWaypoints() { return Map.copyOf(waypoints); }

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (client.screen == null && isEnabled("Toggle Sprint")
            && client.options.keyUp.isDown() && !client.options.keyShift.isDown()) {
            client.player.setSprinting(true);
        }
        scanner.tick(client, scanRange,
            isEnabled("Base Hunting"), isEnabled("Storage Finder"),
            isEnabled("Portal Finder"), isEnabled("Spawner Finder"),
            isEnabled("Block Finder"));
    }

    public void renderHud(GuiGraphics g, Minecraft client) {
        if (client.player == null || client.options.hideGui || client.screen != null) return;
        int y = 10;
        if (isEnabled("FPS Display")) { drawLine(g, "FPS  " + client.getFps(), 10, y); y += 20; }
        if (isEnabled("Coordinates")) {
            drawLine(g, String.format("XYZ  %d  %d  %d",
                client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ()), 10, y);
            y += 20;
        }
        if (isEnabled("Speed Display")) {
            var velocity = client.player.getDeltaMovement();
            double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0;
            drawLine(g, String.format("SPEED  %.2f blocks/s", speed), 10, y); y += 20;
        }
        if (isEnabled("World Time") && client.level != null) {
            long time = Math.floorMod(client.level.getDayTime(), 24000L);
            long hours = (time / 1000 + 6) % 24;
            long minutes = (time % 1000) * 60 / 1000;
            drawLine(g, String.format("WORLD TIME  %02d:%02d", hours, minutes), 10, y); y += 20;
        }
        if (isEnabled("Keystrokes")) drawKeystrokes(g, client, 10, 82);
        if (isEnabled("Armor HUD")) {
            int armorY = 10;
            for (var stack : client.player.getArmorSlots()) {
                if (!stack.isEmpty()) {
                    String label = stack.getHoverName().getString() + "  " +
                        (stack.isDamageableItem() ? (stack.getMaxDamage() - stack.getDamageValue()) + " durability" : "no durability");
                    drawLine(g, label, g.guiWidth() - client.font.width(label) - 24, armorY);
                    armorY += 20;
                }
            }
        }
        if (isEnabled("Totem Counter")) {
            int count = 0;
            for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
                var stack = client.player.getInventory().getItem(i);
                if (stack.is(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
            }
            drawLine(g, "TOTEMS  " + count, 10, y); y += 20;
        }
        if (isEnabled("Durability Warning")) {
            int lowest = 101;
            String itemName = "";
            for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
                var stack = client.player.getInventory().getItem(i);
                if (stack.isDamageableItem()) {
                    int percent = (stack.getMaxDamage() - stack.getDamageValue()) * 100 / stack.getMaxDamage();
                    if (percent < lowest) { lowest = percent; itemName = stack.getHoverName().getString(); }
                }
            }
            if (lowest <= 20) drawLine(g, "LOW DURABILITY  " + itemName + " " + lowest + "%", 10, y);
        }
        scanner.renderHud(g, client, y,
            isEnabled("Base Hunting"), isEnabled("Storage Finder"),
            isEnabled("Portal Finder"), isEnabled("Spawner Finder"),
            isEnabled("Block Finder"), showDistances);
        if (isEnabled("Waypoints") && !waypoints.isEmpty()) {
            int wy = g.guiHeight() - 34;
            for (var entry : waypoints.entrySet()) {
                double distance = Math.sqrt(entry.getValue().distSqr(client.player.blockPosition()));
                String label = entry.getKey() + "  " + entry.getValue().getX() + " "
                    + entry.getValue().getY() + " " + entry.getValue().getZ()
                    + String.format("  (%.0fm)", distance);
                drawLine(g, label, 10, wy);
                wy -= 20;
            }
        }
    }

    private void drawKeystrokes(GuiGraphics g, Minecraft c, int x, int y) {
        drawKey(g, "W", x + 24, y, c.options.keyUp.isDown());
        drawKey(g, "A", x, y + 24, c.options.keyLeft.isDown());
        drawKey(g, "S", x + 24, y + 24, c.options.keyDown.isDown());
        drawKey(g, "D", x + 48, y + 24, c.options.keyRight.isDown());
        drawKey(g, "JUMP", x, y + 48, c.options.keyJump.isDown(), 72);
    }

    private void drawKey(GuiGraphics g, String text, int x, int y, boolean pressed) {
        drawKey(g, text, x, y, pressed, 22);
    }
    private void drawKey(GuiGraphics g, String text, int x, int y, boolean pressed, int width) {
        g.fill(x, y, x + width, y + 20, pressed ? 0xFF7046C8 : BACKGROUND);
        g.renderOutline(x, y, width, 20, pressed ? PURPLE : 0xFF625C70);
        g.drawString(Minecraft.getInstance().font, text,
            x + (width - Minecraft.getInstance().font.width(text)) / 2, y + 6, 0xFFFFFFFF, false);
    }
    private void drawLine(GuiGraphics g, String text, int x, int y) {
        var font = Minecraft.getInstance().font;
        int width = font.width(text) + 12;
        g.fill(x, y, x + width, y + 18, BACKGROUND);
        g.fill(x, y, x + 2, y + 18, PURPLE);
        g.drawString(font, text, x + 7, y + 5, 0xFFF4F1FA, false);
    }

    private void load() {
        Path file = settingsPath();
        if (!Files.exists(file)) return;
        Properties p = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            p.load(reader);
            states.replaceAll((name, old) -> Boolean.parseBoolean(p.getProperty("module." + name, "false")));
            scanRange = Math.max(8, Math.min(64, Integer.parseInt(p.getProperty("scanRange", "24"))));
            showDistances = Boolean.parseBoolean(p.getProperty("showDistances", "true"));
            int count = Integer.parseInt(p.getProperty("waypoint.count", "0"));
            for (int i = 0; i < count; i++) {
                String[] parts = p.getProperty("waypoint." + i, "").split(",");
                if (parts.length == 3) waypoints.put("Waypoint " + (i + 1),
                    new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            }
        } catch (Exception ignored) { }
    }

    private void save() {
        Properties p = new Properties();
        states.forEach((name, enabled) -> p.setProperty("module." + name, Boolean.toString(enabled)));
        p.setProperty("scanRange", Integer.toString(scanRange));
        p.setProperty("showDistances", Boolean.toString(showDistances));
        p.setProperty("waypoint.count", Integer.toString(waypoints.size()));
        int i = 0;
        for (BlockPos pos : waypoints.values())
            p.setProperty("waypoint." + i++, pos.getX() + "," + pos.getY() + "," + pos.getZ());
        try {
            Files.createDirectories(settingsPath().getParent());
            try (var writer = Files.newBufferedWriter(settingsPath())) { p.store(writer, "Fizzy Client settings"); }
        } catch (IOException ignored) { }
    }
    private static Path settingsPath() { return Path.of("config", "fizzy-client.properties"); }
}
