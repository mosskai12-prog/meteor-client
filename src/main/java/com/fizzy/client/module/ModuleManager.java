package com.fizzy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

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
    private final BaseHuntingScanner baseHunting = new BaseHuntingScanner();

    public ModuleManager() {
        states.put("Toggle Sprint", false);
        states.put("FPS Display", false);
        states.put("Coordinates", false);
        states.put("Keystrokes", false);
        states.put("Base Hunting", false);
        states.put("Storage Finder", false);
        states.put("Portal Finder", false);
        states.put("Spawner Finder", false);
        load();
    }

    public void toggle(String name) {
        if (!states.containsKey(name)) return;
        states.put(name, !states.get(name));
        save();
    }

    public boolean isEnabled(String name) {
        return states.getOrDefault(name, false);
    }

    public Map<String, Boolean> getStates() {
        return Map.copyOf(states);
    }

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (client.screen == null && isEnabled("Toggle Sprint")
            && client.options.keyUp.isDown()
            && !client.options.keyShift.isDown()) {
            client.player.setSprinting(true);
        }

        baseHunting.tick(client,
            isEnabled("Base Hunting"),
            isEnabled("Storage Finder"),
            isEnabled("Portal Finder"),
            isEnabled("Spawner Finder"));
    }

    public void renderHud(GuiGraphics graphics, Minecraft client) {
        if (client.player == null || client.options.hideGui || client.screen != null) return;

        int y = 10;
        if (isEnabled("FPS Display")) {
            drawLine(graphics, "FPS  " + client.getFps(), 10, y);
            y += 20;
        }

        if (isEnabled("Coordinates")) {
            String coords = String.format("XYZ  %d  %d  %d",
                client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ());
            drawLine(graphics, coords, 10, y);
            y += 20;
        }

        if (isEnabled("Keystrokes")) {
            drawKeystrokes(graphics, client, 10, 82);
        }

        baseHunting.renderHud(graphics, client, y,
            isEnabled("Base Hunting"),
            isEnabled("Storage Finder"),
            isEnabled("Portal Finder"),
            isEnabled("Spawner Finder"));
    }

    private void drawKeystrokes(GuiGraphics g, Minecraft client, int x, int y) {
        drawKey(g, "W", x + 24, y, client.options.keyUp.isDown());
        drawKey(g, "A", x, y + 24, client.options.keyLeft.isDown());
        drawKey(g, "S", x + 24, y + 24, client.options.keyDown.isDown());
        drawKey(g, "D", x + 48, y + 24, client.options.keyRight.isDown());
        drawKey(g, "JUMP", x, y + 48, client.options.keyJump.isDown(), 72);
    }

    private void drawKey(GuiGraphics g, String label, int x, int y, boolean pressed) {
        drawKey(g, label, x, y, pressed, 22);
    }

    private void drawKey(GuiGraphics g, String label, int x, int y, boolean pressed, int width) {
        int color = pressed ? 0xFF7046C8 : BACKGROUND;
        g.fill(x, y, x + width, y + 20, color);
        g.renderOutline(x, y, width, 20, pressed ? PURPLE : 0xFF625C70);
        var font = Minecraft.getInstance().font;
        g.drawString(font, label, x + (width - font.width(label)) / 2, y + 6, 0xFFFFFFFF, false);
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
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
            states.replaceAll((name, ignored) ->
                Boolean.parseBoolean(properties.getProperty(name, "false")));
        } catch (IOException ignored) {
            // Defaults are used when the config cannot be read.
        }
    }

    private void save() {
        Properties properties = new Properties();
        states.forEach((name, value) -> properties.setProperty(name, Boolean.toString(value)));
        Path file = settingsPath();
        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "Fizzy Client module settings");
            }
        } catch (IOException ignored) {
            // The client remains usable if settings cannot be written.
        }
    }

    private static Path settingsPath() {
        return Path.of("config", "fizzy-client.properties");
    }
}
