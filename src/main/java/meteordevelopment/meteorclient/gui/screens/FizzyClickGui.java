/*
 * Fizzy Client Click GUI.
 * Keeps Meteor's existing module and setting registry.
 */
package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.util.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

final class FizzyClickGui extends WWidget {
    // Layout constants are in GUI-scaled units.
    private static final double PANEL_W = 110, PANEL_H = 202, GAP = 8;
    private static final double HEADER_H = 22, ROW_H = 18, ROW_GAP = 3, PAD = 5;
    private static final Color PANEL = new Color(18, 14, 26, 225);
    private static final Color BORDER = new Color(42, 34, 54, 185);
    private static final Color ROW = new Color(27, 22, 36, 235);
    private static final Color ROW_HOVER = new Color(43, 33, 53, 245);
    private static final Color ACCENT = new Color(204, 102, 224, 255);
    private static final Color TEXT = new Color(239, 231, 244, 245);
    private static final Color MUTED = new Color(142, 135, 156, 255);
    private static final Color RED = new Color(255, 77, 77, 255);
    private static final Color GREEN = new Color(77, 255, 136, 255);
    private static final Color SETTING_BG = new Color(15, 12, 21, 245);

    private final List<Category> categories = new ArrayList<>();
    private final Map<Module, Boolean> expanded = new HashMap<>();
    private final Map<Module, Double> expansion = new HashMap<>();
    private final Map<Category, Double> scroll = new HashMap<>();
    private final Map<Module, Double> enabledAnim = new HashMap<>();
    private final Map<Module, Double> hoverAnim = new HashMap<>();
    private Module sliderDragging;
    private String searchQuery = "";
    private boolean searchFocused;
    private Setting<?> draggingSetting;

    FizzyClickGui() {
        for (Category category : Modules.loopCategories()) categories.add(category);
    }

    @Override protected void onCalculateSize() { width = getWindowWidth(); height = getWindowHeight(); }
    @Override protected void onCalculateWidgetPositions() { x = 0; y = 0; width = getWindowWidth(); height = getWindowHeight(); }

    private double s(double n) { return theme.scale(n); }
    private double panelY() { return Math.max(s(12), (height - s(PANEL_H)) / 2); }
    private double totalWidth() { return categories.size() * s(PANEL_W) + Math.max(0, categories.size() - 1) * s(GAP); }
    private double startX() { return (width - totalWidth()) / 2; }
    private double panelX(int i) { return startX() + i * s(PANEL_W + GAP); }
    private static double ease(double current, double target, double speed, double dt) {
        return current + (target - current) * (1 - Math.exp(-speed * Math.min(dt, 0.05)));
    }

    private void rounded(GuiRenderer r, double x, double y, double w, double h, double radius, Color color) {
        // Scanline rounded rectangle: uses only the renderer's basic quads.
        int lines = Math.max(2, (int)Math.ceil(h));
        for (int i = 0; i < lines; i++) {
            double yy = y + h * i / lines;
            double hh = h / lines + 0.6;
            double inset = 0;
            double cy = yy + hh / 2 - y;
            if (cy < radius) inset = radius - Math.sqrt(Math.max(0, radius * radius - (radius - cy) * (radius - cy)));
            else if (cy > h - radius) {
                double d = cy - (h - radius);
                inset = radius - Math.sqrt(Math.max(0, radius * radius - d * d));
            }
            r.quad(x + inset, yy, Math.max(0, w - inset * 2), hh, color);
        }
    }

    private List<Module> modules(Category c) {
        List<Module> all = Modules.get().getGroup(c);
        if (searchQuery.isBlank()) return all;
        String query = searchQuery.toLowerCase(Locale.ROOT);
        return all.stream().filter(module ->
            module.title.toLowerCase(Locale.ROOT).contains(query)
                || module.name.toLowerCase(Locale.ROOT).contains(query)
                || module.description.toLowerCase(Locale.ROOT).contains(query)
        ).toList();
    }
    private double settingsHeight(Module m) {
        if (!expanded.getOrDefault(m, false)) return 0;
        double h = s(5);
        for (SettingGroup group : m.settings) {
            if (!group.sectionExpanded) continue;
            for (Setting<?> setting : group) if (setting.isVisible()) h += settingHeight(setting);
        }
        return h + s(3);
    }
    private double settingHeight(Setting<?> setting) {
        if (setting instanceof BoolSetting) return s(11);
        if (setting instanceof EnumSetting<?>) return s(23);
        if (setting instanceof IntSetting || setting instanceof DoubleSetting) return s(20);
        return s(12);
    }
    private double contentHeight(Category c) {
        double h = s(HEADER_H + 5);
        for (Module m : modules(c)) h += s(ROW_H + ROW_GAP) + settingsHeight(m);
        return h;
    }

