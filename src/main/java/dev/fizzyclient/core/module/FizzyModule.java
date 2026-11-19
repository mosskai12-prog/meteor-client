package dev.fizzyclient.core.module;

import net.minecraft.client.MinecraftClient;

/**
 * Base class for modules being migrated into Fizzy's own module system.
 *
 * This class intentionally has no Meteor Client imports. Existing Meteor-backed
 * modules remain untouched until they have been migrated and tested.
 */
public abstract class FizzyModule {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean enabled;

    protected FizzyModule(String id, String name, String description, Category category) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Module id cannot be empty.");
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final String getId() { return id; }
    public final String getName() { return name; }
    public final String getDescription() { return description; }
    public final Category getCategory() { return category; }
    public final boolean isEnabled() { return enabled; }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    /** Called once when this module becomes enabled. */
    protected void onEnable() {}

    /** Called once when this module becomes disabled. */
    protected void onDisable() {}

    /** Called by Fizzy's future client tick dispatcher while enabled. */
    public void onTick() {}

    public enum Category {
        COMBAT,
        MOVEMENT,
        PLAYER,
        RENDER,
        WORLD,
        MISC
    }
}
