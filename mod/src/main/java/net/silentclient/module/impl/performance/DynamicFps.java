package net.silentclient.module.impl.performance;

import net.silentclient.module.Category;
import net.silentclient.module.Module;
import net.silentclient.setting.NumberSetting;
import org.lwjgl.opengl.Display;

/** Ρίχνει τα FPS όταν το παιχνίδι δεν είναι στο προσκήνιο, για λιγότερη θερμότητα/CPU. */
public class DynamicFps extends Module {

    private final NumberSetting unfocusedFps = add(new NumberSetting("Unfocused FPS", 10, 1, 60, 1));
    private int savedLimit = -1;

    public DynamicFps() {
        super("Dynamic FPS", "Lower FPS while the game is in the background", Category.PERFORMANCE);
    }

    @Override
    public void onTick() {
        boolean active = Display.isActive();
        if (!active && savedLimit < 0) {
            savedLimit = mc.options.maxFramerate;
            mc.options.maxFramerate = (int) Math.round(unfocusedFps.get());
        } else if (active && savedLimit >= 0) {
            restore();
        }
    }

    @Override
    protected void onDisable() {
        restore();
    }

    private void restore() {
        if (savedLimit >= 0) {
            mc.options.maxFramerate = savedLimit;
            savedLimit = -1;
        }
    }
}
