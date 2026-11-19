package dev.fizzyclient.core.module;

public abstract class FizzyModule {
    private final String name;
    private final String description;
    private boolean enabled;

    protected FizzyModule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public final String getName() { return name; }
    public final String getDescription() { return description; }
    public final boolean isEnabled() { return enabled; }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (enabled) onEnable();
        else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
}
