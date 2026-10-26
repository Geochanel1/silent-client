package net.silentclient.module;

public enum Category {
    ALL("All"),
    PVP("PvP"),
    HUD("HUD"),
    VISUAL("Visual"),
    PERFORMANCE("Performance"),
    WORLD("World");

    public final String display;

    Category(String display) {
        this.display = display;
    }
}
