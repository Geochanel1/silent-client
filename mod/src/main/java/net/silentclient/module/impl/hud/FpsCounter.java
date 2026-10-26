package net.silentclient.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.silentclient.module.Category;
import net.silentclient.module.HudModule;

public class FpsCounter extends HudModule {

    public FpsCounter() {
        super("FPS Counter", "Shows your frames per second", Category.HUD, 0.01f, 0.01f);
    }

    @Override public int getWidth() { return 58; }
    @Override public int getHeight() { return 16; }

    @Override
    public void render(boolean preview) {
        renderSimple(MinecraftClient.getCurrentFps() + " FPS");
    }
}
