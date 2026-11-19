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

import java.util.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Fizzy Client's modern three-column module workspace.
 * Uses Minecraft's native text and rendering APIs, with a responsive desktop-style layout.
 */
public final class FizzyStandaloneScreen extends Screen {
    private static final int OUTER_W = 930, OUTER_H = 500;
    private static final int SIDEBAR_W = 132, SETTINGS_W = 246;
    private static final int TOP_H = 48, FOOT_H = 22;
    private static final int BG = 0xFF17191D, PANEL = 0xFF202329, PANEL_2 = 0xFF282C33;
    private static final int EDGE = 0xFF454A53, TEXT = 0xFFF0F1F3, MUTED = 0xFFA4AAB4;
    private static final int ACCENT = 0xFFD6C86D, ACTIVE = 0xFF34382F;

    private final List<Category> categories = new ArrayList<>();
    private final Map<Category, Integer> scroll = new HashMap<>();
    private final Map<Module, Boolean> expanded = new HashMap<>();
    private int selectedCategory;
    private Module selectedModule;
    private String searchQuery = "";
    private boolean searchFocused;
    private boolean enabledOnly;
    private String topTab = "Modules";
    private Setting<?> sliderSetting;
    private double sliderX, sliderW;

    public FizzyStandaloneScreen() {
        super(Text.literal("Fizzy Client"));
        for (Category category : Modules.loopCategories()) categories.add(category);
        if (!categories.isEmpty()) {
            List<Module> first = Modules.get().getGroup(categories.get(0));
            if (!first.isEmpty()) selectedModule = first.get(0);
        }
    }

    private int panelX() { return (width - Math.min(OUTER_W, width - 24)) / 2; }
    private int panelY() { return (height - Math.min(OUTER_H, height - 24)) / 2; }
    private int panelW() { return Math.min(OUTER_W, width - 24); }
    private int panelH() { return Math.min(OUTER_H, height - 24); }
    private int sideX() { return panelX() + 8; }
    private int sideY() { return panelY() + TOP_H + 8; }
    private int sideH() { return panelH() - TOP_H - FOOT_H - 16; }
    private int centerX() { return sideX() + SIDEBAR_W + 10; }
    private int settingsX() { return panelX() + panelW() - SETTINGS_W - 8; }
    private int centerW() { return settingsX() - centerX() - 10; }
    private int bodyY() { return sideY(); }
    private int bodyBottom() { return panelY() + panelH() - FOOT_H - 8; }

