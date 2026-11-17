/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.world.StashFinder;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Compact Fizzy status dashboard HUD. */
public class FizzyDashboardHud extends HudElement {
    public static final HudElementInfo<FizzyDashboardHud> INFO = new HudElementInfo<>(Hud.GROUP, "fizzy-dashboard", "Fizzy overview with player, server, performance and base-hunting information.", FizzyDashboardHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> showServer = sgGeneral.add(new BoolSetting.Builder()
        .name("show-server")
        .description("Displays the connected server address.")
        .defaultValue(true).build());
    private final Setting<Boolean> showCoordinates = sgGeneral.add(new BoolSetting.Builder()
        .name("show-coordinates")
        .description("Displays your current block coordinates.")
        .defaultValue(true).build());
    private final Setting<Boolean> showPerformance = sgGeneral.add(new BoolSetting.Builder()
        .name("show-performance")
        .description("Displays FPS and server ping.")
        .defaultValue(true).build());
    private final Setting<Boolean> showModules = sgGeneral.add(new BoolSetting.Builder()
        .name("show-active-hunting-modules")
        .description("Lists enabled modules in the Base Hunting category.")
        .defaultValue(true).build());
    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a dark dashboard panel.")
        .defaultValue(true).build());
    private final Setting<SettingColor> accent = sgGeneral.add(new ColorSetting.Builder()
        .name("accent")
        .description("Dashboard accent color.")
        .defaultValue(new SettingColor(235, 75, 75)).build());

    private final List<String> detailLines = new ArrayList<>();
    private final List<String> activeModuleNames = new ArrayList<>();
    private int stashCount;
    private int activeHunting;

    public FizzyDashboardHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        detailLines.clear();
        activeModuleNames.clear();

        boolean online = mc.player != null && mc.world != null;
        detailLines.add(online ? "STATUS  /  ONLINE" : "STATUS  /  DISCONNECTED");

        if (online && showServer.get()) {
            String server = mc.getCurrentServerEntry() == null ? "Singleplayer" : mc.getCurrentServerEntry().address;
            detailLines.add("SERVER  /  " + server);
        }

        if (online && showCoordinates.get()) {
            var pos = mc.player.getBlockPos();
            detailLines.add("XYZ  /  " + pos.getX() + "  " + pos.getY() + "  " + pos.getZ());
        }

        if (online && showPerformance.get()) {
            int ping = -1;
            if (mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            detailLines.add("FPS  /  " + mc.getCurrentFps() + "    PING  /  " + (ping < 0 ? "--" : ping + " ms"));
        }

        StashFinder finder = Modules.get().get(StashFinder.class);
        stashCount = finder == null ? 0 : finder.chunks.size();
        detailLines.add("STASH LOCATIONS  /  " + stashCount);

        activeHunting = 0;
        for (Module module : Modules.get().getGroup(Categories.BaseHunting)) {
            if (module.isActive()) {
                activeHunting++;
                activeModuleNames.add(module.title);
            }
        }
        detailLines.add("HUNTING MODULES  /  " + activeHunting);

        double scale = Hud.get().getTextScale();
        double width = renderer.textWidth("FIZZY / DASHBOARD", true, scale);
        double lineHeight = renderer.textHeight(true, scale) + 3;
        double height = renderer.textHeight(true, scale) + 12;

        for (String line : detailLines) {
            width = Math.max(width, renderer.textWidth(line, true, scale));
            height += lineHeight;
        }

        if (showModules.get()) {
            for (String name : activeModuleNames) {
                String line = "• " + name;
                width = Math.max(width, renderer.textWidth(line, true, scale));
                height += lineHeight;
            }
        }

        setSize(width + 20, height + 4);
    }

    @Override
    public void render(HudRenderer renderer) {
        double scale = Hud.get().getTextScale();
        double x = this.x + 10;
        double y = this.y + 7;
        double lineHeight = renderer.textHeight(true, scale) + 3;

        if (background.get()) renderer.quad(this.x, this.y, getWidth(), getHeight(), new SettingColor(12, 15, 21, 220));

        renderer.text("FIZZY / DASHBOARD", x, y, accent.get(), true, scale);
        y += lineHeight + 2;

        for (int i = 0; i < detailLines.size(); i++) {
            SettingColor color = i == 0 && mc.player != null
                ? new SettingColor(120, 225, 155)
                : new SettingColor(220, 226, 235);
            renderer.text(detailLines.get(i), x, y, color, true, scale);
            y += lineHeight;
        }

        if (showModules.get()) {
            for (String name : activeModuleNames) {
                renderer.text("• " + name, x, y, new SettingColor(245, 125, 125), true, scale);
                y += lineHeight;
            }
        }
    }
}
