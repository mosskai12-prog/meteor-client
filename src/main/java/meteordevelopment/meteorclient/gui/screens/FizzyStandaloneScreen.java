package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
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
    private static final int PANEL_W = 150, PANEL_H = 330, HEADER_H = 34;
    private static final int GAP = 10, ROW_H = 25, PAD = 9;
    private static final int BG = 0xD80B0911, PANEL = 0xF0161220, PANEL_EDGE = 0xFF382B48;
    private static final int ROW = 0xFF211A2B, ROW_HOVER = 0xFF30243D;
    private static final int PURPLE = 0xFFD58BFF, TEXT = 0xFFF4ECFA, MUTED = 0xFF9C91A7;
    private static final int GREEN = 0xFF79E6A2;
    private final List<Category> categories = new ArrayList<>();
    private final Map<Module, Boolean> expanded = new HashMap<>();
    private final Map<Category, Integer> scroll = new HashMap<>();
    private final Map<Category, int[]> positions = new HashMap<>();
    private Category dragging;
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

    private int panelX(int index) {
        Category c = categories.get(index);
        int[] saved = positions.get(c);
        if (saved != null) return Math.max(0, Math.min(width - PANEL_W, saved[0]));
        int total = categories.size() * PANEL_W + Math.max(0, categories.size() - 1) * GAP;
        return Math.max(8, (width - total) / 2) + index * (PANEL_W + GAP);
    }

    private int panelY(int index) {
        int[] saved = positions.get(categories.get(index));
        return saved == null ? Math.max(42, (height - PANEL_H) / 2) : Math.max(8, Math.min(height - 80, saved[1]));
    }

    private int accent() {
        return switch (theme) {
            case "Ocean" -> 0xFF72CFFF;
            case "Crimson" -> 0xFFFF7185;
            case "Mono" -> 0xFFE0E0E0;
            default -> PURPLE;
        };
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, BG);
        drawTextWithShadow(ctx, "FIZZY", 16, 13, accent());
        drawTextWithShadow(ctx, "CLIENT  /  CLICK GUI", 62, 15, MUTED);
        drawTextWithShadow(ctx, "RIGHT SHIFT  •  DRAG HEADERS  •  LEFT CLICK TOGGLE  •  RIGHT CLICK SETTINGS",
            16, height - 17, MUTED);

        for (int i = 0; i < categories.size(); i++) drawPanel(ctx, i, mouseX, mouseY);
        drawThemes(ctx, mouseX, mouseY);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext ctx, int index, int mx, int my) {
        Category category = categories.get(index);
        int x = panelX(index), y = panelY(index);
        ctx.fill(x + 3, y + 4, x + PANEL_W + 3, y + PANEL_H + 4, 0x55000000);
        ctx.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL);
        ctx.fill(x, y, x + PANEL_W, y + 1, PANEL_EDGE);
        ctx.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, PANEL_EDGE);
        ctx.fill(x, y, x + 1, y + PANEL_H, PANEL_EDGE);
        ctx.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, PANEL_EDGE);
        ctx.fill(x + 1, y + 1, x + PANEL_W - 1, y + HEADER_H, 0xFF1D1727);
        String icon = switch (category.name.toLowerCase(Locale.ROOT)) {
            case "combat" -> "✦";
            case "movement" -> "➤";
            case "render" -> "◉";
            case "player" -> "◆";
            default -> "⚙";
        };
        drawCentered(ctx, icon + "  " + category.name.toUpperCase(Locale.ROOT), x, y + 12, PANEL_W, accent());
        ctx.fill(x + 9, y + HEADER_H, x + PANEL_W - 9, y + HEADER_H + 1, PANEL_EDGE);

        int clipTop = y + HEADER_H + 7;
        int clipBottom = y + PANEL_H - 8;
        ctx.enableScissor(x + 3, clipTop, x + PANEL_W - 3, clipBottom);
        int cy = clipTop - scroll.getOrDefault(category, 0);
        for (Module module : Modules.get().getGroup(category)) {
            boolean open = expanded.getOrDefault(module, false);
            boolean hovering = mx >= x + PAD && mx <= x + PANEL_W - PAD && my >= cy && my <= cy + ROW_H;
            ctx.fill(x + PAD, cy, x + PANEL_W - PAD, cy + ROW_H - 1, hovering ? ROW_HOVER : ROW);
            if (module.isActive()) ctx.fill(x + PAD, cy + 3, x + PAD + 2, cy + ROW_H - 4, accent());
            String label = module.title;
            drawCentered(ctx, label, x + PAD + 4, cy + 8, PANEL_W - PAD * 2 - 12, module.isActive() ? accent() : TEXT);
            ctx.fill(x + PANEL_W - 16, cy + 10, x + PANEL_W - 11, cy + 15,
                module.isActive() ? GREEN : 0xFF665D70);
            cy += ROW_H;
            if (open) {
                for (SettingGroup group : module.settings) {
                    for (Setting<?> setting : group) {
                        if (!setting.isVisible()) continue;
                        String value = String.valueOf(setting.get());
                        String line = setting.title + ": " + value;
                        drawTextWithShadow(ctx, trim(line, PANEL_W - 22), x + PAD + 5, cy + 5, MUTED);
                        cy += 17;
                    }
                }
                cy += 4;
            }
        }
        ctx.disableScissor();
        drawTextWithShadow(ctx, " " + Modules.get().getGroup(category).size() + " MODULES", x + 7, y + PANEL_H - 15, MUTED);
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
        int w = 104, h = 25, x = width - w - 14, y = height - h - 28;
        ctx.fill(x, y, x + w, y + h, 0xF0161220);
        ctx.fill(x, y, x + w, y + 1, PANEL_EDGE);
        drawCentered(ctx, "THEMES  ▾", x, y + 8, w, accent());
        if (!themesOpen) return;
        String[] choices = {"Purple Haze", "Ocean", "Crimson", "Mono"};
        int yy = y - choices.length * 22 - 5;
        for (String choice : choices) {
            ctx.fill(x, yy, x + w, yy + 20, 0xF01B1524);
            drawTextWithShadow(ctx, (theme.equals(choice) ? "● " : "○ ") + choice, x + 8, yy + 6,
                theme.equals(choice) ? accent() : TEXT);
            yy += 22;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        int tw = 104, tx = width - tw - 14, ty = height - 25 - 28;
        if (button == GLFW_MOUSE_BUTTON_LEFT && mx >= tx && mx <= tx + tw && my >= ty && my <= ty + 25) {
            themesOpen = !themesOpen;
            return true;
        }
        if (themesOpen && button == GLFW_MOUSE_BUTTON_LEFT) {
            String[] choices = {"Purple Haze", "Ocean", "Crimson", "Mono"};
            int yy = ty - choices.length * 22 - 5;
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
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            int x = panelX(i), y = panelY(i);
            if (mx < x || mx > x + PANEL_W || my < y || my > y + PANEL_H) continue;
            if (button == GLFW_MOUSE_BUTTON_LEFT && my <= y + HEADER_H) {
                dragging = category;
                dragX = mx - x;
                dragY = my - y;
                return true;
            }
            if (my < y + HEADER_H + 7 || my > y + PANEL_H - 8) return true;
            int cy = y + HEADER_H + 7 - scroll.getOrDefault(category, 0);
            for (Module module : Modules.get().getGroup(category)) {
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
        return super.mouseClicked(click, doubled);
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
        if (dragging != null) {
            positions.put(dragging, new int[]{(int)(click.x() - dragX), (int)(click.y() - dragY)});
            return true;
        }
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
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            int x = panelX(i), y = panelY(i);
            if (mouseX >= x && mouseX <= x + PANEL_W && mouseY >= y && mouseY <= y + PANEL_H) {
                int max = Math.max(0, contentHeight(c) - (PANEL_H - HEADER_H - 18));
                scroll.put(c, Math.max(0, Math.min(max, scroll.getOrDefault(c, 0) - (int)(verticalAmount * 24))));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int contentHeight(Category c) {
        int h = 0;
        for (Module module : Modules.get().getGroup(c)) {
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
