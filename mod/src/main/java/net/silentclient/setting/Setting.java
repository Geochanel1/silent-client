package net.silentclient.setting;

import com.google.gson.JsonElement;

public abstract class Setting<T> {

    private final String name;
    protected T value;
    private final T defaultValue;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public void reset() { this.value = defaultValue; }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement json);
}
