package dev.fizzyclient.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FizzyModuleManager {
    private final List<FizzyModule> modules = new ArrayList<>();

    public void register(FizzyModule module) {
        modules.add(module);
    }

    public List<FizzyModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void tick() {
        for (FizzyModule module : modules) {
            if (module.isEnabled()) module.onTick();
        }
    }

    public void disableAll() {
        for (FizzyModule module : modules) module.setEnabled(false);
    }
}
