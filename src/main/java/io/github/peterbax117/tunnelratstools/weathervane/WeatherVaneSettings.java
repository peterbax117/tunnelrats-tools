package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.TunnelRatsConfig;

/** The config values a Weather Vane works with, read once per check so one activation sees one consistent set. */
public record WeatherVaneSettings(boolean enabled, boolean allowTime, boolean allowWeather, int cooldownSeconds,
                                  boolean announceInChat, boolean requireCatalyst, int energyPerUse)
{
    public static WeatherVaneSettings fromConfig() {
        return new WeatherVaneSettings(
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ENABLED),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ALLOW_TIME),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ALLOW_WEATHER),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_COOLDOWN_SECONDS),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ANNOUNCE_IN_CHAT),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_REQUIRE_CATALYST),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ENERGY_PER_USE));
    }

    public boolean allows(WeatherEffect effect) {
        return effect.isTime() ? allowTime : allowWeather;
    }
}
