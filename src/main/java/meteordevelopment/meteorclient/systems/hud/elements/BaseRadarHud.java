/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.StashFinder;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Fizzy's stash radar. It displays stash chunks already discovered by StashFinder;
 * it does not claim to scan chunks that the client has not received.
 */
public class BaseRadarHud extends HudElement {
    public static final HudElementInfo<BaseRadarHud> INFO = new HudElementInfo<>(Hud.GROUP, "fizzy-base-radar", "Shows the nearest stash locations recorded by Stash Finder.", BaseRadarHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
        .name("entries")
        .description("Maximum number of nearby stash locations to display.")
        .defaultValue(5).range(1, 10).sliderRange(1, 10).build());
    private final Setting<Boolean> showCounts = sgGeneral.add(new BoolSetting.Builder()
        .name("show-storage-count")
        .description("Displays the number of storage blocks recorded at each location.")
        .defaultValue(true).build());
    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a dark panel behind the radar.")
        .defaultValue(true).build());
    private final Setting<SettingColor> accent = sgGeneral.add(new ColorSetting.Builder()
        .name("accent")
        .description("Radar heading and distance color.")
        .defaultValue(new SettingColor(75, 220, 191)).build());

    private final List<StashFinder.Chunk> nearby = new ArrayList<>();

    public BaseRadarHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        nearby.clear();
        StashFinder finder = Modules.get().get(StashFinder.class);
        if (finder != null && mc.player != null) {
            nearby.addAll(finder.chunks);
            nearby.sort(Comparator.comparingDouble(c -> mc.player.squaredDistanceTo(c.x, mc.player.getY(), c.z)));
        }

        double scale = Hud.get().getTextScale();
        double width = renderer.textWidth("FIZZY / BASE RADAR", true, scale);
        double height = renderer.textHeight(true, scale) + 4;
        int count = Math.min(limit.get(), nearby.size());
        for (int i = 0; i < count; i++) {
            StashFinder.Chunk chunk = nearby.get(i);
            String line = chunk.x + ", " + chunk.z + "  " + Math.round(Math.sqrt(mc.player.squaredDistanceTo(chunk.x, mc.player.getY(), chunk.z))) + "m";
            if (showCounts.get()) line += "  [" + chunk.getTotal() + "]";
            width = Math.max(width, renderer.textWidth(line, true, scale));
            height += renderer.textHeight(true, scale) + 3;
        }
        if (count == 0) height += renderer.textHeight(true, scale) + 3;
        setSize(width + 16, height + 12);
    }

    @Override
    public void render(HudRenderer renderer) {
        double scale = Hud.get().getTextScale();
        double x = this.x + 8;
        double y = this.y + 6;
        if (background.get()) renderer.quad(this.x, this.y, getWidth(), getHeight(), new SettingColor(12, 17, 23, 205));

        renderer.text("FIZZY / BASE RADAR", x, y, accent.get(), true, scale);
        y += renderer.textHeight(true, scale) + 4;

        if (mc.player == null || nearby.isEmpty()) {
            renderer.text("No recorded stashes nearby", x, y, new SettingColor(170, 180, 190), true, scale);
            return;
        }

        int count = Math.min(limit.get(), nearby.size());
        for (int i = 0; i < count; i++) {
            StashFinder.Chunk chunk = nearby.get(i);
            long distance = Math.round(Math.sqrt(mc.player.squaredDistanceTo(chunk.x, mc.player.getY(), chunk.z)));
            String line = (i + 1) + ". " + chunk.x + ", " + chunk.z + "  " + distance + "m";
            if (showCounts.get()) line += "  [" + chunk.getTotal() + "]";
            renderer.text(line, x, y, new SettingColor(225, 232, 238), true, scale);
            y += renderer.textHeight(true, scale) + 3;
        }
    }
}
