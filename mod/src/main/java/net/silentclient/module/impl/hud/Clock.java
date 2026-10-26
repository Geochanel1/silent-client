package net.silentclient.module.impl.hud;

import net.silentclient.module.Category;
import net.silentclient.module.HudModule;
import net.silentclient.setting.BooleanSetting;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Clock extends HudModule {

    private final BooleanSetting twentyFour = add(new BooleanSetting("24 Hour", true));
    private final BooleanSetting seconds = add(new BooleanSetting("Seconds", false));

    public Clock() {
        super("Clock", "Shows the real-world time", Category.HUD, 0.01f, 0.33f);
    }

    @Override public int getWidth() { return 64; }
    @Override public int getHeight() { return 16; }

    @Override
    public void render(boolean preview) {
        String pattern = (twentyFour.get() ? "HH:mm" : "h:mm") + (seconds.get() ? ":ss" : "") + (twentyFour.get() ? "" : " a");
        renderSimple(new SimpleDateFormat(pattern).format(new Date()));
    }
}
