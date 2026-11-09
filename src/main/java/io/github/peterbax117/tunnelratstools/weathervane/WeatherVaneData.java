package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Server-wide Weather Vane state, saved with the overworld's data.
 *
 * <p>Cooldown: shared by every Weather Vane on the server, so placing more vanes does not allow more changes. Time
 * and weather are server-wide too. Measured in game ticks, which keep counting when a vane changes the time of day.
 *
 * <p>Sequence: an activation pays and starts the cooldown at once, then charges for {@code chargeSeconds}, then
 * releases the effect; a time effect then fast-forwards for {@code animationSeconds}. Only one charge or
 * fast-forward runs at a time on the server; vanes refuse activation meanwhile.
 *
 * <p>Robustness: the pending charge and fast-forward live here, not in the block, so breaking the vane or unloading
 * its chunk does not stop them: the effect was paid for and still applies (only the particles stop). Both are
 * saved; one interrupted by a server stop is finished at once (no charge, no fast-forward) on the first tick after
 * the world loads again, so the world never stays half changed.
 */
public final class WeatherVaneData extends SavedData
{
    private static final String NAME = TunnelRatsTools.MODID + "_weather_vane";
    private static final long NEVER = Long.MIN_VALUE;
    public static final int BEAM_TICKS = 20;

    private long lastActivation = NEVER;
    private @Nullable Charge charge;
    private @Nullable TimeAnimation animation;
    private @Nullable Beam beam;
    // True when the pending charge or fast-forward was read from disk: it was interrupted by a server stop.
    private boolean interrupted;

    /** An activation waiting to release its effect. */
    record Charge(WeatherEffect effect, ResourceKey<Level> dimension, BlockPos pos, int durationTicks, int elapsedTicks,
                  boolean animateTime, int animationTicks, boolean showEffects) {}

    /** A fast-forward from day time {@code from} to {@code to} over {@code durationTicks} ticks. */
    record TimeAnimation(long from, long to, int durationTicks, int elapsedTicks)
    {
        long dayTimeAt(int elapsed) {
            return elapsed >= durationTicks ? to : from + (to - from) * elapsed / durationTicks;
        }
    }

    /** The release column of particles; visual only, not saved. */
    record Beam(WeatherEffect effect, ResourceKey<Level> dimension, BlockPos pos, int elapsedTicks) {}

