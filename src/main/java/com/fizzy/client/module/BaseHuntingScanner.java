package com.fizzy.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BaseHuntingScanner {
    private static final int RANGE = 16;
    private static final int VERTICAL_RANGE = 8;
    private static final int MAX_RESULTS = 6;
    private static final int PURPLE = 0xFFB18AFF;
    private final Map<BlockPos, String> found = new LinkedHashMap<>();
    private int ticksUntilScan;

    void tick(Minecraft client, boolean master, boolean storage, boolean portals, boolean spawners) {
        if (!(master || storage || portals || spawners) || client.player == null || client.level == null) {
            found.clear();
            return;
        }

        if (ticksUntilScan-- > 0) return;
        ticksUntilScan = 20;
        found.clear();

        BlockPos origin = client.player.blockPosition();
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int z = -RANGE; z <= RANGE; z++) {
                for (int y = -VERTICAL_RANGE; y <= VERTICAL_RANGE; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!client.level.hasChunkAt(pos)) continue;

                    var state = client.level.getBlockState(pos);
                    String type = null;
                    if ((master || storage) && (state.is(Blocks.CHEST)
                        || state.is(Blocks.TRAPPED_CHEST)
                        || state.is(Blocks.ENDER_CHEST)
                        || state.is(Blocks.BARREL))) {
                        type = "Storage";
                    } else if ((master || portals) && state.is(Blocks.NETHER_PORTAL)) {
                        type = "Portal";
                    } else if ((master || spawners) && state.is(Blocks.SPAWNER)) {
                        type = "Spawner";
                    }

                    if (type != null) found.put(pos.immutable(), type);
                }
            }
        }
    }

    void renderHud(GuiGraphics graphics, Minecraft client, int startY,
                   boolean master, boolean storage, boolean portals, boolean spawners) {
        if (!(master || storage || portals || spawners) || client.player == null || found.isEmpty()) return;

        BlockPos playerPos = client.player.blockPosition();
        List<Map.Entry<BlockPos, String>> nearest = new ArrayList<>(found.entrySet());
        nearest.sort(Comparator.comparingDouble(entry ->
            entry.getKey().distSqr(playerPos)));

        var font = client.font;
        int y = startY;
        int shown = 0;
        for (var entry : nearest) {
            if (shown++ >= MAX_RESULTS) break;
            BlockPos pos = entry.getKey();
            int distance = (int) Math.sqrt(pos.distSqr(playerPos));
            String label = entry.getValue() + "  " + pos.getX() + " " + pos.getY() + " "
                + pos.getZ() + "  (" + distance + "m)";
            int width = font.width(label) + 12;
            graphics.fill(10, y, 10 + width, y + 18, 0xAA171420);
            graphics.fill(10, y, 12, y + 18, PURPLE);
            graphics.drawString(font, label, 17, y + 5, 0xFFF4F1FA, false);
            y += 20;
        }
    }
}
