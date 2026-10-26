package net.silentclient.module.impl.hud;

import net.minecraft.util.math.BlockPos;
import net.silentclient.module.Category;
import net.silentclient.module.HudModule;
import net.silentclient.setting.BooleanSetting;

public class Coordinates extends HudModule {

    private final BooleanSetting showBiome = add(new BooleanSetting("Show Biome", true));

    public Coordinates() {
        super("Coordinates", "Shows your X, Y, Z position", Category.HUD, 0.01f, 0.13f);
    }

    @Override public int getWidth() { return 92; }
    @Override public int getHeight() { return showBiome.get() ? 46 : 36; }

    @Override
    public void render(boolean preview) {
        drawBackground(getWidth(), getHeight());
        if (mc.player == null) return;
        drawText("X: " + (int) Math.floor(mc.player.x), 5, 4);
        drawText("Y: " + (int) Math.floor(mc.player.y), 5, 14);
        drawText("Z: " + (int) Math.floor(mc.player.z), 5, 24);
        if (showBiome.get() && mc.world != null) {
            BlockPos pos = new BlockPos(mc.player);
            String biome = mc.world.getBiome(pos).name;
            drawText(biome, 5, 34, 0xFF4FC3FF);
        }
    }
}
