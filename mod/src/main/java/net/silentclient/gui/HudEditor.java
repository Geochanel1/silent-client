package net.silentclient.gui;

import net.minecraft.client.gui.screen.Screen;
import net.silentclient.SilentClient;
import net.silentclient.module.HudModule;
import net.silentclient.util.RenderUtil;
import net.silentclient.util.Theme;
import org.lwjgl.input.Keyboard;

import java.util.List;

/** Σύρε τα HUD mods με το ποντίκι για να τα βάλεις όπου θες. */
public class HudEditor extends Screen {

    private static final int SNAP = 4;

    private final Screen parent;
    private HudModule dragging;
    private int offX, offY;

    public HudEditor(Screen parent) {
        this.parent = parent;
    }

    @Override
    public void render(int mx, int my, float partialTicks) {
        RenderUtil.fillRect(0, 0, width, height, 0x50000000);

        if (dragging != null) {
            int nx = mx - offX, ny = my - offY;
            // κούμπωμα στο κέντρο της οθόνης
            int cx = width / 2 - dragging.getScaledWidth() / 2;
            if (Math.abs(nx - cx) < SNAP) {
                nx = cx;
                RenderUtil.fillRect(width / 2, 0, width / 2 + 1, height, Theme.ACCENT_SOFT);
            }
            dragging.setPosition(nx, ny);
        }

        for (HudModule m : SilentClient.INSTANCE.getModuleManager().getHudModules()) {
            if (!m.isEnabled()) continue;
            m.renderScaled(true);
            boolean hov = RenderUtil.hovered(mx, my, m.getX(), m.getY(), m.getScaledWidth(), m.getScaledHeight());
            RenderUtil.outline(m.getX() - 1, m.getY() - 1, m.getScaledWidth() + 2, m.getScaledHeight() + 2,
                    m == dragging || hov ? Theme.ACCENT_SOFT : 0x80FFFFFF);
        }

        String hint = "Drag mods to move them  -  ESC to go back";
        textRenderer.drawWithShadow(hint, width / 2f - textRenderer.getStringWidth(hint) / 2f, height - 14, Theme.TEXT_DIM);
    }

    @Override
    public void mouseClicked(int mx, int my, int button) {
        if (button != 0) return;
        // από πάνω προς τα κάτω, για να πιάνουμε το mod που φαίνεται πρώτο
        List<HudModule> mods = SilentClient.INSTANCE.getModuleManager().getHudModules();
        for (int i = mods.size() - 1; i >= 0; i--) {
            HudModule m = mods.get(i);
            if (m.isEnabled() && RenderUtil.hovered(mx, my, m.getX(), m.getY(), m.getScaledWidth(), m.getScaledHeight())) {
                dragging = m;
                offX = mx - m.getX();
                offY = my - m.getY();
                return;
            }
        }
    }

    @Override
    public void mouseReleased(int mx, int my, int state) {
        dragging = null;
    }

    @Override
    public void keyPressed(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) client.setScreen(parent);
    }

    @Override
    public void removed() {
        SilentClient.INSTANCE.getConfigManager().save(SilentClient.INSTANCE.getModuleManager());
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
