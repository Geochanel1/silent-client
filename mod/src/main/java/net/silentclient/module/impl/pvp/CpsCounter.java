package net.silentclient.module.impl.pvp;

import net.silentclient.module.Category;
import net.silentclient.module.HudModule;
import net.silentclient.setting.BooleanSetting;
import net.silentclient.util.ClickTracker;

public class CpsCounter extends HudModule {

    private final BooleanSetting showRight = add(new BooleanSetting("Show Right Click", false));

    public CpsCounter() {
        super("CPS Counter", "Shows your clicks per second", Category.PVP, 0.01f, 0.39f);
    }

    @Override public int getWidth() { return 64; }
    @Override public int getHeight() { return 16; }

    @Override
    public void render(boolean preview) {
        String text = showRight.get()
                ? ClickTracker.getLeftCps() + " | " + ClickTracker.getRightCps() + " CPS"
                : ClickTracker.getLeftCps() + " CPS";
        renderSimple(text);
    }
}