    private void rounded(DrawContext ctx, int x1, int y1, int x2, int y2, int radius, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r == 0) { ctx.fill(x1, y1, x2, y2, color); return; }
        ctx.fill(x1 + r, y1, x2 - r, y2, color);
        for (int y = 0; y < r; y++) {
            int inset = (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - (r - y - 0.5) * (r - y - 0.5))));
            ctx.fill(x1 + inset, y1 + y, x2 - inset, y1 + y + 1, color);
            ctx.fill(x1 + inset, y2 - y - 1, x2 - inset, y2 - y, color);
        }
    }

    private void gradient(DrawContext ctx, int x1, int y1, int x2, int y2, int top, int bottom) {
        int h = Math.max(1, y2 - y1);
        for (int y = 0; y < h; y++) ctx.fill(x1, y1 + y, x2, y1 + y + 1, mix(top, bottom, y / (float) h));
    }

    private int mix(int a, int b, float t) {
        int aa=(a>>>24)&255, ar=(a>>>16)&255, ag=(a>>>8)&255, ab=a&255;
        int ba=(b>>>24)&255, br=(b>>>16)&255, bg=(b>>>8)&255, bb=b&255;
        return (Math.round(aa+(ba-aa)*t)<<24) | (Math.round(ar+(br-ar)*t)<<16)
            | (Math.round(ag+(bg-ag)*t)<<8) | Math.round(ab+(bb-ab)*t);
    }

    private void text(DrawContext ctx, String value, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(value), x, y, color);
    }
    private void centered(DrawContext ctx, String value, int x, int y, int w, int color) {
        text(ctx, value, x + (w - textRenderer.getWidth(value)) / 2, y, color);
    }
    private String trim(String value, int maxWidth) {
        while (!value.isEmpty() && textRenderer.getWidth(value) > maxWidth)
            value = value.substring(0, value.length() - 1);
        return value.isEmpty() ? value : value + "…";
    }

    private List<Module> visibleModules() {
        if (categories.isEmpty()) return List.of();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        return Modules.get().getGroup(categories.get(selectedCategory)).stream()
            .filter(m -> (!enabledOnly || m.isActive()))
            .filter(m -> q.isEmpty() || m.title.toLowerCase(Locale.ROOT).contains(q)
                || m.name.toLowerCase(Locale.ROOT).contains(q)
                || m.description.toLowerCase(Locale.ROOT).contains(q))
            .toList();
    }

    private void selectCategory(int index) {
        if (index < 0 || index >= categories.size()) return;
        selectedCategory = index;
        List<Module> list = visibleModules();
        selectedModule = list.isEmpty() ? null : list.get(0);
    }

    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = panelX(), y = panelY(), w = panelW(), h = panelH();
        rounded(ctx, x + 5, y + 7, x + w + 5, y + h + 7, 13, 0x77000000);
        rounded(ctx, x, y, x + w, y + h, 13, EDGE);
        rounded(ctx, x + 1, y + 1, x + w - 1, y + h - 1, 12, BG);
        rounded(ctx, x + 1, y + 1, x + w - 1, y + TOP_H, 12, PANEL);
        ctx.fill(x + 1, y + TOP_H, x + w - 1, y + TOP_H + 1, EDGE);

        // Header / branding
        rounded(ctx, x + 14, y + 13, x + 32, y + 31, 9, ACCENT);
        rounded(ctx, x + 19, y + 18, x + 27, y + 26, 4, 0xFF25282D);
        text(ctx, "FIZZY", x + 42, y + 12, TEXT);
        text(ctx, "CLIENT WORKSPACE", x + 42, y + 25, MUTED);
        String[] tabs = {"Modules", "HUD", "Appearance"};
        int tabX = x + w - 235;
        for (String tab : tabs) {
            int tw = tab.equals("Appearance") ? 88 : 66;
            boolean active = topTab.equals(tab);
            if (active) rounded(ctx, tabX, y + 10, tabX + tw, y + 38, 7, ACTIVE);
            centered(ctx, tab, tabX, y + 19, tw, active ? TEXT : MUTED);
            if (active) rounded(ctx, tabX + 12, y + 37, tabX + tw - 12, y + 39, 1, ACCENT);
            tabX += tw + 8;
        }

        drawSidebar(ctx, mouseX, mouseY);
        if (topTab.equals("Modules")) {
            drawModuleDesk(ctx, mouseX, mouseY);
            drawSettings(ctx);
        } else if (topTab.equals("HUD")) {
            drawSimplePage(ctx, "HUD", "Configure the on-screen module list and status overlays.");
        } else {
            drawSimplePage(ctx, "Appearance", "Customize the Fizzy Client interface.");
        }

        text(ctx, "FIZZY  /  YOUR WORKSPACE", x + 16, y + h - 15, MUTED);
        text(ctx, Modules.get().getAll().stream().filter(Module::isActive).count() + " modules active",
            x + w - 118, y + h - 15, MUTED);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawSidebar(DrawContext ctx, int mx, int my) {
        int x = sideX(), y = sideY(), h = sideH();
        rounded(ctx, x, y, x + SIDEBAR_W, y + h, 9, PANEL);
        text(ctx, "WORKSPACE", x + 10, y + 11, 0xFFB8D8E8);
        int cy = y + 31;
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            boolean active = i == selectedCategory;
            int rowY = cy + i * 29;
            if (active) {
                rounded(ctx, x + 5, rowY, x + SIDEBAR_W - 5, rowY + 25, 6, ACTIVE);
                rounded(ctx, x + 5, rowY + 5, x + 7, rowY + 20, 1, ACCENT);
            }
            text(ctx, (active ? "✦ " : "· ") + c.name, x + 13, rowY + 8, active ? TEXT : MUTED);
        }
        rounded(ctx, x + 10, y + h - 37, x + 16, y + h - 31, 3, ACCENT);
        text(ctx, "Enabled only", x + 23, y + h - 37, enabledOnly ? TEXT : MUTED);
    }

    private void drawModuleDesk(DrawContext ctx, int mx, int my) {
        int x = centerX(), y = bodyY(), w = centerW();
        rounded(ctx, x, y, x + w, y + 78, 9, PANEL_2);
        text(ctx, "YOUR MODULE DESK", x + 12, y + 11, ACCENT);
        text(ctx, selectedModule == null ? "Modules" : selectedModule.title, x + 12, y + 29, TEXT);
        text(ctx, "Select a module to configure. Left click toggles it.", x + 12, y + 48, MUTED);

        int sy = y + 88;
        rounded(ctx, x, sy, x + w, sy + 28, 6, PANEL_2);
        text(ctx, searchQuery.isEmpty() ? "⌕  Search modules..." : "⌕  " + searchQuery,
            x + 10, sy + 9, searchQuery.isEmpty() ? MUTED : TEXT);
        int listTop = sy + 37;
        int bottom = bodyBottom();
        ctx.enableScissor(x, listTop, x + w, bottom);
        int cy = listTop - scroll.getOrDefault(categories.get(selectedCategory), 0);
        for (Module module : visibleModules()) {
            boolean chosen = module == selectedModule;
            rounded(ctx, x, cy, x + w, cy + 53, 7, chosen ? 0xFF33372F : PANEL);
            if (chosen) rounded(ctx, x, cy + 7, x + 2, cy + 46, 1, ACCENT);
            text(ctx, trim(module.title, w - 70), x + 11, cy + 12, TEXT);
            text(ctx, module.isActive() ? "ACTIVE" : "INACTIVE", x + 11, cy + 31,
                module.isActive() ? ACCENT : MUTED);
            rounded(ctx, x + w - 33, cy + 18, x + w - 13, cy + 31, 7,
                module.isActive() ? ACCENT : 0xFF454A53);
            rounded(ctx, x + (module.isActive() ? w - 23 : w - 32), cy + 20, x + (module.isActive() ? w - 15 : w - 24), cy + 28, 4, 0xFFF5F5F5);
            cy += 61;
        }
        ctx.disableScissor();
    }

    private void drawSettings(DrawContext ctx) {
        int x = settingsX(), y = bodyY(), w = panelX() + panelW() - 8 - x, bottom = bodyBottom();
        rounded(ctx, x, y, x + w, bottom, 9, PANEL);
        text(ctx, "MODULE SETTINGS", x + 12, y + 14, MUTED);
        if (selectedModule == null) {
            text(ctx, "Select a module", x + 12, y + 42, TEXT);
            return;
        }
        int cy = y + 37;
        text(ctx, trim(selectedModule.title, w - 24), x + 12, cy, TEXT); cy += 20;
        text(ctx, " " + trim(selectedModule.category.name, w - 24), x + 12, cy, MUTED); cy += 22;
        String desc = selectedModule.description;
        for (String line : wrap(desc, w - 24)) {
            text(ctx, line, x + 12, cy, MUTED); cy += 12;
            if (cy > bottom - 18) return;
        }
        cy += 9;
        ctx.fill(x + 10, cy, x + w - 10, cy + 1, EDGE); cy += 10;
        for (SettingGroup group : selectedModule.settings) {
            for (Setting<?> setting : group) {
                if (!setting.isVisible() || cy > bottom - 22) continue;
                text(ctx, trim(setting.title, w - 65), x + 12, cy + 4, TEXT);
                if (setting instanceof BoolSetting bool) {
                    rounded(ctx, x + w - 37, cy, x + w - 12, cy + 16, 8, bool.get() ? ACCENT : 0xFF454A53);
                    int knobX = bool.get() ? x + w - 22 : x + w - 35;
                    rounded(ctx, knobX, cy + 2, knobX + 12, cy + 14, 6, 0xFFF4F4F4);
                } else {
                    text(ctx, trim(String.valueOf(setting.get()), 65), x + w - 67, cy + 4, MUTED);
                    if (setting instanceof IntSetting || setting instanceof DoubleSetting) {
                        rounded(ctx, x + 12, cy + 23, x + w - 12, cy + 26, 2, 0xFF555B65);
                        rounded(ctx, x + 12, cy + 23, x + 12 + 8, cy + 26, 2, ACCENT);
                        cy += 12;
                    }
                }
                cy += 29;
            }
        }
    }

    private List<String> wrap(String value, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.getWidth(candidate) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString()); line = new StringBuilder(word);
            } else line = new StringBuilder(candidate);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private void drawSimplePage(DrawContext ctx, String title, String description) {
        int x = centerX(), y = bodyY(), w = panelX() + panelW() - 8 - x;
        rounded(ctx, x, y, x + w, bodyBottom(), 9, PANEL);
        text(ctx, title, x + 18, y + 20, TEXT);
        text(ctx, description, x + 18, y + 43, MUTED);
        if (title.equals("HUD")) {
            text(ctx, "The compact Active Modules list is shown in-game when enabled.", x + 18, y + 82, TEXT);
            text(ctx, "Use the module settings to control individual HUD options.", x + 18, y + 102, MUTED);
        }
    }

    private int categoryAt(double mx, double my) {
        int x = sideX(), y = sideY() + 31;
        if (mx < x || mx > x + SIDEBAR_W || my < y) return -1;
        int i = (int) ((my - y) / 29);
        return i >= 0 && i < categories.size() && my <= y + i * 29 + 25 ? i : -1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        int x = panelX(), y = panelY();

        // Header tabs
        int tx = x + panelW() - 235, ty = y + 10;
        String[] tabs = {"Modules", "HUD", "Appearance"};
        for (String tab : tabs) {
            int tw = tab.equals("Appearance") ? 88 : 66;
            if (mx >= tx && mx <= tx + tw && my >= ty && my <= ty + 29) {
                topTab = tab; return true;
            }
            tx += tw + 8;
        }

        int ci = categoryAt(mx, my);
        if (ci >= 0) { selectCategory(ci); return true; }
        int sx = sideX(), sy = sideY(), sh = sideH();
        if (mx >= sx && mx <= sx + SIDEBAR_W && my >= sy + sh - 43 && my <= sy + sh - 22) {
            enabledOnly = !enabledOnly;
            List<Module> list = visibleModules();
            selectedModule = list.isEmpty() ? null : list.get(0);
            return true;
        }

        if (topTab.equals("Modules")) {
            int cx = centerX(), searchY = bodyY() + 88;
            if (mx >= cx && mx <= cx + centerW() && my >= searchY && my <= searchY + 28) {
                searchFocused = true; return true;
            }
            searchFocused = false;
            int listY = searchY + 37 - scroll.getOrDefault(categories.get(selectedCategory), 0);
            for (Module module : visibleModules()) {
                if (mx >= cx && mx <= cx + centerW() && my >= listY && my <= listY + 53) {
                    selectedModule = module;
                    if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
                    else if (button == GLFW_MOUSE_BUTTON_RIGHT) expanded.put(module, !expanded.getOrDefault(module, false));
                    return true;
                }
                listY += 61;
            }
            if (selectedModule != null && mx >= settingsX() && mx <= x + panelW() - 8) {
                int cy = bodyY() + 37 + 20 + 22;
                for (String line : wrap(selectedModule.description, SETTINGS_W - 30)) cy += 12;
                cy += 19;
                for (SettingGroup group : selectedModule.settings) for (Setting<?> setting : group) {
                    if (!setting.isVisible()) continue;
                    if (my >= cy && my <= cy + 29) {
                        if (setting instanceof BoolSetting bool) bool.set(!bool.get());
                        else if (setting instanceof EnumSetting<?> mode) {
                            List<String> opts = mode.getSuggestions();
                            if (!opts.isEmpty()) {
                                int current = opts.indexOf(mode.get().toString());
                                mode.parse(opts.get((current + 1) % opts.size()));
                            }
                        } else if (setting instanceof IntSetting || setting instanceof DoubleSetting) {
                            sliderSetting = setting; sliderX = settingsX() + 12; sliderW = SETTINGS_W - 36; updateSlider(mx);
                        }
                        return true;
                    }
                    cy += (setting instanceof IntSetting || setting instanceof DoubleSetting) ? 41 : 29;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (searchFocused) {
            if (input.key() == GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
            if (input.key() == GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.offsetByCodePoints(searchQuery.length(), -1));
                scroll.clear(); return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override public boolean charTyped(CharInput input) {
        if (!searchFocused) return super.charTyped(input);
        int cp = input.codepoint();
        if (Character.isValidCodePoint(cp) && !Character.isISOControl(cp)) { searchQuery += new String(Character.toChars(cp)); scroll.clear(); }
        return true;
    }

    private void updateSlider(double mx) {
        if (sliderSetting == null || sliderW <= 0) return;
        double ratio = Math.max(0, Math.min(1, (mx - sliderX) / sliderW));
        if (sliderSetting instanceof IntSetting s) sliderSetting.parse(Integer.toString((int)Math.round(s.sliderMin + (s.sliderMax - s.sliderMin) * ratio)));
        else if (sliderSetting instanceof DoubleSetting s) sliderSetting.parse(Double.toString(s.sliderMin + (s.sliderMax - s.sliderMin) * ratio));
    }

    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (sliderSetting != null) { updateSlider(click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }
    @Override public boolean mouseReleased(Click click) {
        if (sliderSetting != null) { sliderSetting = null; return true; }
        return super.mouseReleased(click);
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int cx = centerX(), top = bodyY() + 125;
        if (mx >= cx && mx <= cx + centerW() && my >= top && my <= bodyBottom() && !categories.isEmpty()) {
            Category c = categories.get(selectedCategory);
            int max = Math.max(0, visibleModules().size() * 61 - (bodyBottom() - top));
            scroll.put(c, Math.max(0, Math.min(max, scroll.getOrDefault(c, 0) - (int)(vertical * 38))));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }
    @Override public boolean shouldPause() { return false; }
}
