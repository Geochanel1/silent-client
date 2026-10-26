package net.silentclient.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.Window;
import net.silentclient.SilentClient;
import net.silentclient.module.Category;
import net.silentclient.module.Module;
import net.silentclient.setting.BooleanSetting;
import net.silentclient.setting.ColorSetting;
import net.silentclient.setting.NumberSetting;
import net.silentclient.setting.Setting;
import net.silentclient.util.RenderUtil;
import net.silentclient.util.Theme;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.List;

/** Το κύριο μενού του Silent Client (ανοίγει με Right Shift). */
public class ClickGui extends Screen {

    private static final int W = 440, H = 270;
    private static final int SIDEBAR = 96;
    private static final int CARD_W = 104, CARD_H = 52, GAP = 6, COLS = 3;

    private static Category selected = Category.ALL;
    private TextFieldWidget search;
    private Module editing;          // όταν δεν είναι null δείχνουμε τις ρυθμίσεις του
    private NumberSetting dragging;
    private boolean waitingKey;
    private int scroll;

    private int left, top;

    @Override
    public void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        Keyboard.enableRepeatEvents(true);
        search = new TextFieldWidget(0, textRenderer, left + W - 128, top + 12, 116, 14);
        search.setMaxLength(32);
        search.setHasBorder(false);
        Mouse.getDWheel(); // μηδενίζει ό,τι scroll έγινε πριν ανοίξει το μενού
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
        SilentClient.INSTANCE.getConfigManager().save(SilentClient.INSTANCE.getModuleManager());
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(int mx, int my, float partialTicks) {
        RenderUtil.fillRect(0, 0, width, height, Theme.OVERLAY);

        // Κύριο panel
        RenderUtil.roundedRect(left, top, W, H, Theme.PANEL);
        RenderUtil.outline(left, top, W, H, Theme.BORDER);

        // Sidebar
        RenderUtil.fillRect(left + 1, top + 1, left + SIDEBAR, top + H - 1, Theme.PANEL_LIGHT);
        GlStateManager.pushMatrix();
        GlStateManager.translate(left + 12, top + 12, 0);
        GlStateManager.scale(1.3f, 1.3f, 1);
        textRenderer.drawWithShadow("SILENT", 0, 0, Theme.ACCENT_SOFT);
        GlStateManager.popMatrix();
        textRenderer.draw("CLIENT", left + 12, top + 25, Theme.TEXT_DIM);

        int cy = top + 46;
        for (Category c : Category.values()) {
            boolean sel = c == selected && editing == null;
            boolean hov = RenderUtil.hovered(mx, my, left + 6, cy, SIDEBAR - 12, 18);
            if (sel) RenderUtil.roundedRect(left + 6, cy, SIDEBAR - 12, 18, Theme.ACCENT);
            else if (hov) RenderUtil.roundedRect(left + 6, cy, SIDEBAR - 12, 18, Theme.CARD_HOVER);
            textRenderer.draw(c.display, left + 14, cy + 5, sel ? 0xFFFFFFFF : Theme.TEXT);
            cy += 21;
        }

        // Κουμπί HUD Editor
        int by = top + H - 30;
        boolean hudHov = RenderUtil.hovered(mx, my, left + 6, by, SIDEBAR - 12, 20);
        RenderUtil.roundedRect(left + 6, by, SIDEBAR - 12, 20, hudHov ? Theme.ACCENT_SOFT : Theme.ACCENT);
        drawCentered("Edit HUD", left + SIDEBAR / 2, by + 6, 0xFFFFFFFF);

        if (editing == null) drawModules(mx, my);
        else drawSettings(mx, my);

    }

    private void drawModules(int mx, int my) {
        int cx = left + SIDEBAR + 12;
        textRenderer.draw(selected.display + " Mods", cx, top + 15, Theme.TEXT);

        // search box
        RenderUtil.roundedRect(left + W - 132, top + 9, 122, 16, Theme.CARD);
        if (search.getText().isEmpty() && !search.isFocused()) {
            textRenderer.draw("Search...", left + W - 128, top + 13, Theme.TEXT_DIM);
        }
        search.render();

        int wheel = Mouse.getDWheel();
        if (wheel != 0) scroll += wheel > 0 ? -20 : 20;

        List<Module> list = SilentClient.INSTANCE.getModuleManager().getModules(selected, search.getText());
        int areaTop = top + 34;
        int areaBottom = top + H - 8;
        clampScroll(list.size());

        startScissor(left + SIDEBAR, areaTop, W - SIDEBAR, areaBottom - areaTop);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int x = cx + (i % COLS) * (CARD_W + GAP);
            int y = areaTop + (i / COLS) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < areaTop || y > areaBottom) continue;
            boolean hov = RenderUtil.hovered(mx, my, x, y, CARD_W, CARD_H) && my >= areaTop && my <= areaBottom;

