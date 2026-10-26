package net.silentclient.util;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Deque;

/** Μετράει τα κλικ του τελευταίου δευτερολέπτου (για CPS και Keystrokes). */
public final class ClickTracker {

    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    private ClickTracker() {}

    /** Από το KeyBindingMixin: κάθε φορά που πατιέται ένα πλήκτρο/κουμπί. */
    public static void onPress(int code) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        long now = System.currentTimeMillis();
        if (code == mc.options.attackKey.getCode()) LEFT.addLast(now);
        else if (code == mc.options.useKey.getCode()) RIGHT.addLast(now);
    }

    public static int getLeftCps() { return count(LEFT); }
    public static int getRightCps() { return count(RIGHT); }

    private static int count(Deque<Long> clicks) {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.pollFirst();
        return clicks.size();
    }
}
