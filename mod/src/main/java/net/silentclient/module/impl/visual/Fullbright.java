package net.silentclient.module.impl.visual;

import net.silentclient.module.Category;
import net.silentclient.module.Module;

/** Βλέπεις καθαρά και στο σκοτάδι (ανεβάζει το gamma). */
public class Fullbright extends Module {

    private float previousGamma = 1f;

    public Fullbright() {
        super("Fullbright", "See clearly in dark places", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        if (mc.options == null) return;
        previousGamma = Math.min(mc.options.gamma, 1f);
        mc.options.gamma = 100f;
    }

    @Override
    public void onTick() {
        if (mc.options != null) mc.options.gamma = 100f;
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) mc.options.gamma = previousGamma;
    }
}