    @Override
    protected void onRender(GuiRenderer r, double mx, double my, double delta) {
        double px0 = startX(), py = panelY(), pw = s(PANEL_W), ph = Math.min(s(PANEL_H), height - s(24));
        // Gentle dim, leaving the world visible.
        r.quad(0, 0, width, height, new Color(0, 0, 0, 82));
        drawSearchBar(r);
        for (int ci = 0; ci < categories.size(); ci++) {
            Category category = categories.get(ci);
            double px = panelX(ci);
            double entrance = Math.min(1, Math.max(0, 1 - Math.pow(1 - Math.min(1, (delta * 0 + 1)), 3)));
            rounded(r, px + s(1), py + s(2), pw, ph, s(5), new Color(0, 0, 0, 75));
            rounded(r, px, py, pw, ph, s(5), PANEL);
            rounded(r, px, py, pw, ph, s(5), BORDER);
            // Inner panel fill keeps the border subtle.
            rounded(r, px + s(0.7), py + s(0.7), pw - s(1.4), ph - s(1.4), s(4.5), PANEL);

            String icon = switch (category.name.toLowerCase(Locale.ROOT)) {
                case "combat" -> "⚔";
                case "movement" -> "⌁";
                case "render" -> "◉";
                case "player" -> "♟";
                default -> "⚙";
            };
            String heading = icon + " " + category.name;
            double titleW = theme.textWidth(heading, heading.length(), false);
            r.text(heading, px + (pw - titleW) / 2, py + s(7), TEXT, false);
            r.quad(px + s(4), py + s(HEADER_H), pw - s(8), s(0.7), BORDER);

            double top = py + s(HEADER_H + 5);
            double bottom = py + ph - s(5);
            double offset = scroll.getOrDefault(category, 0.0);
            r.scissorStart(px + s(2), top, pw - s(4), bottom - top);
            double cy = top - offset;
            for (Module module : modules(category)) {
                double rowY = cy;
                boolean hover = mx >= px + s(PAD) && mx <= px + pw - s(PAD) && my >= rowY && my <= rowY + s(ROW_H);
                double ha = hoverAnim.compute(module, (k,v) -> ease(v == null ? 0 : v, hover ? 1 : 0, 17, delta));
                double ea = enabledAnim.compute(module, (k,v) -> ease(v == null ? (module.isActive() ? 1 : 0) : v, module.isActive() ? 1 : 0, 15, delta));
                double open = expansion.compute(module, (k,v) -> ease(v == null ? (expanded.getOrDefault(module, false) ? 1 : 0) : v, expanded.getOrDefault(module, false) ? 1 : 0, 18, delta));
                Color rowColor = mix(ROW, ROW_HOVER, ha);
                rounded(r, px + s(PAD), rowY, pw - s(PAD * 2), s(ROW_H), s(3), rowColor);
                if (ea > 0.01) rounded(r, px + s(PAD), rowY, pw - s(PAD * 2), s(ROW_H), s(3), new Color(ACCENT.r, ACCENT.g, ACCENT.b, (int)(22 * ea)));
                Color label = mix(MUTED, ACCENT, ea);
                double tw = theme.textWidth(module.title, module.title.length(), false);
                r.text(module.title, px + (pw - tw) / 2, rowY + s(5), label, false);
                // Settings state dot: green when open, red when closed.
                boolean isOpen = expanded.getOrDefault(module, false);
                r.quad(px + pw - s(8), rowY + s(8), s(2.5), s(2.5), isOpen ? GREEN : RED);
                cy += s(ROW_H + ROW_GAP);
                double fullSettings = settingsHeight(module);
                if (fullSettings > 0 || open > 0.015) {
                    double drawH = fullSettings * open;
                    rounded(r, px + s(PAD), cy, pw - s(PAD * 2), drawH, s(3), SETTING_BG);
                    double sy = cy + s(3);
                    for (SettingGroup group : module.settings) {
                        if (!group.sectionExpanded) continue;
                        for (Setting<?> setting : group) {
                            if (!setting.isVisible()) continue;
                            drawSetting(r, setting, px + s(PAD + 4), sy, pw - s(PAD * 2 + 8), mx, my);
                            sy += settingHeight(setting);
                        }
                    }
                    cy += drawH;
                }
            }
            r.scissorEnd();
        }
        drawThemesTab(r);
    }

