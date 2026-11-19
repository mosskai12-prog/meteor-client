/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.systems.hud.elements.InventoryHud;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * Shows the player's inventory as an on-screen HUD element.
 */
public class InventoryViewer extends Module {
    private HudElement inventoryElement;

    public InventoryViewer() {
        super(Categories.Player, "inventory-viewer", "Displays your inventory on screen without opening the inventory screen.");
    }

    @Override
    public void onActivate() {
        inventoryElement = findInventoryElement();

        if (inventoryElement == null) {
            Hud.get().add(InventoryHud.INFO, -4, -4, XAnchor.Right, YAnchor.Bottom);
            inventoryElement = findInventoryElement();
        }

        if (inventoryElement != null && !inventoryElement.isActive()) {
            inventoryElement.toggle();
        }
    }

    @Override
    public void onDeactivate() {
        if (inventoryElement != null && inventoryElement.isActive()) {
            inventoryElement.toggle();
        }
        inventoryElement = null;
    }

    private HudElement findInventoryElement() {
        for (HudElement element : Hud.get()) {
            if (element.info == InventoryHud.INFO) return element;
        }
        return null;
    }
}
