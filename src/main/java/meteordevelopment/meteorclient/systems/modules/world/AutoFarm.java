/*
 * Fizzy Client - automatic crop farming.
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
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BeetrootsBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;

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
    private BlockPos replantTarget;
    private Block replantCrop;
    private Iterator<BlockPos> scanIterator;
    private BlockPos scanBest;
    private BlockPos cachedCrop;
    private double scanBestDistance;
    private int scanCooldown;

    public AutoFarm() {
        super(Categories.World, "auto-farm", "Finds mature crops, walks to them, selects a suitable tool, harvests and replants.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        replantTarget = null;
        replantCrop = null;
        scanIterator = null;
        scanBest = null;
        cachedCrop = null;
        scanCooldown = 0;
    }

    @Override
    public void onDeactivate() {
        stopWalking();
        replantTarget = null;
        replantCrop = null;
        scanIterator = null;
        cachedCrop = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        // Finish a replant after the server has acknowledged the harvested block.
        tryReplant();

        // Spread crop scanning across ticks instead of checking tens of thousands
        // of blocks in one frame. This prevents large, repeated client-thread spikes.
        if (scanIterator == null && scanCooldown-- <= 0) beginScan();
        if (scanIterator != null) scanBlocks(2048);

        if (scanIterator != null) return;
        if (++ticks < actionDelay.get()) return;
        ticks = 0;

        BlockPos crop = cachedCrop;
        if (crop == null || !isMatureCrop(mc.world.getBlockState(crop))) {
            cachedCrop = null;
            stopWalking();
            return;
        }

        if (!PlayerUtils.isWithinReach(crop)) {
            walkTo(crop);
            return;
        }

        stopWalking();
        BlockState state = mc.world.getBlockState(crop);
        Block block = state.getBlock();
        selectBestTool(state);

        BlockPos target = crop.toImmutable();
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () -> {
            if (mc.world == null || !isMatureCrop(mc.world.getBlockState(target))) return;
            BlockUtils.breakBlock(target, swingHand.get());
            cachedCrop = null;
            if (replant.get()) {
                replantTarget = target;
                replantCrop = block;
            }
        });
    }

    private void beginScan() {
        BlockPos origin = mc.player.getBlockPos();
        int radius = (int) Math.ceil(scanRange.get());
        // Crops are normally on farmland near the player's level. Limit vertical
        // scanning to four blocks above and below instead of scanning a full cube.
        scanIterator = BlockPos.iterate(
            origin.add(-radius, -4, -radius),
            origin.add(radius, 4, radius)
        ).iterator();
        scanBest = null;
        scanBestDistance = Double.MAX_VALUE;
        scanCooldown = 5;
    }

    private void scanBlocks(int budget) {
        BlockPos origin = mc.player.getBlockPos();
        int checked = 0;
        while (scanIterator.hasNext() && checked++ < budget) {
            BlockPos pos = scanIterator.next();
            BlockState state = mc.world.getBlockState(pos);
            if (!isMatureCrop(state)) continue;

            double distance = origin.getSquaredDistance(pos);
            if (distance < scanBestDistance) {
                scanBestDistance = distance;
                scanBest = pos.toImmutable();
            }
        }

        if (!scanIterator.hasNext()) {
            cachedCrop = scanBest;
            scanIterator = null;
            ticks = actionDelay.get();
        }
    }

    private boolean isMatureCrop(BlockState state) {
        // Beetroots are CropBlocks too, but they use their own 0-3 AGE property.
        // CropBlock.AGE is the 0-7 property and is not present on beetroot states.
        if (state.isOf(Blocks.BEETROOTS)) {
            return state.get(BeetrootsBlock.AGE) >= BeetrootsBlock.BEETROOTS_MAX_AGE;
        }

        // Nether wart uses its own 0-3 age property, not CropBlock.AGE.
        if (state.isOf(Blocks.NETHER_WART)) {
            return state.get(NetherWartBlock.AGE) >= 3;
        }

        if (!(state.getBlock() instanceof CropBlock)) return false;
        return state.get(CropBlock.AGE) >= CropBlock.MAX_AGE;
    }

    // Walk toward the target using ordinary client movement keys. This does not
    // require Baritone and deliberately leaves the camera facing where the player
    // was looking, so farming does not cause sudden head turns.
    private void walkTo(BlockPos crop) {
        double dx = crop.getX() + 0.5 - mc.player.getX();
        double dz = crop.getZ() + 0.5 - mc.player.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length < 0.05) {
            stopWalking();
            return;
        }

        dx /= length;
        dz /= length;

        double yaw = Math.toRadians(mc.player.getYaw());
        double forward = -dx * Math.sin(yaw) + dz * Math.cos(yaw);
        double sideways = dx * Math.cos(yaw) + dz * Math.sin(yaw);

        mc.options.forwardKey.setPressed(forward > 0.25);
        mc.options.backKey.setPressed(forward < -0.25);
        mc.options.leftKey.setPressed(sideways < -0.25);
        mc.options.rightKey.setPressed(sideways > 0.25);
    }

    private void stopWalking() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
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
        Block support = mc.world.getBlockState(replantTarget.down()).getBlock();
        boolean validSupport = replantCrop == Blocks.NETHER_WART
            ? support == Blocks.SOUL_SAND
            : support == Blocks.FARMLAND;
        if (!validSupport) {
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
        if (crop == Blocks.NETHER_WART) return Items.NETHER_WART;
        return null;
    }
}
