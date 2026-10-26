package net.silentclient.module.impl.hud;

import net.minecraft.client.network.PlayerListEntry;
import net.silentclient.module.Category;
import net.silentclient.module.HudModule;

public class PingCounter extends HudModule {

    public PingCounter() {
        super("Ping", "Shows your latency to the server", Category.HUD, 0.01f, 0.07f);
    }

    @Override public int getWidth() { return 58; }
    @Override public int getHeight() { return 16; }

    @Override
    public void render(boolean preview) {
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null && !mc.isInSingleplayer()) {
            PlayerListEntry info = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (info != null) ping = info.getLatency();
        }
        renderSimple(ping + " ms");
    }
}
