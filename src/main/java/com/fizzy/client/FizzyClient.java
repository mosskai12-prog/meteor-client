package com.fizzy.client;

import com.fizzy.client.gui.ClickGuiScreen;
import com.fizzy.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FizzyClient implements ClientModInitializer {
    public static final String MOD_ID = "fizzy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ModuleManager MODULES = new ModuleManager();
    private static boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean rightShiftDown = GLFW.glfwGetKey(
                client.getWindow().handle(),
                GLFW.GLFW_KEY_RIGHT_SHIFT
            ) == GLFW.GLFW_PRESS;

            if (rightShiftDown && !rightShiftWasDown && client.screen == null) {
                client.setScreen(new ClickGuiScreen());
            }
            rightShiftWasDown = rightShiftDown;
            MODULES.tick(client);
        });

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(MOD_ID, "fizzy_hud"),
            (graphics, tickCounter) -> MODULES.renderHud(
                graphics,
                net.minecraft.client.Minecraft.getInstance()
            )
        );

        LOGGER.info("Fizzy Client initialized with {} working modules.", MODULES.getStates().size());
    }
}