    public static WeatherVaneData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WeatherVaneData::new, WeatherVaneData::load, null), NAME);
    }

    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    // Cooldown

    public long remainingTicks(long now, int cooldownSeconds) {
        if(lastActivation == NEVER || cooldownSeconds <= 0) return 0;
        long cooldownTicks = cooldownSeconds * 20L;
        return Math.min(cooldownTicks, Math.max(0, lastActivation + cooldownTicks - now));
    }

    public void markUsed(long now) {
        lastActivation = now;
        setDirty();
    }

    /** Forgets the last use and finishes any pending charge or fast-forward at once (for tests and admins). */
    public void reset(MinecraftServer server) {
        lastActivation = NEVER;
        finishAll(server);
        beam = null;
        setDirty();
    }

    // Charge, release and fast-forward

    /** READY, or CHARGING / ANIMATING while a vane somewhere is busy. */
    public WeatherVaneStatus busyStatus() {
        if(charge != null) return WeatherVaneStatus.CHARGING;
        if(animation != null) return WeatherVaneStatus.ANIMATING;
        return WeatherVaneStatus.READY;
    }

    public boolean animating() {
        return animation != null;
    }

    public boolean charging() {
        return charge != null;
    }

    /** Starts the charge for a paid activation, or releases at once when chargeSeconds is 0. */
    public void begin(ServerLevel level, BlockPos pos, WeatherEffect effect, WeatherVaneSettings settings) {
        Charge started = new Charge(effect, level.dimension(), pos.immutable(), settings.chargeTicks(), 0,
                settings.animateTime(), settings.animationTicks(), settings.showEffects());
        interrupted = false;
        if(started.durationTicks() <= 0) {
            release(level.getServer(), started);
            return;
        }
        charge = started;
        setDirty();
        if(started.showEffects()) WeatherVaneEffects.chargeStart(level, pos);
    }

    public void startAnimation(long from, long to, int durationTicks) {
        animation = new TimeAnimation(from, to, Math.max(1, durationTicks), 0);
        setDirty();
    }

    /** Called at the end of every server tick. */
    public void tick(MinecraftServer server) {
        if(interrupted) {
            TunnelRatsTools.LOGGER.info("Finishing a Weather Vane change interrupted by a server stop");
            finishAll(server);
            return;
        }
        if(charge != null) {
            int elapsed = charge.elapsedTicks() + 1;
            Charge current = new Charge(charge.effect(), charge.dimension(), charge.pos(), charge.durationTicks(), elapsed,
                    charge.animateTime(), charge.animationTicks(), charge.showEffects());
            ServerLevel level = server.getLevel(current.dimension());
            if(elapsed >= current.durationTicks()) {
                charge = null;
                release(server, current);
            } else {
                charge = current;
                if(current.showEffects() && level != null && WeatherVaneEffects.vanePresent(level, current.pos())) {
                    WeatherVaneEffects.chargeTick(level, current.pos(), current.effect(), elapsed, current.durationTicks());
                }
            }
        }
        if(animation != null) {
            int elapsed = animation.elapsedTicks() + 1;
            if(elapsed >= animation.durationTicks()) {
                finishAnimation(server);
            } else {
                animation = new TimeAnimation(animation.from(), animation.to(), animation.durationTicks(), elapsed);
                WeatherVaneLogic.setTimeEverywhere(server, animation.dayTimeAt(elapsed));
            }
        }
        if(beam != null) {
            ServerLevel level = server.getLevel(beam.dimension());
            if(level != null && level.isLoaded(beam.pos())) {
                WeatherVaneEffects.beamTick(level, beam.pos(), beam.effect(), beam.elapsedTicks());
            }
            int elapsed = beam.elapsedTicks() + 1;
            beam = elapsed >= BEAM_TICKS ? null : new Beam(beam.effect(), beam.dimension(), beam.pos(), elapsed);
        }
    }

    private void release(MinecraftServer server, Charge released) {
        ServerLevel level = server.getLevel(released.dimension());
        if(level == null) level = server.overworld();
        WeatherVaneLogic.apply(level, released.effect(), released.animateTime(), released.animationTicks());
        if(released.showEffects() && WeatherVaneEffects.vanePresent(level, released.pos())) {
            WeatherVaneEffects.release(level, released.pos(), released.effect());
            beam = new Beam(released.effect(), released.dimension(), released.pos(), 0);
        }
        setDirty();
    }

    /** Applies a pending charge and ends a fast-forward at once, without particles. */
    private void finishAll(MinecraftServer server) {
        interrupted = false;
        if(charge != null) {
            Charge pending = charge;
            charge = null;
            ServerLevel level = server.getLevel(pending.dimension());
            WeatherVaneLogic.apply(level != null ? level : server.overworld(), pending.effect(), false, 0);
        }
        if(animation != null) finishAnimation(server);
        setDirty();
    }

    private void finishAnimation(MinecraftServer server) {
        long target = animation.to();
        animation = null;
        WeatherVaneLogic.setTimeEverywhere(server, target);
        setDirty();
    }

    /** Reads saved data; a charge or fast-forward found here was interrupted and is finished on the next tick. */
    public static WeatherVaneData load(CompoundTag tag, HolderLookup.Provider registries) {
        WeatherVaneData data = new WeatherVaneData();
        if(tag.contains("LastActivation")) data.lastActivation = tag.getLong("LastActivation");
        if(tag.contains("Charge")) {
            CompoundTag c = tag.getCompound("Charge");
            ResourceLocation dimension = ResourceLocation.tryParse(c.getString("Dimension"));
            data.charge = new Charge(WeatherEffect.byName(c.getString("Effect"), WeatherEffect.CLEAR),
                    ResourceKey.create(Registries.DIMENSION, dimension != null ? dimension : Level.OVERWORLD.location()),
                    BlockPos.of(c.getLong("Pos")), c.getInt("Duration"), c.getInt("Elapsed"),
                    c.getBoolean("AnimateTime"), c.getInt("AnimationTicks"), c.getBoolean("ShowEffects"));
        }
        if(tag.contains("Animation")) {
            CompoundTag a = tag.getCompound("Animation");
            data.animation = new TimeAnimation(a.getLong("From"), a.getLong("To"), a.getInt("Duration"), a.getInt("Elapsed"));
        }
        data.interrupted = data.charge != null || data.animation != null;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if(lastActivation != NEVER) tag.putLong("LastActivation", lastActivation);
        if(charge != null) {
            CompoundTag c = new CompoundTag();
            c.putString("Effect", charge.effect().getSerializedName());
            c.putString("Dimension", charge.dimension().location().toString());
            c.putLong("Pos", charge.pos().asLong());
            c.putInt("Duration", charge.durationTicks());
            c.putInt("Elapsed", charge.elapsedTicks());
            c.putBoolean("AnimateTime", charge.animateTime());
            c.putInt("AnimationTicks", charge.animationTicks());
            c.putBoolean("ShowEffects", charge.showEffects());
            tag.put("Charge", c);
        }
        if(animation != null) {
            CompoundTag a = new CompoundTag();
            a.putLong("From", animation.from());
            a.putLong("To", animation.to());
            a.putInt("Duration", animation.durationTicks());
            a.putInt("Elapsed", animation.elapsedTicks());
            tag.put("Animation", a);
        }
        return tag;
    }
}