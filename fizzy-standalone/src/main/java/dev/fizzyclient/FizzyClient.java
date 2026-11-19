package dev.fizzyclient;

import dev.fizzyclient.core.module.FizzyModuleManager;
import dev.fizzyclient.gui.FizzyClickGui;
import dev.fizzyclient.modules.SellModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class FizzyClient implements ClientModInitializer {
    public static final FizzyModuleManager MODULES = new FizzyModuleManager();
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        MODULES.register(new SellModule());

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.fizzy.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new FizzyClickGui());
                else if (client.currentScreen instanceof FizzyClickGui) client.setScreen(null);
            }
            MODULES.tick();
        });
    }
}
