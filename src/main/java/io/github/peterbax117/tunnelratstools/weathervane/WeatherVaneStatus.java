package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Whether a Weather Vane can fire right now, and if not, why. Checked in this order. */
public enum WeatherVaneStatus
{
    READY,
    DISABLED,
    TIME_DISABLED,
    WEATHER_DISABLED,
    FIXED_TIME,
    NO_WEATHER,
    COOLDOWN,
    NO_FUEL;

    private static final WeatherVaneStatus[] VALUES = values();

    public Component message() {
        return Component.translatable("status." + TunnelRatsTools.MODID + ".weather_vane." + name().toLowerCase(Locale.ROOT));
    }

    public static WeatherVaneStatus byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : DISABLED;
    }
}
