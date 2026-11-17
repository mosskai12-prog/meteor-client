/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.world.StashFinder;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Compact Fizzy status dashboard HUD. */
public class FizzyDashboardHud extends HudElement {
    public static final HudElementInfo<FizzyDashboardHud> INFO = new HudElementInfo<>(Hud.GROUP, "fizzy-dashboard", "Fizzy overview with hunting status and detected stash totals.", FizzyDashboardHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
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
        .defaultValue(new SettingColor(75, 220, 191)).build());

    private int stashCount;
    private int activeHunting;

    public FizzyDashboardHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        StashFinder finder = Modules.get().get(StashFinder.class);
        stashCount = finder == null ? 0 : finder.chunks.size();
        activeHunting = 0;
        for (Module module : Modules.get().getGroup(Categories.BaseHunting)) {
            if (module.isActive()) activeHunting++;
        }

        double scale = Hud.get().getTextScale();
        double width = renderer.textWidth("FIZZY CLIENT", true, scale);
        double height = renderer.textHeight(true, scale) * 4 + 22;
        if (showModules.get()) {
            for (Module module : Modules.get().getGroup(Categories.BaseHunting)) {
                if (module.isActive()) {
                    width = Math.max(width, renderer.textWidth("• " + module.title, true, scale));
                    height += renderer.textHeight(true, scale) + 2;
                }
            }
        }
        setSize(width + 20, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double scale = Hud.get().getTextScale();
        double x = this.x + 10;
        double y = this.y + 8;
        if (background.get()) renderer.quad(this.x, this.y, getWidth(), getHeight(), new SettingColor(12, 17, 23, 210));

        renderer.text("FIZZY CLIENT", x, y, accent.get(), true, scale);
        y += renderer.textHeight(true, scale) + 4;
        renderer.text(mc.player == null ? "STATUS  /  DISCONNECTED" : "STATUS  /  ONLINE", x, y, new SettingColor(220, 228, 235), true, scale);
        y += renderer.textHeight(true, scale) + 3;
        renderer.text("STASH LOCATIONS  /  " + stashCount, x, y, new SettingColor(220, 228, 235), true, scale);
        y += renderer.textHeight(true, scale) + 3;
        renderer.text("HUNTING MODULES  /  " + activeHunting, x, y, new SettingColor(220, 228, 235), true, scale);

        if (!showModules.get()) return;
        for (Module module : Modules.get().getGroup(Categories.BaseHunting)) {
            if (!module.isActive()) continue;
            y += renderer.textHeight(true, scale) + 2;
            renderer.text("• " + module.title, x, y, new SettingColor(145, 210, 195), true, scale);
        }
    }
}
