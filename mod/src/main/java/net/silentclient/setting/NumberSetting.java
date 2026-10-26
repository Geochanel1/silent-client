package net.silentclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {

    private final double min, max, step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double v) {
        double snapped = Math.round(v / step) * step;
        value = Math.max(min, Math.min(max, snapped));
    }

    public double getMin() { return min; }
    public double getMax() { return max; }

    public String format() {
        return step >= 1 ? String.valueOf((int) Math.round(value)) : String.format("%.2f", value);
    }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive()) set(json.getAsDouble());
    }
}
