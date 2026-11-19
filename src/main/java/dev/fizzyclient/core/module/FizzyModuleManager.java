package dev.fizzyclient.core.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fizzy-owned module registry. This registry is independent of Meteor's
 * Modules and Module classes; migrated modules will be registered here.
 */
public final class FizzyModuleManager {
    private static final FizzyModuleManager INSTANCE = new FizzyModuleManager();

    private final Map<String, FizzyModule> modules = new LinkedHashMap<>();

    private FizzyModuleManager() {}

    public static FizzyModuleManager get() {
        return INSTANCE;
    }

    public synchronized void register(FizzyModule module) {
        if (module == null) throw new IllegalArgumentException("Module cannot be null.");
        String id = module.getId().toLowerCase(java.util.Locale.ROOT);
        if (modules.containsKey(id)) {
            throw new IllegalStateException("A Fizzy module with id '" + id + "' is already registered.");
        }
        modules.put(id, module);
    }

    public synchronized Optional<FizzyModule> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(modules.get(id.toLowerCase(java.util.Locale.ROOT)));
    }

    public synchronized Collection<FizzyModule> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    public synchronized List<FizzyModule> getEnabled() {
        List<FizzyModule> enabled = new ArrayList<>();
        for (FizzyModule module : modules.values()) {
            if (module.isEnabled()) enabled.add(module);
        }
        return Collections.unmodifiableList(enabled);
    }

    public synchronized void disableAll() {
        for (FizzyModule module : modules.values()) module.setEnabled(false);
    }
}
