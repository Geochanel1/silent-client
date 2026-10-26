package net.silentclient.module.impl.hud;

import net.minecraft.util.math.MathHelper;
import net.silentclient.module.Category;
import net.silentclient.module.HudModule;

public class Direction extends HudModule {

    private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public Direction() {
        super("Direction", "Shows which way you are facing", Category.HUD, 0.01f, 0.27f);
    }

    @Override public int getWidth() { return 58; }
    @Override public int getHeight() { return 16; }

    @Override
    public void render(boolean preview) {
        if (mc.player == null) return;
        float yaw = MathHelper.wrapDegrees(mc.player.yaw);
        int idx = MathHelper.floor(yaw * 8.0 / 360.0 + 0.5) & 7;
        renderSimple(NAMES[idx] + "  " + (int) yaw + "°");
    }
}
