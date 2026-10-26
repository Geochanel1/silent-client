package net.silentclient.module;

import net.silentclient.module.impl.hud.*;
import net.silentclient.module.impl.performance.DynamicFps;
import net.silentclient.module.impl.pvp.*;
import net.silentclient.module.impl.visual.Fullbright;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // HUD
        register(new FpsCounter());
        register(new PingCounter());
        register(new Coordinates());
        register(new Direction());
        register(new Clock());
        // PvP
        register(new CpsCounter());
        register(new Keystrokes());
        register(new ArmorStatus());
        register(new PotionEffects());
        register(new ToggleSprint());
        register(new ToggleSneak());
        // Visual
        register(new Fullbright());
        // Performance
        register(new DynamicFps());
    }

    private void register(Module m) {
        modules.add(m);
    }

    public List<Module> getModules() { return modules; }

    public List<Module> getModules(Category category, String search) {
        String q = search == null ? "" : search.toLowerCase().trim();
        return modules.stream()
                .filter(m -> category == Category.ALL || m.getCategory() == category)
                .filter(m -> q.isEmpty() || m.getName().toLowerCase().contains(q)
                        || m.getDescription().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public List<HudModule> getHudModules() {
        List<HudModule> out = new ArrayList<>();
        for (Module m : modules) if (m instanceof HudModule) out.add((HudModule) m);
        return out;
    }

    public Module get(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
