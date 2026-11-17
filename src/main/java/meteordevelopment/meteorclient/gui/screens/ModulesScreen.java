/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.MacWindowUtil;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static org.lwjgl.glfw.GLFW.*;

public class ModulesScreen extends TabScreen {
    private Category selectedCategory;
    private Module selectedModule;
    private WWindow moduleWindow;
    private WWindow settingsWindow;
    private WVerticalList moduleList;
    private WTextBox searchBox;
    private WContainerSettings settingsPanel;

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        List<Category> categories = new ArrayList<>();
        for (Category category : Modules.loopCategories()) categories.add(category);
        if (selectedCategory == null || !categories.contains(selectedCategory)) {
            selectedCategory = categories.isEmpty() ? null : categories.get(0);
        }

        WHorizontalList workspace = add(theme.horizontalList()).expandX().widget();
        workspace.spacing = 8;

        // Left navigation
        WWindow navigation = theme.window("FIZZY CLIENT");
        navigation.id = null;
        navigation.padding = 6;
        navigation.view.hasScrollBar = false;
        workspace.add(navigation);

        navigation.add(theme.label("CATEGORIES", true)).expandX().pad(4);
        for (Category category : categories) {
            WButton button = navigation.add(theme.button(category.name)).expandX().widget();
            button.action = () -> {
                selectedCategory = category;
                selectedModule = null;
                reload();
            };
        }

        // Centre module browser
        moduleWindow = theme.window("MODULES");
        moduleWindow.id = null;
        moduleWindow.padding = 6;
        moduleWindow.view.hasScrollBar = false;
        workspace.add(moduleWindow).expandX();

        searchBox = moduleWindow.add(theme.textBox("")).expandX().widget();
        searchBox.setFocused(false);
        searchBox.action = this::refreshModuleList;
        moduleList = moduleWindow.add(theme.verticalList()).expandX().widget();
        moduleList.spacing = 3;
        refreshModuleList();

        // Right settings inspector
        settingsWindow = theme.window("SETTINGS");
        settingsWindow.id = null;
        settingsWindow.padding = 8;
        settingsWindow.view.hasScrollBar = false;
        workspace.add(settingsWindow).expandX();
        refreshSettingsPanel();

        navigation.view.maxHeight = getWindowHeight() - theme.scale(48);
        moduleWindow.view.maxHeight = getWindowHeight() - theme.scale(48);
        settingsWindow.view.maxHeight = getWindowHeight() - theme.scale(48);
    }

    private void refreshModuleList() {
        if (moduleList == null || selectedCategory == null) return;
        moduleList.clear();

        String query = searchBox == null ? "" : searchBox.get().trim().toLowerCase();
        List<Module> modules = new ArrayList<>(Modules.get().getGroup(selectedCategory));
        modules.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.title, b.title));

        for (Module module : modules) {
            if (Config.get().hiddenModules.get().contains(module)) continue;
            if (!query.isEmpty() && !module.title.toLowerCase().contains(query) && !module.name.toLowerCase().contains(query)) continue;

            WHorizontalList row = theme.horizontalList();
            row.spacing = 4;

            WButton select = row.add(theme.button(module.title)).expandX().widget();
            select.action = () -> {
                selectedModule = module;
                refreshSettingsPanel();
            };

            WCheckbox enabled = row.add(theme.checkbox(module.isActive())).widget();
            enabled.action = () -> {
                if (module.isActive() != enabled.checked) module.toggle();
                enabled.checked = module.isActive();
            };

            moduleList.add(row).expandX();
        }
        invalidate();
    }

    private void refreshSettingsPanel() {
        if (settingsWindow == null) return;
        settingsWindow.clear();
        settingsPanel = null;

        if (selectedModule == null) {
            settingsWindow.add(theme.label("Select a module to view its settings.", false)).expandX().pad(6);
            return;
        }

        Module module = selectedModule;
        settingsWindow.add(theme.label(module.title, true)).expandX().pad(4);
        settingsWindow.add(theme.label(module.description, false)).expandX().pad(4);

        WCheckbox active = settingsWindow.add(theme.checkbox(module.isActive())).widget();
        active.action = () -> {
            if (module.isActive() != active.checked) module.toggle();
            active.checked = module.isActive();
        };

        if (!module.settings.groups.isEmpty()) {
            settingsPanel = theme.verticalList();
            settingsPanel.add(theme.settings(module.settings)).expandX();
            settingsWindow.add(settingsPanel).expandX();
        }

        var customWidget = module.getWidget(theme);
        if (customWidget != null) settingsWindow.add(customWidget).expandX();
    }

    @Override
    public void tick() {
        super.tick();
        if (selectedModule != null && settingsPanel != null) {
            selectedModule.settings.tick(settingsPanel, theme);
        }
    }

    @Override
    public boolean keyPressed(KeyInput value) {
        if (locked) return false;

        boolean control = MacWindowUtil.IS_MAC ? value.modifiers() == GLFW_MOD_SUPER : value.modifiers() == GLFW_MOD_CONTROL;
        if (control && value.key() == GLFW_KEY_F) {
            if (searchBox != null) {
                searchBox.setFocused(true);
                searchBox.setCursorMax();
            }
            return true;
        }

        return super.keyPressed(value);
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    @Override
    public void reload() {
        moduleList = null;
        settingsWindow = null;
        settingsPanel = null;
        super.reload();
    }

}
