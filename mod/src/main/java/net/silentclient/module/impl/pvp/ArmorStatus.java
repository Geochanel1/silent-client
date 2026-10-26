package net.silentclient.module.impl.pvp;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.silentclient.module.Category;
import net.silentclient.module.HudModule;
import net.silentclient.setting.BooleanSetting;

public class ArmorStatus extends HudModule {

    private final BooleanSetting showHeld = add(new BooleanSetting("Show Held Item", true));
    private final BooleanSetting percent = add(new BooleanSetting("Show Percent", false));
    private ItemStack[] preview;

    public ArmorStatus() {
        super("Armor Status", "Shows your armor and its durability", Category.PVP, 0.90f, 0.40f);
        background.set(false);
    }

    @Override public int getWidth() { return 52; }
    @Override public int getHeight() { return (showHeld.get() ? 5 : 4) * 17; }

    @Override
    public void render(boolean isPreview) {
        drawBackground(getWidth(), getHeight());
        if (mc.player == null) return;

        ItemStack[] armor = mc.player.inventory.armor;
        boolean any = false;
        for (ItemStack s : armor) if (s != null) any = true;
        if (!any && isPreview) armor = previewArmor();

        int y = 0;
        for (int i = 3; i >= 0; i--) {
            drawItem(armor[i], y);
            y += 17;
        }
        if (showHeld.get()) drawItem(mc.player.inventory.getMainHandStack(), y);
    }

    private ItemStack[] previewArmor() {
        if (preview == null) {
            preview = new ItemStack[]{
                    new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_LEGGINGS),
                    new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_HELMET)
            };
        }
        return preview;
    }

    private void drawItem(ItemStack stack, int y) {
        if (stack == null) return;
        GlStateManager.pushMatrix();
        DiffuseLighting.enableNormally();
        mc.getItemRenderer().renderInGuiWithOverrides(stack, 0, y);
        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, stack, 0, y);
        DiffuseLighting.disable();
        GlStateManager.popMatrix();

        if (stack.isDamageable()) {
            int left = stack.getMaxDamage() - stack.getDamage();
            String text = percent.get() ? (left * 100 / stack.getMaxDamage()) + "%" : String.valueOf(left);
            float pct = left / (float) stack.getMaxDamage();
            int color = pct > 0.6f ? 0xFF55FF55 : pct > 0.25f ? 0xFFFFAA00 : 0xFFFF5555;
            drawText(text, 19, y + 4, color);
        }
    }
}
