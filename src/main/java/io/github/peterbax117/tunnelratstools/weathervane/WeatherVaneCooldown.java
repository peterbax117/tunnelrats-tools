package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The cooldown is shared by every Weather Vane on the server, so placing more vanes does not allow more changes.
 * Time and weather are server-wide too. Stored with the overworld's data and measured in game ticks, which
 * keep counting when a vane changes the time of day.
 */
public final class WeatherVaneCooldown extends SavedData
{
    private static final String NAME = TunnelRatsTools.MODID + "_weather_vane";
    private static final long NEVER = Long.MIN_VALUE;

    private long lastActivation = NEVER;

    public static WeatherVaneCooldown get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WeatherVaneCooldown::new, WeatherVaneCooldown::load, null), NAME);
    }

    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    public long remainingTicks(long now, int cooldownSeconds) {
        if(lastActivation == NEVER || cooldownSeconds <= 0) return 0;
        long cooldownTicks = cooldownSeconds * 20L;
        return Math.min(cooldownTicks, Math.max(0, lastActivation + cooldownTicks - now));
    }

    public void markUsed(long now) {
        lastActivation = now;
        setDirty();
    }

    /** Forgets the last use (for tests and admins). */
    public void reset() {
        lastActivation = NEVER;
        setDirty();
    }

    private static WeatherVaneCooldown load(CompoundTag tag, HolderLookup.Provider registries) {
        WeatherVaneCooldown data = new WeatherVaneCooldown();
        if(tag.contains("LastActivation")) data.lastActivation = tag.getLong("LastActivation");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if(lastActivation != NEVER) tag.putLong("LastActivation", lastActivation);
        return tag;
    }
}
