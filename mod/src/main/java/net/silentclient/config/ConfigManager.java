package net.silentclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.silentclient.module.HudModule;
import net.silentclient.module.Module;
import net.silentclient.module.ModuleManager;
import net.silentclient.setting.Setting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

/** Αποθηκεύει/φορτώνει όλα τα modules στο .minecraft/config/silentclient/config.json */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File file;

    public ConfigManager(File file) {
        this.file = file;
    }

    public void save(ModuleManager manager) {
        JsonObject root = new JsonObject();
        for (Module m : manager.getModules()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("keybind", m.getKeybind());
            if (m instanceof HudModule) {
                o.addProperty("x", ((HudModule) m).getRelX());
                o.addProperty("y", ((HudModule) m).getRelY());
            }
            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) settings.add(s.getName(), s.toJson());
            o.add("settings", settings);
            root.add(m.getName(), o);
        }
        try (Writer w = new FileWriter(file)) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            System.err.println("[Silent Client] Could not save config: " + e);
        }
    }

    public void load(ModuleManager manager) {
        if (!file.exists()) {
            enableDefaults(manager);
            return;
        }
        try (Reader r = new FileReader(file)) {
            JsonObject root = new JsonParser().parse(r).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                Module m = manager.get(e.getKey());
                if (m == null || !e.getValue().isJsonObject()) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                if (o.has("keybind")) m.setKeybind(o.get("keybind").getAsInt());
                if (m instanceof HudModule && o.has("x") && o.has("y")) {
                    ((HudModule) m).setRel(o.get("x").getAsFloat(), o.get("y").getAsFloat());
                }
                if (o.has("settings")) {
                    JsonObject settings = o.getAsJsonObject("settings");
                    for (Setting<?> s : m.getSettings()) {
                        if (settings.has(s.getName())) s.fromJson(settings.get(s.getName()));
                    }
                }
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
            }
        } catch (Exception ex) {
            System.err.println("[Silent Client] Could not load config, using defaults: " + ex);
            enableDefaults(manager);
        }
    }

    private void enableDefaults(ModuleManager manager) {
        for (String name : new String[]{"FPS Counter", "CPS Counter", "Keystrokes", "Coordinates"}) {
            Module m = manager.get(name);
            if (m != null) m.setEnabled(true);
        }
    }
}
