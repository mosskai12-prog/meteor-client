package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Fizzy's standalone click GUI. This is a real Minecraft Screen, not a Meteor WWidget.
 * It deliberately uses the existing module registry for compatibility with current modules.
 */
public final class FizzyStandaloneScreen extends Screen {
    private static final int PANEL_W = 260, PANEL_H = 360, HEADER_H = 29;
    private static final int GAP = 7, ROW_H = 21, PAD = 7;
    // Fully opaque surfaces keep the Minecraft world from showing through the GUI.
    private static final int BG = 0xFF061426, PANEL = 0xFF0B2440, PANEL_EDGE = 0xFF36BFFF;
    private static final int ROW = 0xFF103457, ROW_HOVER = 0xFF185080;
    private static final int PURPLE = 0xFF65D8FF, TEXT = 0xFFF0FAFF, MUTED = 0xFF9BCBE8;
    private static final int GREEN = 0xFF70F5C5;
    private final List<Category> categories = new ArrayList<>();
    private final Map<Module, Boolean> expanded = new HashMap<>();
    private final Map<Category, Integer> scroll = new HashMap<>();
    private final Map<Category, int[]> positions = new HashMap<>();
    private Category dragging;
    private int selectedCategory;
    private String searchQuery = "";
    private boolean searchFocused;
    private double dragX, dragY;
    private String theme = "Purple Haze";
    private boolean themesOpen;
    private Setting<?> sliderSetting;
    private double sliderX, sliderW;

    public FizzyStandaloneScreen() {
        super(Text.literal("Fizzy Client"));
        for (Category category : Modules.loopCategories()) categories.add(category);
        loadLayout();
    }

