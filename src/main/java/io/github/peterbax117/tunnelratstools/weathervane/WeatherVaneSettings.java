package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.TunnelRatsConfig;

/** The config values a Weather Vane works with, read once per check so one activation sees one consistent set. */
public record WeatherVaneSettings(boolean enabled, boolean allowTime, boolean allowWeather, int cooldownSeconds,
                                  boolean announceInChat, boolean requireCatalyst, int energyPerUse,
                                  double chargeSeconds, boolean animateTime, double animationSeconds, boolean showEffects)
{
    public static WeatherVaneSettings fromConfig() {
        return new WeatherVaneSettings(
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ENABLED),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ALLOW_TIME),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ALLOW_WEATHER),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_COOLDOWN_SECONDS),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ANNOUNCE_IN_CHAT),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_REQUIRE_CATALYST),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ENERGY_PER_USE),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_CHARGE_SECONDS),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ANIMATE_TIME),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ANIMATION_SECONDS),
                TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_SHOW_EFFECTS));
    }

    public boolean allows(WeatherEffect effect) {
        return effect.isTime() ? allowTime : allowWeather;
    }

    /** Length of the charge before the effect in ticks; 0 releases at once. */
    public int chargeTicks() {
        return Math.max(0, (int) Math.round(chargeSeconds * 20));
    }

    /** Length of a time fast-forward in ticks, at least 1. */
    public int animationTicks() {
        return Math.max(1, (int) Math.round(animationSeconds * 20));
    }
}
