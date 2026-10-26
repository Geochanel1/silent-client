package net.silentclient.module.impl.pvp;

import net.minecraft.client.option.KeyBinding;
import net.silentclient.module.Category;
import net.silentclient.module.Module;

/** Πατάς μία φορά το sneak και μένεις σκυμμένος μέχρι να το ξαναπατήσεις. */
public class ToggleSneak extends Module {

    private boolean toggled;

    public ToggleSneak() {
        super("Toggle Sneak", "Press sneak once to keep sneaking", Category.PVP);
    }

    @Override
    public void onKeyPress(int key) {
        if (key == mc.options.sneakKey.getCode()) toggled = !toggled;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) return;
        if (toggled) KeyBinding.setKeyPressed(mc.options.sneakKey.getCode(), true);
    }

    @Override
    protected void onDisable() {
        toggled = false;
        KeyBinding.setKeyPressed(mc.options.sneakKey.getCode(), false);
    }
}
