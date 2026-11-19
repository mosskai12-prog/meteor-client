package dev.fizzyclient.modules;

import dev.fizzyclient.core.module.FizzyModule;
import net.minecraft.client.MinecraftClient;

public final class SellModule extends FizzyModule {
    private int ticks;
    private boolean wasFull;

    public SellModule() {
        super("Auto Sell", "Sends /sell once when all 36 main inventory slots are occupied.");
    }

    @Override
    public void onEnable() {
        ticks = 0;
        wasFull = false;
    }

    @Override
    public void onDisable() {
        ticks = 0;
        wasFull = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (++ticks < 20) return;
        ticks = 0;

        boolean full = true;
        for (int slot = 0; slot < 36; slot++) {
            if (mc.player.getInventory().getStack(slot).isEmpty()) {
                full = false;
                break;
            }
        }

        if (!full) {
            wasFull = false;
            return;
        }

        if (!wasFull) {
            wasFull = true;
            mc.getNetworkHandler().sendChatCommand("sell");
        }
    }
}
