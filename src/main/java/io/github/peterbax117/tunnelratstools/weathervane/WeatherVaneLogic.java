package io.github.peterbax117.tunnelratstools.weathervane;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/** Time and weather rules, separate from the block so tests can check them directly. */
public final class WeatherVaneLogic
{
    public static final long DAY_LENGTH = 24000L;

    private WeatherVaneLogic() {}

    /**
     * The next day time, strictly after {@code current}, whose time of day is {@code target}. The day count
     * ({@code dayTime / 24000}) never goes down: time only moves forward, unlike {@code /time set}.
     */
    public static long nextDayTime(long current, long target) {
        long next = Math.floorDiv(current, DAY_LENGTH) * DAY_LENGTH + target;
        return next <= current ? next + DAY_LENGTH : next;
    }

    /** Weather exists where it can be seen: dimensions with a sky and no ceiling (not the Nether or the End). */
    public static boolean hasWeather(Level level) {
        DimensionType type = level.dimensionType();
        return type.hasSkyLight() && !type.hasCeiling();
    }

    /** READY, or why this dimension cannot have the effect. */
    public static WeatherVaneStatus dimensionStatus(Level level, WeatherEffect effect) {
        if(effect.isTime()) return level.dimensionType().hasFixedTime() ? WeatherVaneStatus.FIXED_TIME : WeatherVaneStatus.READY;
        return hasWeather(level) ? WeatherVaneStatus.READY : WeatherVaneStatus.NO_WEATHER;
    }

    /** Every check except fuel, in the order the GUI reports them. */
    public static WeatherVaneStatus check(Level level, WeatherEffect effect, WeatherVaneSettings settings, long cooldownTicks) {
        if(!settings.enabled()) return WeatherVaneStatus.DISABLED;
        if(!settings.allows(effect)) return effect.isTime() ? WeatherVaneStatus.TIME_DISABLED : WeatherVaneStatus.WEATHER_DISABLED;
        WeatherVaneStatus dimension = dimensionStatus(level, effect);
        if(dimension != WeatherVaneStatus.READY) return dimension;
        if(cooldownTicks > 0) return WeatherVaneStatus.COOLDOWN;
        return WeatherVaneStatus.READY;
    }

    /**
     * Applies the effect the way the vanilla commands do. Time and weather are server-wide (other dimensions read the
     * overworld's), so like {@code /time} the time is set on every level, and like {@code /weather} the weather is set
     * on the overworld with the same random durations.
     */
    public static void apply(ServerLevel level, WeatherEffect effect) {
        var server = level.getServer();
        if(effect.isTime()) {
            long next = nextDayTime(level.getDayTime(), effect.targetTime());
            for(ServerLevel each : server.getAllLevels()) {
                each.setDayTime(next);
            }
            return;
        }
        ServerLevel overworld = server.overworld();
        var random = overworld.getRandom();
        switch(effect) {
            case CLEAR -> overworld.setWeatherParameters(ServerLevel.RAIN_DELAY.sample(random), 0, false, false);
            case RAIN -> overworld.setWeatherParameters(0, ServerLevel.RAIN_DURATION.sample(random), true, false);
            case THUNDER -> overworld.setWeatherParameters(0, ServerLevel.THUNDER_DURATION.sample(random), true, true);
            default -> throw new IllegalArgumentException("Not a weather effect: " + effect);
        }
    }
}
