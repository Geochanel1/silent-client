package net.silentclient.module.impl.pvp;

import net.minecraft.client.option.KeyBinding;
import net.silentclient.util.RenderUtil;
import net.silentclient.module.Category;
import net.silentclient.module.HudModule;
import net.silentclient.setting.BooleanSetting;
import net.silentclient.setting.ColorSetting;
import net.silentclient.util.ClickTracker;
import net.silentclient.util.Theme;
import org.lwjgl.input.Keyboard;

public class Keystrokes extends HudModule {

    private static final int K = 22, G = 2;

    private final BooleanSetting showMouse = add(new BooleanSetting("Show Mouse", true));
    private final BooleanSetting showSpace = add(new BooleanSetting("Show Space", true));
    private final BooleanSetting showCps = add(new BooleanSetting("CPS on Mouse", true));
    private final ColorSetting pressedColor = add(new ColorSetting("Pressed Color", Theme.ACCENT));

    /** 0..1 για ομαλό fade σε κάθε πλήκτρο: W A S D LMB RMB SPACE */
    private final float[] anim = new float[7];
    private long lastFrame = System.currentTimeMillis();

    public Keystrokes() {
        super("Keystrokes", "Shows WASD, mouse and space presses", Category.PVP, 0.85f, 0.70f);
    }

    @Override
    public int getWidth() {
        return K * 3 + G * 2;
    }

    @Override
    public int getHeight() {
        int h = K * 2 + G;
        if (showMouse.get()) h += K + G;
        if (showSpace.get()) h += 12 + G;
        return h;
    }

    @Override
    public void render(boolean preview) {
        long now = System.currentTimeMillis();
        float dt = Math.min(1f, (now - lastFrame) / 80f);
        lastFrame = now;

        KeyBinding[] keys = {
                mc.options.forwardKey, mc.options.leftKey,
                mc.options.backKey, mc.options.rightKey,
                mc.options.attackKey, mc.options.useKey,
                mc.options.jumpKey
        };
        for (int i = 0; i < keys.length; i++) {
            float target = keys[i].isPressed() ? 1f : 0f;
            anim[i] += (target - anim[i]) * dt;
        }

        int y = 0;
        key(label(keys[0], "W"), K + G, y, K, K, anim[0]);
        y += K + G;
        key(label(keys[1], "A"), 0, y, K, K, anim[1]);
        key(label(keys[2], "S"), K + G, y, K, K, anim[2]);
        key(label(keys[3], "D"), (K + G) * 2, y, K, K, anim[3]);
        y += K + G;

        if (showMouse.get()) {
            int mw = (getWidth() - G) / 2;
            key(showCps.get() ? ClickTracker.getLeftCps() + " CPS" : "LMB", 0, y, mw, K, anim[4]);
            key(showCps.get() ? ClickTracker.getRightCps() + " CPS" : "RMB", mw + G, y, mw, K, anim[5]);
            y += K + G;
        }
        if (showSpace.get()) {
            float a = anim[6];
            RenderUtil.fillRect(0, y, getWidth(), y + 12, blend(Theme.HUD_BG, pressedColor.get(), a));
            RenderUtil.fillRect(getWidth() / 2 - 12, y + 5, getWidth() / 2 + 12, y + 6, blend(textColor.get(), 0xFFFFFFFF, a));
        }
    }

    private void key(String text, int x, int y, int w, int h, float a) {
        RenderUtil.fillRect(x, y, x + w, y + h, blend(Theme.HUD_BG, pressedColor.get(), a));
        int tw = mc.textRenderer.getStringWidth(text);
        drawText(text, x + (w - tw) / 2f, y + (h - 8) / 2f + 0.5f, blend(textColor.get(), 0xFFFFFFFF, a));
    }

    private String label(KeyBinding kb, String fallback) {
        int code = kb.getCode();
        if (code <= 0) return fallback;
        String name = Keyboard.getKeyName(code);
        return name == null || name.length() > 3 ? fallback : name;
    }

    private static int blend(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 0xFF) + ((((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t));
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int g = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
