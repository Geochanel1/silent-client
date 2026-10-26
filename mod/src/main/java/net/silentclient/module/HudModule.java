package net.silentclient.module;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.Window;
import net.silentclient.setting.BooleanSetting;
import net.silentclient.setting.ColorSetting;
import net.silentclient.setting.NumberSetting;
import net.silentclient.util.Theme;

/**
 * Ένα module που ζωγραφίζει κάτι στην οθόνη και μπορεί να μετακινηθεί
 * στον HUD Editor. Η θέση αποθηκεύεται ως ποσοστό της οθόνης (0..1),
 * ώστε να μένει σωστή σε κάθε ανάλυση.
 */
public abstract class HudModule extends Module {

    private float relX, relY;

    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    public final BooleanSetting background = add(new BooleanSetting("Background", true));
    public final BooleanSetting textShadow = add(new BooleanSetting("Text Shadow", true));
    public final ColorSetting textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));

    protected HudModule(String name, String description, Category category, float defaultX, float defaultY) {
        super(name, description, category);
        this.relX = defaultX;
        this.relY = defaultY;
    }

    /** Πλάτος σε pixels (χωρίς scale). */
    public abstract int getWidth();

    /** Ύψος σε pixels (χωρίς scale). */
    public abstract int getHeight();

    /** Ζωγραφίζει στο (0,0). Το {@code preview} είναι true μέσα στον HUD Editor. */
    public abstract void render(boolean preview);

    public void renderScaled(boolean preview) {
        float s = (float) (double) scale.get();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) getX(), (float) getY(), 0f);
        GlStateManager.scale(s, s, 1f);
        render(preview);
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ----- Βοηθητικά για τα παιδιά -----

    protected void drawBackground(int w, int h) {
        if (background.get()) DrawableHelper.fill(0, 0, w, h, Theme.HUD_BG);
    }

    protected void drawText(String text, float x, float y) {
        drawText(text, x, y, textColor.get());
    }

    protected void drawText(String text, float x, float y, int color) {
        if (textShadow.get()) mc.textRenderer.drawWithShadow(text, x, y, color);
        else mc.textRenderer.draw(text, (int) x, (int) y, color);
    }

    /** Απλό module με μία γραμμή κειμένου σε κουτάκι. */
    protected void renderSimple(String text) {
        int w = getWidth(), h = getHeight();
        drawBackground(w, h);
        drawText(text, (w - mc.textRenderer.getStringWidth(text)) / 2f, (h - 8) / 2f + 0.5f);
    }

    // ----- Θέση -----

    public int getX() {
        Window w = new Window(mc);
        return clamp((int) (relX * w.getWidth()), w.getWidth() - getScaledWidth());
    }

    public int getY() {
        Window w = new Window(mc);
        return clamp((int) (relY * w.getHeight()), w.getHeight() - getScaledHeight());
    }

    public void setPosition(int x, int y) {
        Window w = new Window(mc);
        relX = x / (float) w.getWidth();
        relY = y / (float) w.getHeight();
    }

    public int getScaledWidth() { return (int) (getWidth() * scale.get()); }
    public int getScaledHeight() { return (int) (getHeight() * scale.get()); }

    public float getRelX() { return relX; }
    public float getRelY() { return relY; }
    public void setRel(float x, float y) { relX = x; relY = y; }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(v, Math.max(0, max)));
    }
}
