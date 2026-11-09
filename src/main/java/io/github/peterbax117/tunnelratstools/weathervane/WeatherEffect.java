package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * What a Weather Vane does when it fires. Time targets are ticks into the 24000 tick day: sunrise is 23000 (the sky
 * starts to brighten and the sun comes over the horizon), noon 6000, sunset 12000, midnight 18000.
 */
public enum WeatherEffect implements StringRepresentable
{
    SUNRISE("sunrise", 23000),
    NOON("noon", 6000),
    SUNSET("sunset", 12000),
    MIDNIGHT("midnight", 18000),
    CLEAR("clear", -1),
    RAIN("rain", -1),
    THUNDER("thunder", -1);

    private static final WeatherEffect[] VALUES = values();

    private final String name;
    private final long targetTime;

    WeatherEffect(String name, long targetTime) {
        this.name = name;
        this.targetTime = targetTime;
    }

    public boolean isTime() {
        return targetTime >= 0;
    }

    /** Tick of the day this effect moves to; only for time effects. */
    public long targetTime() {
        return targetTime;
    }

    public Component displayName() {
        return Component.translatable("effect." + TunnelRatsTools.MODID + "." + name);
    }

    /** "time set to Noon", "weather set to Rain": used in chat announcements. */
    public Component changeDescription() {
        return Component.translatable("effect." + TunnelRatsTools.MODID + "." + name + ".change");
    }

    public static @Nullable WeatherEffect byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : null;
    }

    public static WeatherEffect byName(String name, WeatherEffect fallback) {
        for(WeatherEffect effect : VALUES) {
            if(effect.name.equals(name)) return effect;
        }
        return fallback;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