    private void drawSearchBar(GuiRenderer r) {
        double w = s(150), h = s(19);
        double x = (width - w) / 2, y = s(10);
        rounded(r, x, y, w, h, s(4), new Color(18, 14, 26, 245));
        rounded(r, x, y, w, s(1), s(0.5), searchFocused ? ACCENT : BORDER);
        String shown = searchQuery.isEmpty() ? "Search modules..." : searchQuery;
        Color color = searchQuery.isEmpty() ? MUTED : TEXT;
        r.text("⌕", x + s(6), y + s(5), ACCENT, false);
        r.text(shown, x + s(19), y + s(5), color, false);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            double textW = theme.textWidth(searchQuery, searchQuery.length(), false);
            r.quad(x + s(19) + textW + s(1), y + s(5), s(1), s(8), ACCENT);
        }
    }

    private boolean isSearchBar(double mx, double my) {
        double w = s(150), h = s(19);
        double x = (width - w) / 2, y = s(10);
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int)(a.r + (b.r-a.r)*t), (int)(a.g + (b.g-a.g)*t), (int)(a.b + (b.b-a.b)*t), (int)(a.a + (b.a-a.a)*t));
    }

    private void drawSetting(GuiRenderer r, Setting<?> setting, double x, double y, double w, double mx, double my) {
        if (setting instanceof BoolSetting bool) {
            r.text((bool.get() ? "✓" : "✗") + "  " + setting.title, x, y + s(2), bool.get() ? GREEN : RED, false);
        } else if (setting instanceof IntSetting integer) {
            drawSlider(r, setting.title, String.valueOf(integer.get()), x, y, w,
                integer.sliderMin, integer.sliderMax, integer.get());
        } else if (setting instanceof DoubleSetting number) {
            drawSlider(r, setting.title, String.format(Locale.ROOT, "%." + number.decimalPlaces + "f", number.get()), x, y, w,
                number.sliderMin, number.sliderMax, number.get());
        } else if (setting instanceof EnumSetting<?> mode) {
            r.text(setting.title + ":", x, y, MUTED, false);
            r.text(String.join("  ", mode.getSuggestions()), x, y + s(10), ACCENT, false);
        } else {
            r.text(setting.title + ": " + String.valueOf(setting.get()), x, y + s(2), MUTED, false);
        }
    }

    private void drawSlider(GuiRenderer r, String name, String value, double x, double y, double w, double min, double max, double current) {
        r.text(name, x, y, MUTED, false);
        double vw = theme.textWidth(value, value.length(), false);
        r.text(value, x + w - vw, y, TEXT, false);
        double trackY = y + s(11), trackW = w;
        rounded(r, x, trackY, trackW, s(2.5), s(1.2), new Color(42, 36, 51, 255));
        double ratio = max <= min ? 0 : Math.max(0, Math.min(1, (current-min)/(max-min)));
        rounded(r, x, trackY, trackW * ratio, s(2.5), s(1.2), ACCENT);
        r.quad(x + trackW * ratio - s(1.5), trackY - s(1), s(4), s(4), TEXT);
    }

    private void drawThemesTab(GuiRenderer r) {
        double w = s(62), h = s(15), x = width - w - s(5), y = height - h - s(5);
        rounded(r, x, y, w, h, s(4), new Color(18, 14, 26, 235));
        r.quad(x + s(6), y + s(6), s(3), s(3), ACCENT);
        r.text("Themes", x + s(13), y + s(4), TEXT, false);
    }

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        if (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_RIGHT && button != GLFW_MOUSE_BUTTON_MIDDLE) return false;
        if (isSearchBar(mx, my)) { searchFocused = true; return true; }
        searchFocused = false;
        for (int ci = 0; ci < categories.size(); ci++) {
            Category category = categories.get(ci);
            double px = panelX(ci), py = panelY(), pw = s(PANEL_W), ph = s(PANEL_H);
            if (mx < px || mx > px + pw || my < py || my > py + ph) continue;
            double cy = py + s(HEADER_H + 5) - scroll.getOrDefault(category, 0.0);
            for (Module module : modules(category)) {
                if (mx >= px + s(PAD) && mx <= px + pw - s(PAD) && my >= cy && my <= cy + s(ROW_H)) {
                    if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
                    else if (button == GLFW_MOUSE_BUTTON_RIGHT && !module.settings.groups.isEmpty()) expanded.put(module, !expanded.getOrDefault(module, false));
                    else if (button == GLFW_MOUSE_BUTTON_MIDDLE) {
                        mc.setScreen(theme.moduleScreen(module));
                    }
                    return true;
                }
                cy += s(ROW_H + ROW_GAP);
                if (expanded.getOrDefault(module, false)) {
                    for (SettingGroup group : module.settings) {
                        if (!group.sectionExpanded) continue;
                        for (Setting<?> setting : group) {
                            if (!setting.isVisible()) continue;
                            double sh = settingHeight(setting);
                            if (my >= cy && my <= cy + sh && mx >= px + s(PAD) && mx <= px + pw - s(PAD)) {
                                if (setting instanceof BoolSetting bool) bool.set(!bool.get());
                                else if (setting instanceof EnumSetting<?> mode) cycleMode(mode);
                                else if (setting instanceof IntSetting || setting instanceof DoubleSetting) {
                                    sliderDragging = module;
                                    draggingSetting = setting;
                                    updateSlider(setting, mx, px + s(PAD + 4), pw - s(PAD * 2 + 8));
                                }
                                return true;
                            }
                            cy += sh;
                        }
                    }
                    cy += s(3);
                }
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void cycleMode(EnumSetting mode) {
        List values = mode.getSuggestions();
        if (values.isEmpty()) return;
        int index = values.indexOf(mode.get().toString());
        mode.parse((String) values.get((index + 1) % values.size()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void updateSlider(Setting setting, double mx, double x, double w) {
        double ratio = Math.max(0, Math.min(1, (mx - x) / w));
        if (setting instanceof IntSetting i) {
            int value = (int)Math.round(i.sliderMin + (i.sliderMax - i.sliderMin) * ratio);
            setting.set(value);
        } else if (setting instanceof DoubleSetting d) {
            double value = d.sliderMin + (d.sliderMax - d.sliderMin) * ratio;
            double scale = Math.pow(10, d.decimalPlaces);
            setting.set(Math.round(value * scale) / scale);
        }
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        if (draggingSetting != null) {
            for (int ci = 0; ci < categories.size(); ci++) {
                double px = panelX(ci), pw = s(PANEL_W);
                if (mouseX >= px && mouseX <= px + pw) {
                    updateSlider(draggingSetting, mouseX, px + s(PAD + 4), pw - s(PAD * 2 + 8));
                    break;
                }
            }
        }
    }

    @Override
    public boolean onMouseReleased(Click click) {
        draggingSetting = null;
        sliderDragging = null;
        return false;
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        double mx = mc.mouse.getX() / mc.getWindow().getScaleFactor();
        double my = mc.mouse.getY() / mc.getWindow().getScaleFactor();
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            double px = panelX(i), py = panelY();
            if (mx >= px && mx <= px + s(PANEL_W) && my >= py && my <= py + s(PANEL_H)) {
                double max = Math.max(0, contentHeight(c) - s(PANEL_H - HEADER_H - 10));
                scroll.put(c, Math.max(0, Math.min(max, scroll.getOrDefault(c, 0.0) - amount * s(18))));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        if (!searchFocused) return false;
        int codepoint = input.codepoint();
        if (Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint)) {
            searchQuery += new String(Character.toChars(codepoint));
            for (Category category : categories) scroll.put(category, 0.0);
        }
        return true;
    }

    @Override
    public boolean onKeyPressed(KeyInput input) {
        if (input.key() == GLFW_KEY_ESCAPE) {
            if (searchFocused) { searchFocused = false; return true; }
            mc.setScreen(null);
            return true;
        }
        if (searchFocused && input.key() == GLFW_KEY_BACKSPACE) {
            if (!searchQuery.isEmpty()) {
                int end = searchQuery.offsetByCodePoints(searchQuery.length(), -1);
                searchQuery = searchQuery.substring(0, end);
                for (Category category : categories) scroll.put(category, 0.0);
            }
            return true;
        }
        return searchFocused;
    }
}