    private void loadLayout() {
        File file = configFile();
        if (!file.isFile()) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            p.load(in);
            theme = p.getProperty("theme", theme);
            for (int i = 0; i < categories.size(); i++) {
                Category c = categories.get(i);
                String x = p.getProperty(c.name + ".x");
                String y = p.getProperty(c.name + ".y");
                if (x != null && y != null) positions.put(c, new int[]{Integer.parseInt(x), Integer.parseInt(y)});
            }
        } catch (Exception ignored) {}
    }

    private void saveLayout() {
        File file = configFile();
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        Properties p = new Properties();
        p.setProperty("theme", theme);
        positions.forEach((c, xy) -> {
            p.setProperty(c.name + ".x", Integer.toString(xy[0]));
            p.setProperty(c.name + ".y", Integer.toString(xy[1]));
        });
        try (FileOutputStream out = new FileOutputStream(file)) {
            p.store(out, "Fizzy Client click GUI");
        } catch (IOException ignored) {}
    }

    private File configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("fizzy-clickgui.properties").toFile();
    }

    private int panelX() { return (width - PANEL_W) / 2; }
    private int panelY() { return Math.max(96, (height - PANEL_H) / 2 + 18); }
    private int tabsWidth() { return categories.size() * 82; }
    private int tabsX() { return (width - tabsWidth()) / 2; }
    private int tabY() { return 65; }
    private boolean overSearch(double mx, double my) {
        int w = 240, x = (width - w) / 2;
        return mx >= x && mx <= x + w && my >= 36 && my <= 60;
    }
    private List<Module> visibleModules(Category category) {
        String query = searchQuery.trim().toLowerCase(Locale.ROOT);
        List<Module> all = Modules.get().getGroup(category);
        if (query.isEmpty()) return all;
        return all.stream().filter(module ->
            module.title.toLowerCase(Locale.ROOT).contains(query)
                || module.name.toLowerCase(Locale.ROOT).contains(query)
                || module.description.toLowerCase(Locale.ROOT).contains(query)
        ).toList();
    }

    private int accent() {
        return switch (theme) {
            case "Ocean" -> 0xFF42D9FF;
            case "Crimson" -> 0xFFFF557A;
            case "Mono" -> 0xFFF0EAF5;
            default -> PURPLE;
        };
    }

    // Prevent Minecraft's default screen background blur; this screen draws its own solid backdrop.
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Intentionally empty: no blur and no translucent world background.
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // No full-screen backdrop: keep the Minecraft world visible behind the GUI.
        drawTextWithShadow(ctx, "FIZZY", 12, 10, accent());
        drawTextWithShadow(ctx, "CLIENT  /  CLICK GUI", 53, 12, MUTED);

        int searchW = 240, searchX = (width - searchW) / 2;
        ctx.fill(searchX, 36, searchX + searchW, 60, 0xFF0D2B49);
        ctx.fill(searchX, 36, searchX + searchW, 39, searchFocused ? accent() : 0xFF36BFFF);
        String searchText = searchQuery.isEmpty() ? "Search modules..." : searchQuery;
        drawTextWithShadow(ctx, searchText, searchX + 10, 44, searchQuery.isEmpty() ? MUTED : TEXT);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = searchX + 10 + textRenderer.getWidth(searchQuery);
            ctx.fill(cursorX + 1, 43, cursorX + 2, 55, accent());
        }

        for (int i = 0; i < categories.size(); i++) {
            int x = tabsX() + i * 82, y = tabY();
            boolean active = i == selectedCategory;
            ctx.fill(x, y, x + 78, y + 22, active ? 0xFF14518A : 0xFF0D2B49);
            ctx.fill(x, y, x + 78, y + 2, active ? accent() : 0xFF247EB8);
            drawCentered(ctx, categories.get(i).name.toUpperCase(Locale.ROOT), x, y + 7, 78, active ? accent() : MUTED);
        }

        if (!categories.isEmpty()) drawPanel(ctx, Math.max(0, Math.min(selectedCategory, categories.size() - 1)), mouseX, mouseY);
        drawTextWithShadow(ctx, "RIGHT SHIFT  •  LEFT CLICK TOGGLE  •  RIGHT CLICK SETTINGS",
            12, height - 14, MUTED);
        drawThemes(ctx, mouseX, mouseY);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext ctx, int index, int mx, int my) {
        Category category = categories.get(index);
        int x = panelX(), y = panelY();
        ctx.fill(x + 3, y + 4, x + PANEL_W + 3, y + PANEL_H + 4, 0x55000000);
        ctx.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL);
        ctx.fill(x, y, x + PANEL_W, y + 1, PANEL_EDGE);
        ctx.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, PANEL_EDGE);
        ctx.fill(x, y, x + 1, y + PANEL_H, PANEL_EDGE);
        ctx.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, PANEL_EDGE);
        ctx.fill(x + 1, y + 1, x + PANEL_W - 1, y + HEADER_H, 0xFF0A1E35);
        drawCentered(ctx, category.name.toUpperCase(Locale.ROOT) + " MODULES", x, y + 12, PANEL_W, accent());
        ctx.fill(x + 9, y + HEADER_H, x + PANEL_W - 9, y + HEADER_H + 1, PANEL_EDGE);

        int clipTop = y + HEADER_H + 7;
        int clipBottom = y + PANEL_H - 8;
        ctx.enableScissor(x + 3, clipTop, x + PANEL_W - 3, clipBottom);
        int cy = clipTop - scroll.getOrDefault(category, 0);
        for (Module module : visibleModules(category)) {
            boolean open = expanded.getOrDefault(module, false);
            boolean hovering = mx >= x + PAD && mx <= x + PANEL_W - PAD && my >= cy && my <= cy + ROW_H;
            ctx.fill(x + PAD, cy, x + PANEL_W - PAD, cy + ROW_H - 1, hovering ? ROW_HOVER : ROW);
            if (module.isActive()) ctx.fill(x + PAD, cy + 3, x + PAD + 2, cy + ROW_H - 4, accent());
            drawTextWithShadow(ctx, module.title, x + PAD + 9, cy + 6, module.isActive() ? accent() : TEXT);
            ctx.fill(x + PANEL_W - 16, cy + 10, x + PANEL_W - 11, cy + 15,
                open ? GREEN : (module.isActive() ? GREEN : 0xFF477A9D));
            cy += ROW_H;
            if (open) {
                for (SettingGroup group : module.settings) {
                    for (Setting<?> setting : group) {
                        if (!setting.isVisible()) continue;
                        String line = setting.title + ": " + String.valueOf(setting.get());
                        drawTextWithShadow(ctx, trim(line, PANEL_W - 22), x + PAD + 5, cy + 5, MUTED);
                        cy += 17;
                    }
                }
                cy += 4;
            }
        }
        ctx.disableScissor();
        drawTextWithShadow(ctx, " " + visibleModules(category).size() + " MODULES", x + 7, y + PANEL_H - 15, MUTED);
    }

    private String trim(String value, int maxWidth) {
        while (!value.isEmpty() && textRenderer.getWidth(value) > maxWidth) value = value.substring(0, value.length() - 2) + "…";
        return value;
    }

    private void drawTextWithShadow(DrawContext ctx, String value, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(value), x, y, color);
    }

    private void drawCentered(DrawContext ctx, String value, int x, int y, int w, int color) {
        ctx.drawTextWithShadow(textRenderer, value, x + (w - textRenderer.getWidth(value)) / 2, y, color);
    }

    private void drawThemes(DrawContext ctx, int mx, int my) {
        int w = 88, h = 22, x = width - w - 10, y = height - h - 22;
        ctx.fill(x, y, x + w, y + h, 0xFF281A38);
        ctx.fill(x, y, x + w, y + 1, PANEL_EDGE);
        drawCentered(ctx, "THEMES  ▾", x, y + 8, w, accent());
        if (!themesOpen) return;
        String[] choices = {"Purple Haze", "Ocean", "Crimson", "Mono"};
        int yy = y - choices.length * 22 - 5;
        for (String choice : choices) {
            ctx.fill(x, yy, x + w, yy + 20, 0xFF103457);
            drawTextWithShadow(ctx, (theme.equals(choice) ? "● " : "○ ") + choice, x + 8, yy + 6,
                theme.equals(choice) ? accent() : TEXT);
            yy += 19;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        int tw = 88, tx = width - tw - 10, ty = height - 22 - 22;
        if (button == GLFW_MOUSE_BUTTON_LEFT && overSearch(mx, my)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        if (button == GLFW_MOUSE_BUTTON_LEFT && my >= tabY() && my <= tabY() + 22 && mx >= tabsX() && mx <= tabsX() + tabsWidth()) {
            int index = (int)((mx - tabsX()) / 82);
            if (index >= 0 && index < categories.size()) selectedCategory = index;
            return true;
        }
        if (button == GLFW_MOUSE_BUTTON_LEFT && mx >= tx && mx <= tx + tw && my >= ty && my <= ty + 22) {
            themesOpen = !themesOpen;
            return true;
        }
        if (themesOpen && button == GLFW_MOUSE_BUTTON_LEFT) {
            String[] choices = {"Purple Haze", "Ocean", "Crimson", "Mono"};
            int yy = ty - choices.length * 19 - 5;
            for (String choice : choices) {
                if (mx >= tx && mx <= tx + tw && my >= yy && my <= yy + 20) {
                    theme = choice;
                    themesOpen = false;
                    saveLayout();
                    return true;
                }
                yy += 22;
            }
        }
        if (categories.isEmpty()) return super.mouseClicked(click, doubled);
        Category category = categories.get(Math.max(0, Math.min(selectedCategory, categories.size() - 1)));
        int x = panelX(), y = panelY();
        if (mx < x || mx > x + PANEL_W || my < y || my > y + PANEL_H) return super.mouseClicked(click, doubled);
        if (my < y + HEADER_H + 7 || my > y + PANEL_H - 8) return true;
        int cy = y + HEADER_H + 7 - scroll.getOrDefault(category, 0);
        for (Module module : visibleModules(category)) {
            if (mx >= x + PAD && mx <= x + PANEL_W - PAD && my >= cy && my <= cy + ROW_H) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
                else if (button == GLFW_MOUSE_BUTTON_RIGHT) expanded.put(module, !expanded.getOrDefault(module, false));
                return true;
            }
            cy += ROW_H;
            if (expanded.getOrDefault(module, false)) {
                for (SettingGroup group : module.settings) {
                    for (Setting<?> setting : group) {
                        if (!setting.isVisible()) continue;
                        if (mx >= x + PAD && mx <= x + PANEL_W - PAD && my >= cy && my <= cy + 17) {
                            if (setting instanceof BoolSetting bool) bool.set(!bool.get());
                            else if (setting instanceof EnumSetting<?> mode) {
                                List<String> options = mode.getSuggestions();
                                if (!options.isEmpty()) {
                                    int current = options.indexOf(mode.get().toString());
                                    mode.parse(options.get((current + 1) % options.size()));
                                }
                            } else if (setting instanceof IntSetting || setting instanceof DoubleSetting) {
                                sliderSetting = setting;
                                sliderX = x + PAD + 5;
                                sliderW = PANEL_W - PAD * 2 - 10;
                                updateSlider(mx);
                            }
                            return true;
                        }
                        cy += 17;
                    }
                }
                cy += 4;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (searchFocused) {
            if (input.key() == GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
            if (input.key() == GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    int end = searchQuery.offsetByCodePoints(searchQuery.length(), -1);
                    searchQuery = searchQuery.substring(0, end);
                    for (Category category : categories) scroll.put(category, 0);
                }
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!searchFocused) return super.charTyped(input);
        int codepoint = input.codepoint();
        if (Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint)) {
            searchQuery += new String(Character.toChars(codepoint));
            for (Category category : categories) scroll.put(category, 0);
        }
        return true;
    }

    private void updateSlider(double mouseX) {
        if (sliderSetting == null || sliderW <= 0) return;
        double ratio = Math.max(0, Math.min(1, (mouseX - sliderX) / sliderW));
        if (sliderSetting instanceof IntSetting setting) {
            int value = (int) Math.round(setting.sliderMin + (setting.sliderMax - setting.sliderMin) * ratio);
            sliderSetting.parse(Integer.toString(value));
        } else if (sliderSetting instanceof DoubleSetting setting) {
            double value = setting.sliderMin + (setting.sliderMax - setting.sliderMin) * ratio;
            sliderSetting.parse(Double.toString(value));
        }
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (sliderSetting != null) {
            updateSlider(click.x());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            saveLayout();
            return true;
        }
        if (sliderSetting != null) {
            sliderSetting = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (categories.isEmpty()) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        int x = panelX(), y = panelY();
        if (mouseX >= x && mouseX <= x + PANEL_W && mouseY >= y && mouseY <= y + PANEL_H) {
            Category category = categories.get(Math.max(0, Math.min(selectedCategory, categories.size() - 1)));
            int max = Math.max(0, contentHeight(category) - (PANEL_H - HEADER_H - 18));
            scroll.put(category, Math.max(0, Math.min(max, scroll.getOrDefault(category, 0) - (int)(verticalAmount * 24))));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int contentHeight(Category c) {
        int h = 0;
        for (Module module : visibleModules(c)) {
            h += ROW_H;
            if (expanded.getOrDefault(module, false)) {
                for (SettingGroup group : module.settings)
                    for (Setting<?> setting : group) if (setting.isVisible()) h += 17;
                h += 4;
            }
        }
        return h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
