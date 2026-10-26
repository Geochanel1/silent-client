package net.silentclient.util;

import net.minecraft.client.gui.DrawableHelper;

public final class RenderUtil {
    private RenderUtil() {}

    /** Γέμισμα από (x1,y1) μέχρι (x2,y2). */
    public static void fillRect(int x1, int y1, int x2, int y2, int color) {
        DrawableHelper.fill(x1, y1, x2, y2, color);
    }

    public static void rect(int x, int y, int w, int h, int color) {
        DrawableHelper.fill(x, y, x + w, y + h, color);
    }

    /** Ορθογώνιο με «κομμένες» γωνίες (φαίνεται στρογγυλεμένο σε μικρά μεγέθη). */
    public static void roundedRect(int x, int y, int w, int h, int color) {
        DrawableHelper.fill(x + 1, y, x + w - 1, y + h, color);
        DrawableHelper.fill(x, y + 1, x + 1, y + h - 1, color);
        DrawableHelper.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void outline(int x, int y, int w, int h, int color) {
        DrawableHelper.fill(x, y, x + w, y + 1, color);
        DrawableHelper.fill(x, y + h - 1, x + w, y + h, color);
        DrawableHelper.fill(x, y, x + 1, y + h, color);
        DrawableHelper.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static boolean hovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
