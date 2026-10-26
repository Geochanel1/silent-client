package net.silentclient.module.impl.pvp;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.silentclient.module.Category;
import net.silentclient.module.HudModule;

import java.util.ArrayList;
import java.util.List;

public class PotionEffects extends HudModule {

    private static final Identifier INVENTORY = new Identifier("textures/gui/container/inventory.png");
    private static final DrawableHelper GUI = new DrawableHelper();
    private static final int ROW = 22;

    public PotionEffects() {
        super("Potion Effects", "Shows your active potion effects", Category.PVP, 0.01f, 0.50f);
        background.set(false);
    }

    @Override public int getWidth() { return 110; }
    @Override public int getHeight() { return Math.max(1, effects(true).size()) * ROW; }

    private List<StatusEffectInstance> effects(boolean preview) {
        List<StatusEffectInstance> list = new ArrayList<>();
        if (mc.player != null) {
            for (Object o : mc.player.getStatusEffectInstances()) {
                if (o instanceof StatusEffectInstance) list.add((StatusEffectInstance) o);
            }
        }
        if (list.isEmpty() && preview) {
            list.add(new StatusEffectInstance(StatusEffect.SPEED.id, 20 * 90, 1));
            list.add(new StatusEffectInstance(StatusEffect.FIRE_RESISTANCE.id, 20 * 180, 0));
        }
        return list;
    }

    @Override
    public void render(boolean preview) {
        List<StatusEffectInstance> list = effects(preview);
        drawBackground(getWidth(), Math.max(1, list.size()) * ROW);
        int y = 0;
        for (StatusEffectInstance e : list) {
            int id = e.getEffectId();
            if (id < 0 || id >= StatusEffect.STATUS_EFFECTS.length) continue;
            StatusEffect p = StatusEffect.STATUS_EFFECTS[id];
            if (p == null) continue;
            if (p.hasIcon()) {
                GlStateManager.color(1f, 1f, 1f, 1f);
                GlStateManager.enableBlend();
                mc.getTextureManager().bindTexture(INVENTORY);
                int idx = p.getIconLevel();
                GUI.drawTexture(0, y + 2, idx % 8 * 18, 198 + idx / 8 * 18, 18, 18);
            }
            String name = I18n.translate(p.getTranslationKey()) + " " + roman(e.getAmplifier() + 1);
            drawText(name, 22, y + 3);
            drawText(StatusEffect.getFormattedDuration(e), 22, y + 12, 0xFF4FC3FF);
            y += ROW;
        }
    }

    private static String roman(int n) {
        switch (n) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(n);
        }
    }
}
