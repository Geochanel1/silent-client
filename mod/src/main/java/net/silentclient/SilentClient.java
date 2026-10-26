package net.silentclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.silentclient.config.ConfigManager;
import net.silentclient.gui.ClickGui;
import net.silentclient.gui.HudEditor;
import net.silentclient.module.HudModule;
import net.silentclient.module.Module;
import net.silentclient.module.ModuleManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SilentClient implements ClientModInitializer {

    public static final String NAME = "Silent Client";
    public static final String VERSION = "0.1.0";
    public static final int MENU_KEY = Keyboard.KEY_RSHIFT;

    public static SilentClient INSTANCE;

    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private boolean titleSet;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        File dir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "silentclient");
        if (!dir.exists()) dir.mkdirs();

        moduleManager = new ModuleManager();
        configManager = new ConfigManager(new File(dir, "config.json"));
        configManager.load(moduleManager);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> configManager.save(moduleManager)));
    }

    /** Από το MinecraftClientMixin, κάθε tick. */
    public void onTick() {
        if (!titleSet) {
            Display.setTitle(NAME + " " + VERSION + " | 1.8.9");
            setWindowIcon();
            titleSet = true;
        }
        for (Module m : moduleManager.getModules()) {
            if (m.isEnabled()) m.onTick();
        }
    }

    /** Από το MinecraftClientMixin, όταν πατιέται πλήκτρο μέσα στο παιχνίδι. */
    public void onKey(int key, boolean down) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!down || key == Keyboard.KEY_NONE || mc.currentScreen != null) return;

        if (key == MENU_KEY) {
            mc.setScreen(new ClickGui());
            return;
        }
        for (Module m : moduleManager.getModules()) {
            if (m.getKeybind() == key) m.toggle();
            if (m.isEnabled()) m.onKeyPress(key);
        }
    }

    /** Από το InGameHudMixin, κάθε frame. */
    public void onRenderHud() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.debugEnabled || mc.currentScreen instanceof HudEditor) return;
        for (Module m : moduleManager.getModules()) {
            if (m.isEnabled() && m instanceof HudModule) ((HudModule) m).renderScaled(false);
        }
    }

    /** Βάζει το λογότυπο του Silent Client ως εικονίδιο του παραθύρου. */
    private void setWindowIcon() {
        try {
            Display.setIcon(new ByteBuffer[]{loadIcon("window_16.png"), loadIcon("window_32.png")});
        } catch (Exception e) {
            System.err.println("[Silent Client] Could not set window icon: " + e);
        }
    }

    private static ByteBuffer loadIcon(String name) throws Exception {
        try (InputStream in = SilentClient.class.getResourceAsStream("/assets/silentclient/" + name)) {
            BufferedImage img = ImageIO.read(in);
            int w = img.getWidth(), h = img.getHeight();
            ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    buf.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >> 24));
                }
            }
            buf.flip();
            return buf;
        }
    }

    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
}
