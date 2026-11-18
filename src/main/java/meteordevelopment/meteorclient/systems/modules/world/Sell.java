/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

/**
 * Automatically sends /sell once when all 36 main inventory slots are occupied.
 */
public class Sell extends Module {
    private boolean inventoryWasFull;
    private int checkTicks;

    public Sell() {
        super(Categories.World, "sell", "Automatically sends /sell when your inventory is full.");
    }

    @Override
    public void onActivate() {
        inventoryWasFull = false;
        checkTicks = 0;
    }

    @Override
    public void onDeactivate() {
        inventoryWasFull = false;
        checkTicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        // Inventory state only needs checking periodically; avoid doing it every tick.
        if (++checkTicks < 20) return;
        checkTicks = 0;

        boolean full = true;
        for (int slot = 0; slot < 36; slot++) {
            if (mc.player.getInventory().getStack(slot).isEmpty()) {
                full = false;
                break;
            }
        }

        if (!full) {
            inventoryWasFull = false;
            return;
        }

        if (!inventoryWasFull) {
            inventoryWasFull = true;
            mc.getNetworkHandler().sendChatCommand("sell");
        }
    }
}
