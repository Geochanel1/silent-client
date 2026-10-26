package net.silentclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Χρώμα ARGB. Στο μενού αλλάζει με κλικ ανάμεσα σε έτοιμα χρώματα. */
public class ColorSetting extends Setting<Integer> {

    public static final int[] PALETTE = {
            0xFFFFFFFF, 0xFF4FC3FF, 0xFF1E7BFF, 0xFFB57BFF, 0xFFFF5C8A, 0xFFFF5555,
            0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF, 0xFF5599FF, 0xFFAAAAAA
    };

    public ColorSetting(String name, int defaultValue) {
        super(name, defaultValue);
    }

    public void next() {
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == value) {
                value = PALETTE[(i + 1) % PALETTE.length];
                return;
            }
        }
        value = PALETTE[0];
    }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive()) value = json.getAsInt();
    }
}
