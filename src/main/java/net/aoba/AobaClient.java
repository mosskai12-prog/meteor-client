/*
 * Derived from Aoba Client.
 * Copyright (C) 2019-2024 Aoba contributors.
 * Licensed under the GNU General Public License, Version 3 or later.
 */
package net.aoba;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.aoba.api.IAddon;
import net.aoba.command.GlobalChat;
import net.aoba.gui.GuiManager;
import net.aoba.gui.font.FontManager;
import net.aoba.managers.CombatManager;
import net.aoba.managers.CommandManager;
import net.aoba.managers.EntityManager;
import net.aoba.managers.EventManager;
import net.aoba.managers.ModuleManager;
import net.aoba.managers.SettingManager;
import net.aoba.managers.altmanager.AltManager;
import net.aoba.managers.macros.MacroManager;
import net.aoba.managers.proxymanager.ProxyManager;
import net.aoba.managers.rotation.RotationManager;
import net.aoba.mixin.interfaces.IMinecraftClient;
import net.aoba.module.Module;
import net.aoba.settings.friends.FriendsList;
import net.minecraft.client.MinecraftClient;

/**
 * Fizzy client runtime. The Aoba-derived UI and module framework are retained,
 * while third-party addon entrypoints and unsolicited network services are
 * intentionally not started.
 */
public class AobaClient {
	public static final String AOBA_VERSION = "Fizzy";

	public static MinecraftClient MC;
	public static IMinecraftClient IMC;

	public RotationManager rotationManager;
	public ModuleManager moduleManager;
	public CommandManager commandManager;
	// Retained only for legacy screen source compatibility; Fizzy does not initialize or persist it.
	public AltManager altManager;
	public ProxyManager proxyManager;
	public GuiManager guiManager;
	public FontManager fontManager;
	public CombatManager combatManager;
	public SettingManager settingManager;
	public FriendsList friendsList;
	// Compatibility field only; the Global Chat socket listener is never started.
	public GlobalChat globalChat;
	public EventManager eventManager;
	public MacroManager macroManager;

	// Kept empty for compatibility with UI code; external addon entrypoints are
	// not discovered or executed by Fizzy.
	public static List<IAddon> addons = new ArrayList<>();
	public static Logger LOGGER;

	public void Initialize() {
		MC = MinecraftClient.getInstance();
		IMC = (IMinecraftClient) MC;
		LOGGER = LogUtils.getLogger();
	}

	public void loadAssets() {
		LOGGER.info("[Fizzy] Starting client");
		eventManager = new EventManager();

		settingManager = new SettingManager();
		friendsList = new FriendsList();
		rotationManager = new RotationManager();

		// No third-party addon entrypoints are loaded automatically.
		moduleManager = new ModuleManager(List.of());
		commandManager = new CommandManager(List.of());

		fontManager = new FontManager();
		fontManager.Initialize();
		combatManager = new CombatManager();
		entityManager = new EntityManager();
		macroManager = new MacroManager();

		guiManager = new GuiManager();
		guiManager.Initialize();

		// Proxy support is user-configured and does not initiate a connection.
		proxyManager = new ProxyManager();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				endClient();
			} catch (Exception e) {
				LOGGER.error("[Fizzy] Error during shutdown", e);
			}
		}));

		SettingManager.loadGlobalSettings();
		SettingManager.loadSettings();
		LOGGER.info("[Fizzy] Ready");
	}

	public EntityManager entityManager;

	public void endClient() {
		LOGGER.info("[Fizzy] Shutting down");
		try {
			SettingManager.saveSettings();
			friendsList.save();
			macroManager.save();
			moduleManager.modules.forEach(Module::onDisable);
		} catch (Exception e) {
			LOGGER.error("[Fizzy] Error saving data", e);
		}
	}
}
