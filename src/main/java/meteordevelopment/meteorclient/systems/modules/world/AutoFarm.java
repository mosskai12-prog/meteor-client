/*
 * Fizzy Client - automatic crop farming.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
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
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class AutoFarm extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scanRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("scan-range")
        .description("How far away to search for mature crops.")
        .defaultValue(24)
        .min(4)
        .sliderMax(48)
        .build()
    );

    private final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder()
        .name("action-delay")
        .description("Ticks to wait between farming actions.")
        .defaultValue(4)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> replant = sgGeneral.add(new BoolSetting.Builder()
        .name("replant")
        .description("Replants harvested crops when the matching seed or crop is in your hotbar.")
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
    private BlockPos pathTarget;
    private BlockPos replantTarget;
    private Block replantCrop;
    private boolean warnedNoPathing;

    public AutoFarm() {
        super(Categories.World, "auto-farm", "Finds mature crops, walks to them, selects a suitable tool, harvests and replants.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        pathTarget = null;
        replantTarget = null;
        replantCrop = null;
        warnedNoPathing = false;
    }

    @Override
    public void onDeactivate() {
        if (pathTarget != null && BaritoneUtils.IS_AVAILABLE) PathManagers.get().stop();
        pathTarget = null;
        replantTarget = null;
        replantCrop = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        // Finish a replant after the server has acknowledged the harvested block.
        tryReplant();

        if (++ticks < actionDelay.get()) return;
        ticks = 0;

        BlockPos crop = findNearestMatureCrop();
        if (crop == null) {
            stopPathing();
            return;
        }

        if (!PlayerUtils.isWithinReach(crop)) {
            walkTo(crop);
            return;
        }

        stopPathing();
        BlockState state = mc.world.getBlockState(crop);
        Block block = state.getBlock();
        selectBestTool(state);

        BlockPos target = crop.toImmutable();
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> {
            if (mc.world == null || !isMatureCrop(mc.world.getBlockState(target))) return;
            BlockUtils.breakBlock(target, swingHand.get());
            if (replant.get()) {
                replantTarget = target;
                replantCrop = block;
            }
        });
    }

    private BlockPos findNearestMatureCrop() {
        BlockPos origin = mc.player.getBlockPos();
        int radius = (int) Math.ceil(scanRange.get());
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(
            origin.add(-radius, -radius, -radius),
            origin.add(radius, radius, radius)
        )) {
            BlockState state = mc.world.getBlockState(pos);
            if (!isMatureCrop(state)) continue;

            double distance = origin.getSquaredDistance(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.toImmutable();
            }
        }

        return best;
    }

    private boolean isMatureCrop(BlockState state) {
        if (!(state.getBlock() instanceof CropBlock)) return false;

        int maxAge = state.isOf(Blocks.BEETROOTS) ? 3 : 7;
        return state.get(CropBlock.AGE) >= maxAge;
    }

    private void walkTo(BlockPos crop) {
        if (!BaritoneUtils.IS_AVAILABLE) {
            if (!warnedNoPathing) {
                warning("Install/enable Baritone to walk to crops. Nearby crops can still be farmed.");
                warnedNoPathing = true;
            }
            return;
        }

        if (pathTarget == null || !pathTarget.equals(crop) || !PathManagers.get().isPathing()) {
            pathTarget = crop.toImmutable();
            PathManagers.get().moveTo(pathTarget);
        }
    }

    private void stopPathing() {
        if (pathTarget != null && BaritoneUtils.IS_AVAILABLE) PathManagers.get().stop();
        pathTarget = null;
    }

    private void selectBestTool(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;

        // Search all 36 main-inventory slots, not just the hotbar.
        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            var stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;

            float speed = stack.getMiningSpeedMultiplier(state);
            if (stack.isSuitableFor(state)) speed += 0.01f;

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }

        if (bestSlot == -1) return;

        if (bestSlot < 9) {
            InvUtils.swap(bestSlot, false);
        } else {
            // Put the selected tool into the currently selected hotbar slot.
            InvUtils.move().from(bestSlot).toHotbar(mc.player.getInventory().getSelectedSlot());
        }
    }

    private void tryReplant() {
        if (!replant.get() || replantTarget == null || replantCrop == null) return;
        if (!mc.world.getBlockState(replantTarget).isAir()) return;
        if (!mc.world.getBlockState(replantTarget.down()).isOf(Blocks.FARMLAND)) {
            replantTarget = null;
            replantCrop = null;
            return;
        }

        Item seed = seedFor(replantCrop);
        if (seed == null) {
            replantTarget = null;
            replantCrop = null;
            return;
        }

        FindItemResult seeds = InvUtils.findInHotbar(seed);
        if (!seeds.found()) return;

        BlockUtils.place(replantTarget, seeds, true, 0, swingHand.get());
        replantTarget = null;
        replantCrop = null;
    }

    private Item seedFor(Block crop) {
        if (crop == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (crop == Blocks.CARROTS) return Items.CARROT;
        if (crop == Blocks.POTATOES) return Items.POTATO;
        if (crop == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        return null;
    }
}
