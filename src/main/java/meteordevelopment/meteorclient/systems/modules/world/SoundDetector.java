/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class SoundDetector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Maximum distance for sound alerts. Minecraft only provides sounds the client can hear.")
        .defaultValue(64)
        .min(8)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> alertCooldown = sgGeneral.add(new DoubleSetting.Builder()
        .name("alert-cooldown")
        .description("Minimum time between sound alerts, in seconds.")
        .defaultValue(0.75)
        .min(0)
        .sliderMax(3)
        .build()
    );

    private final Setting<Boolean> storage = sgGeneral.add(new BoolSetting.Builder()
        .name("storage")
        .description("Detects chests, barrels and shulker boxes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> doors = sgGeneral.add(new BoolSetting.Builder()
        .name("doors")
        .description("Detects doors and trapdoors.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> footsteps = sgGeneral.add(new BoolSetting.Builder()
        .name("footsteps")
        .description("Detects player movement sounds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> blockActions = sgGeneral.add(new BoolSetting.Builder()
        .name("block-actions")
        .description("Detects block placement and breaking sounds.")
        .defaultValue(true)
        .build()
    );

    private long lastAlertTime;

    public SoundDetector() {
        super(Categories.BaseHunting, "sound-detector", "Alerts you to nearby storage, movement and block sounds.");
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (mc.player == null || mc.world == null) return;

        String id = event.sound.getId().getPath();
        String label = null;

        if (storage.get() && (id.contains("chest") || id.contains("barrel") || id.contains("shulker_box"))) {
            if (id.endsWith(".open") || id.endsWith(".close")) label = "Storage";
        }

        if (label == null && doors.get() && (id.contains("door") || id.contains("trapdoor"))
            && (id.endsWith(".open") || id.endsWith(".close"))) {
            label = "Door";
        }

        if (label == null && footsteps.get() && id.startsWith("entity.player.") && id.contains("step")) {
            label = "Footsteps";
        }

        if (label == null && blockActions.get()
            && (id.equals("block.stone.break") || id.equals("block.wood.break")
            || id.equals("block.gravel.break") || id.equals("block.stone.place")
            || id.equals("block.wood.place") || id.equals("block.gravel.place"))) {
            label = "Block activity";
        }

        if (label == null) return;

        Vec3d soundPos = new Vec3d(event.sound.getX(), event.sound.getY(), event.sound.getZ());
        double distance = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(soundPos);
        if (distance > range.get()) return;

        long now = java.lang.System.currentTimeMillis();
        if (now - lastAlertTime < alertCooldown.get() * 1000) return;
        lastAlertTime = now;

        double dx = soundPos.x - mc.player.getX();
        double dz = soundPos.z - mc.player.getZ();
        String[] directions = {"South", "South-West", "West", "North-West", "North", "North-East", "East", "South-East"};
        int direction = Math.floorMod((int) Math.round(Math.atan2(dx, dz) / (Math.PI / 4)), 8);

        info("%s detected %.0f blocks %s.", label, distance, directions[direction]);
    }
}
