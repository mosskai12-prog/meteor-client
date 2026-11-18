package com.fizzy.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FizzyClient implements ClientModInitializer {
    public static final String MOD_ID = "fizzy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Fizzy Client initialized.");
    }
}
