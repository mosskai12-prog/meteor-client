/*
 * Derived from Aoba Client.
 * Copyright (C) 2019-2024 Aoba contributors.
 * Licensed under the GNU General Public License, Version 3 or later.
 */
package net.aoba.gui.screens;

import static net.aoba.AobaClient.MC;

import net.aoba.AobaClient;
import net.aoba.gui.components.widgets.AobaButtonWidget;
import net.aoba.gui.screens.AobaCreditsScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {
	private final int buttonWidth = 170;
	private final int buttonHeight = 26;
	private final int spacing = 6;

	public MainMenuScreen() {
		super(Text.of("Fizzy Client"));
	}

	@Override
	protected void init() {
		super.init();
		int startX = (width - buttonWidth * 2 - spacing) / 2;
		int startY = (height - (buttonHeight * 3 + spacing * 2)) / 2 + 24;

		AobaButtonWidget singleplayer = new AobaButtonWidget(startX, startY, buttonWidth, buttonHeight, Text.of("Singleplayer"));
		singleplayer.setPressAction(b -> client.setScreen(new SelectWorldScreen(this)));
		addDrawableChild(singleplayer);

		AobaButtonWidget multiplayer = new AobaButtonWidget(startX + buttonWidth + spacing, startY, buttonWidth, buttonHeight, Text.of("Multiplayer"));
		multiplayer.setPressAction(b -> client.setScreen(new MultiplayerScreen(this)));
		addDrawableChild(multiplayer);

		AobaButtonWidget realms = new AobaButtonWidget(startX, startY + buttonHeight + spacing, buttonWidth, buttonHeight, Text.of("Realms"));
		realms.setPressAction(b -> client.setScreen(new RealmsMainScreen(this)));
		addDrawableChild(realms);

		AobaButtonWidget settings = new AobaButtonWidget(startX + buttonWidth + spacing, startY + buttonHeight + spacing, buttonWidth, buttonHeight, Text.of("Settings"));
		settings.setPressAction(b -> client.setScreen(new OptionsScreen(this, MC.options)));
		addDrawableChild(settings);

		AobaButtonWidget credits = new AobaButtonWidget(startX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight, Text.of("Credits"));
		credits.setPressAction(b -> MC.setScreen(new AobaCreditsScreen()));
		addDrawableChild(credits);

		AobaButtonWidget quit = new AobaButtonWidget(startX + buttonWidth + spacing, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight, Text.of("Quit"));
		quit.setPressAction(b -> client.stop());
		addDrawableChild(quit);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, "FIZZY CLIENT", width / 2, height / 2 - 100, 0xFFB56CFF);
		context.drawCenteredTextWithShadow(textRenderer, "Aoba-derived UI framework", width / 2, height / 2 - 82, 0xFFB0A6BC);
		context.drawTextWithShadow(textRenderer, "Fizzy Client", 6, height - 12, 0xFFB56CFF);
	}

	@Override
	protected void renderPanoramaBackground(DrawContext context, float delta) {
		super.renderPanoramaBackground(context, delta);
	}
}
