/*
 * Fizzy Client fishing reward and efficiency tracker.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.FishingBobberEntityAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;

/**
 * Tracks confirmed fishing bites while Auto Fish is running.
 * This does not alter server-side loot chances or reward packets.
 */
public class FishingOptimizer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder()
        .name("catch-notifications")
        .description("Print a short update after each batch of confirmed catches.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> reportEvery = sgGeneral.add(new IntSetting.Builder()
        .name("report-every")
        .description("How many confirmed catches between reports.")
        .defaultValue(10)
        .min(1)
        .sliderMax(100)
        .build()
    );

    private FishingBobberEntity trackedBobber;
    private boolean countedCurrentCatch;
    private int catches;
    private long startedAt;

    public FishingOptimizer() {
        super(Categories.Player, "fishing-optimizer", "Tracks fishing catches and measures your real catch rate. Does not change server loot odds.");
    }

    @Override
    public void onActivate() {
        trackedBobber = null;
        countedCurrentCatch = false;
        catches = 0;
        startedAt = System.currentTimeMillis();
        info("Fishing tracker started. Keep Auto Fish enabled to measure your catches.");
    }

    @Override
    public void onDeactivate() {
        if (startedAt != 0 && catches > 0) report("Session finished");
        trackedBobber = null;
        countedCurrentCatch = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        FishingBobberEntity bobber = mc.player.fishHook;
        if (bobber == null) {
            trackedBobber = null;
            countedCurrentCatch = false;
            return;
        }

        if (bobber != trackedBobber) {
            trackedBobber = bobber;
            countedCurrentCatch = false;
        }

        if (!countedCurrentCatch && ((FishingBobberEntityAccessor) bobber).meteor$hasCaughtFish()) {
            countedCurrentCatch = true;
            catches++;

            if (notifications.get() && catches % reportEvery.get() == 0) {
                report("Fishing update");
            }
        }
    }

    private void report(String prefix) {
        long elapsedMillis = Math.max(1000L, System.currentTimeMillis() - startedAt);
        double hours = elapsedMillis / 3600000.0;
        double perHour = catches / hours;
        info("%s: (highlight)%d(default) confirmed catches | (highlight)%.1f(default) catches/hour.",
            prefix, catches, perHour);
    }
}
