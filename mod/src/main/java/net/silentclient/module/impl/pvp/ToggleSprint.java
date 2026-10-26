package net.silentclient.module.impl.pvp;

import net.minecraft.client.option.KeyBinding;
import net.silentclient.module.Category;
import net.silentclient.module.HudModule;
import net.silentclient.setting.BooleanSetting;

/** Πατάς μία φορά το πλήκτρο του sprint και τρέχεις συνέχεια. */
public class ToggleSprint extends HudModule {

    private final BooleanSetting showText = add(new BooleanSetting("Show Status Text", true));
    private boolean toggled = true;

    public ToggleSprint() {
        super("Toggle Sprint", "Press sprint once to keep sprinting", Category.PVP, 0.01f, 0.95f);
        background.set(false);
    }

    @Override public int getWidth() { return 110; }
    @Override public int getHeight() { return 10; }

    @Override
    public void onKeyPress(int key) {
        if (key == mc.options.sprintKey.getCode()) toggled = !toggled;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) return;
        if (toggled) KeyBinding.setKeyPressed(mc.options.sprintKey.getCode(), true);
    }

    @Override
    protected void onDisable() {
        KeyBinding.setKeyPressed(mc.options.sprintKey.getCode(), false);
    }

    @Override
    public void render(boolean preview) {
        if (!showText.get() && !preview) return;
        drawText(toggled ? "[Sprinting (Toggled)]" : "[Sprint off]", 0, 1);
    }
}
