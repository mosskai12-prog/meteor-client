/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class WheatFarmer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Search radius for fully grown wheat. Harvesting is limited to normal block reach.")
        .defaultValue(4.5)
        .min(1)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder()
        .name("action-delay")
        .description("Ticks to wait between crop actions.")
        .defaultValue(4)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> replant = sgGeneral.add(new BoolSetting.Builder()
        .name("replant")
        .description("Plants wheat seeds after harvesting when seeds are available in your hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swings your hand when harvesting or planting.")
        .defaultValue(true)
        .build()
    );

    private int ticks;
    private boolean inventoryWasFull;

    public WheatFarmer() {
        super(Categories.World, "wheat-farmer", "Automatically harvests nearby mature wheat and replants it.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        inventoryWasFull = false;
    }

    @Override
    public void onDeactivate() {
        if (fullInventoryUseTicks > 0 && mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
        fullInventoryUseTicks = 0;
        inventoryWasFull = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (handleFullInventory()) return;

        if (++ticks < actionDelay.get()) return;
        ticks = 0;

        BlockPos origin = mc.player.getBlockPos();
        int r = (int) Math.ceil(range.get());

        for (BlockPos pos : BlockPos.iterate(
            origin.add(-r, -2, -r),
            origin.add(r, 2, r)
        )) {
            if (!mc.world.getBlockState(pos).isOf(Blocks.WHEAT)) continue;
            if (mc.world.getBlockState(pos).get(CropBlock.AGE) < CropBlock.MAX_AGE) continue;
            if (!PlayerUtils.isWithinReach(pos)) continue;

            // Turn toward the crop before sending the harvest action.
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> BlockUtils.breakBlock(pos, swingHand.get()));
            return;
        }

        if (!replant.get()) return;

        FindItemResult seeds = InvUtils.findInHotbar(Items.WHEAT_SEEDS);
        if (!seeds.found()) return;

        for (BlockPos pos : BlockPos.iterate(
            origin.add(-r, -2, -r),
            origin.add(r, 2, r)
        )) {
            if (!mc.world.getBlockState(pos).isAir()) continue;
            if (!mc.world.getBlockState(pos.down()).isOf(Blocks.FARMLAND)) continue;
            if (!PlayerUtils.isWithinReach(pos)) continue;

            BlockUtils.place(pos, seeds, true, 0, swingHand.get());
            return;
        }
    }

    private boolean handleFullInventory() {
        boolean full = true;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                full = false;
                break;
            }
        }

        if (!full) {
            inventoryWasFull = false;
            return false;
        }

        // Send /sell once as soon as the inventory becomes full.
        if (!inventoryWasFull) {
            inventoryWasFull = true;
            mc.getNetworkHandler().sendChatCommand("sell");
        }

        return true;
    }
}
