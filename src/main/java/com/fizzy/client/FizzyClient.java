package com.fizzy.client;

import com.fizzy.client.gui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FizzyClient implements ClientModInitializer {
    public static final String MOD_ID = "fizzy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean rightShiftDown = GLFW.glfwGetKey(
                client.getWindow().getWindow(),
                GLFW.GLFW_KEY_RIGHT_SHIFT
            ) == GLFW.GLFW_PRESS;

            if (rightShiftDown && !rightShiftWasDown && client.screen == null) {
                client.setScreen(new ClickGuiScreen());
            }

            rightShiftWasDown = rightShiftDown;
        });

        LOGGER.info("Fizzy Client initialized. Press Right Shift to open the Click GUI.");
    }
}