            RenderUtil.roundedRect(x, y, CARD_W, CARD_H, hov ? Theme.CARD_HOVER : Theme.CARD);
            if (m.isEnabled()) RenderUtil.fillRect(x, y + 3, x + 2, y + CARD_H - 3, Theme.ACCENT);
            textRenderer.draw(m.getName(), x + 7, y + 7, Theme.TEXT);
            textRenderer.draw(trim(m.getDescription(), CARD_W - 14), x + 7, y + 19, Theme.TEXT_DIM);

            // toggle
            drawSwitch(x + 7, y + CARD_H - 15, m.isEnabled());
            // options
            String opt = "Options";
            int ow = textRenderer.getStringWidth(opt);
            boolean optHov = hov && RenderUtil.hovered(mx, my, x + CARD_W - ow - 9, y + CARD_H - 16, ow + 4, 12);
            textRenderer.draw(opt, x + CARD_W - ow - 7, y + CARD_H - 13, optHov ? Theme.ACCENT_SOFT : Theme.TEXT_DIM);
        }
        endScissor();
    }

    private void drawSettings(int mx, int my) {
        int cx = left + SIDEBAR + 12;
        boolean backHov = RenderUtil.hovered(mx, my, cx, top + 10, 40, 14);
        textRenderer.draw("< Back", cx, top + 13, backHov ? Theme.ACCENT_SOFT : Theme.TEXT_DIM);
        textRenderer.drawWithShadow(editing.getName(), cx + 48, top + 13, Theme.TEXT);

        int rowW = W - SIDEBAR - 24;
        int y = top + 34;

        // Enabled
        y = drawRowBase("Enabled", cx, y, rowW, mx, my);
        drawSwitch(cx + rowW - 26, y - 17, editing.isEnabled());

        // Keybind
        y = drawRowBase("Keybind", cx, y, rowW, mx, my);
        String key = waitingKey ? "Press a key..." :
                (editing.getKeybind() == Keyboard.KEY_NONE ? "None" : Keyboard.getKeyName(editing.getKeybind()));
        int kw = textRenderer.getStringWidth(key) + 10;
        RenderUtil.roundedRect(cx + rowW - kw - 6, y - 19, kw, 14, waitingKey ? Theme.ACCENT : Theme.CARD_HOVER);
        textRenderer.draw(key, cx + rowW - kw - 1, y - 16, Theme.TEXT);

        for (Setting<?> s : editing.getSettings()) {
            y = drawRowBase(s.getName(), cx, y, rowW, mx, my);
            int ry = y - 17;
            if (s instanceof BooleanSetting) {
                drawSwitch(cx + rowW - 26, ry, ((BooleanSetting) s).get());
            } else if (s instanceof NumberSetting) {
                NumberSetting n = (NumberSetting) s;
                int sx = cx + rowW - 126, sw = 100;
                if (dragging == n) {
                    double pct = Math.max(0, Math.min(1, (mx - sx) / (double) sw));
                    n.set(n.getMin() + pct * (n.getMax() - n.getMin()));
                }
                double pct = (n.get() - n.getMin()) / (n.getMax() - n.getMin());
                RenderUtil.fillRect(sx, ry + 4, sx + sw, ry + 6, Theme.OFF);
                RenderUtil.fillRect(sx, ry + 4, sx + (int) (sw * pct), ry + 6, Theme.ACCENT);
                int kx = sx + (int) (sw * pct);
                RenderUtil.roundedRect(kx - 3, ry + 1, 6, 8, 0xFFFFFFFF);
                textRenderer.draw(n.format(), sx + sw + 6, ry + 1, Theme.TEXT_DIM);
            } else if (s instanceof ColorSetting) {
                int c = ((ColorSetting) s).get();
                RenderUtil.roundedRect(cx + rowW - 26, ry, 20, 10, c);
                RenderUtil.outline(cx + rowW - 26, ry, 20, 10, Theme.BORDER);
            }
        }
    }

    private int drawRowBase(String label, int x, int y, int w, int mx, int my) {
        boolean hov = RenderUtil.hovered(mx, my, x, y, w, 22);
        RenderUtil.roundedRect(x, y, w, 22, hov ? Theme.CARD_HOVER : Theme.CARD);
        textRenderer.draw(label, x + 8, y + 7, Theme.TEXT);
        return y + 25;
    }

    private void drawSwitch(int x, int y, boolean on) {
        RenderUtil.roundedRect(x, y, 20, 10, on ? Theme.ON : Theme.OFF);
        RenderUtil.roundedRect(on ? x + 11 : x + 1, y + 1, 8, 8, 0xFFFFFFFF);
    }

    // ---------------------------------------------------------------- input

    @Override
    public void mouseClicked(int mx, int my, int button) {

        // Κατηγορίες
        int cy = top + 46;
        for (Category c : Category.values()) {
            if (RenderUtil.hovered(mx, my, left + 6, cy, SIDEBAR - 12, 18)) {
                selected = c;
                editing = null;
                scroll = 0;
                return;
            }
            cy += 21;
        }
        if (RenderUtil.hovered(mx, my, left + 6, top + H - 30, SIDEBAR - 12, 20)) {
            client.setScreen(new HudEditor(this));
            return;
        }

        if (editing == null) clickModules(mx, my, button);
        else clickSettings(mx, my);
    }

    private void clickModules(int mx, int my, int button) {
        search.mouseClicked(mx, my, button);
        List<Module> list = SilentClient.INSTANCE.getModuleManager().getModules(selected, search.getText());
        int cx = left + SIDEBAR + 12;
        int areaTop = top + 34, areaBottom = top + H - 8;
        if (my < areaTop || my > areaBottom) return;
        for (int i = 0; i < list.size(); i++) {
            int x = cx + (i % COLS) * (CARD_W + GAP);
            int y = areaTop + (i / COLS) * (CARD_H + GAP) - scroll;
            if (!RenderUtil.hovered(mx, my, x, y, CARD_W, CARD_H)) continue;
            Module m = list.get(i);
            int ow = textRenderer.getStringWidth("Options");
            boolean optClick = RenderUtil.hovered(mx, my, x + CARD_W - ow - 9, y + CARD_H - 16, ow + 4, 12);
            if (button == 1 || optClick) {
                editing = m;
                waitingKey = false;
            } else {
                m.toggle();
            }
            return;
        }
    }

    private void clickSettings(int mx, int my) {
        int cx = left + SIDEBAR + 12;
        int rowW = W - SIDEBAR - 24;
        if (RenderUtil.hovered(mx, my, cx, top + 10, 40, 14)) {
            editing = null;
            waitingKey = false;
            return;
        }
        int y = top + 34;
        if (RenderUtil.hovered(mx, my, cx, y, rowW, 22)) { editing.toggle(); return; }
        y += 25;
        if (RenderUtil.hovered(mx, my, cx, y, rowW, 22)) { waitingKey = true; return; }
        y += 25;
        for (Setting<?> s : editing.getSettings()) {
            if (RenderUtil.hovered(mx, my, cx, y, rowW, 22)) {
                if (s instanceof BooleanSetting) ((BooleanSetting) s).toggle();
                else if (s instanceof ColorSetting) ((ColorSetting) s).next();
                else if (s instanceof NumberSetting) dragging = (NumberSetting) s;
                return;
            }
            y += 25;
        }
    }

    @Override
    public void mouseReleased(int mx, int my, int state) {
        dragging = null;
    }

    @Override
    public void keyPressed(char c, int key) {
        if (waitingKey && editing != null) {
            editing.setKeybind(key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_BACK ? Keyboard.KEY_NONE : key);
            waitingKey = false;
            return;
        }
        if (editing == null && search.isFocused()) {
            if (key == Keyboard.KEY_ESCAPE) { search.setFocused(false); return; }
            search.keyPressed(c, key);
            scroll = 0;
            return;
        }
        if (key == Keyboard.KEY_RSHIFT || key == Keyboard.KEY_ESCAPE) {
            client.setScreen(null);
        }
    }

    @Override
    public void tick() {
        search.tick();
    }

    // ---------------------------------------------------------------- helpers

    private void clampScroll(int count) {
        int rows = (count + COLS - 1) / COLS;
        int content = rows * (CARD_H + GAP);
        int view = H - 42;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, content - view)));
    }

    private String trim(String s, int maxW) {
        if (textRenderer.getStringWidth(s) <= maxW) return s;
        return textRenderer.trimToWidth(s, maxW - textRenderer.getStringWidth("...")) + "...";
    }

    private void drawCentered(String s, int x, int y, int color) {
        textRenderer.draw(s, x - textRenderer.getStringWidth(s) / 2, y, color);
    }

    private void startScissor(int x, int y, int w, int h) {
        Window sr = new Window(client);
        int f = sr.getScaleFactor();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(x * f, client.height - (y + h) * f, w * f, h * f);
    }

    private void endScissor() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }
}
