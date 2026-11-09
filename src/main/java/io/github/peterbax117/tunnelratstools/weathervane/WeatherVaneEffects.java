package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/**
 * Charge swirl, release beam and sounds. Vanilla particle types and sounds only, sent by the server
 * (ServerLevel.sendParticles reaches players within 32 blocks of each particle), so clients need no mod code.
 * Counts are kept small: a few particle packets per tick.
 */
public final class WeatherVaneEffects
{
    private static final int BEAM_HEIGHT = 64;

    private WeatherVaneEffects() {}

    /** Warm gold for time, near white for Clear, blue for Rain, purple for Thunder. */
    static DustParticleOptions dust(WeatherEffect effect, float scale) {
        Vector3f color = switch(effect) {
            case CLEAR -> new Vector3f(0.80F, 0.93F, 1.00F);
            case RAIN -> new Vector3f(0.20F, 0.42F, 1.00F);
            case THUNDER -> new Vector3f(0.62F, 0.28F, 0.95F);
            default -> new Vector3f(1.00F, 0.76F, 0.22F);
        };
        return new DustParticleOptions(color, scale);
    }

    /**
     * A glowing particle in the effect's color. Dust takes the world's light and looks dark at night; these vanilla
     * glow particles stay bright: orange wax for time, white wax for Clear, glow for Rain, sparks for Thunder.
     */
    static ParticleOptions accent(WeatherEffect effect) {
        return switch(effect) {
            case CLEAR -> ParticleTypes.WAX_OFF;
            case RAIN -> ParticleTypes.GLOW;
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.WAX_ON;
        };
    }

    static boolean vanePresent(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).is(ModRegistries.WEATHER_VANE.get());
    }

    static void chargeStart(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 0.8F);
    }

    /** A spiral rising from the vane: more particles, a tighter radius and a higher pitch as the charge completes. */
    static void chargeTick(ServerLevel level, BlockPos pos, WeatherEffect effect, int elapsed, int duration) {
        double progress = Math.min(1.0, elapsed / (double) duration);
        double x = pos.getX() + 0.5, y = pos.getY() + 1.05, z = pos.getZ() + 0.5;
        int arms = 2 + (int) (progress * 4);
        double radius = 0.9 - 0.55 * progress;
        DustParticleOptions dust = dust(effect, 1.0F + (float) progress);
        for(int i = 0; i < arms; i++) {
            double angle = elapsed * 0.45 + i * (Math.PI * 2 / arms);
            double height = (elapsed * 0.08 + i * 0.37) % 1.8;
            double px = x + Math.cos(angle) * radius, py = y + height, pz = z + Math.sin(angle) * radius;
            level.sendParticles(dust, px, py, pz, 1, 0, 0, 0, 0);
            if(elapsed % 2 == 0) level.sendParticles(accent(effect), px, py, pz, 1, 0, 0, 0, 0);
        }
        if(progress > 0.6 && elapsed % 3 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, x, y + 0.6, z, 2, 0.2, 0.3, 0.2, 0.02);
        }
        if(elapsed % 5 == 0) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 0.5F + 1.5F * (float) progress);
        }
    }

    static void release(ServerLevel level, BlockPos pos, WeatherEffect effect) {
        if(effect == WeatherEffect.THUNDER) {
            level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.2F, 1.0F);
        } else {
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
    }

    /** One tick of the release column: colored dust along the column and a few particles shooting upward. */
    static void beamTick(ServerLevel level, BlockPos pos, WeatherEffect effect, int elapsed) {
        int height = Math.max(4, Math.min(BEAM_HEIGHT, level.getMaxBuildHeight() - pos.getY() - 1));
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
        DustParticleOptions dust = dust(effect, 1.6F);
        int segments = 6;
        for(int i = 0; i < segments; i++) {
            // Spread the segments over the column and shift them each tick, so 20 ticks fill it evenly.
            double h = 1.0 + ((i + (elapsed * 0.37) % 1.0) / segments) * height;
            level.sendParticles(dust, x, pos.getY() + h, z, 3, 0.08, height / (segments * 4.0), 0.08, 0);
            if(i % 2 == 0) level.sendParticles(accent(effect), x, pos.getY() + h, z, 1, 0.05, height / (segments * 4.0), 0.05, 0);
        }
        // Count 0 makes the offsets a velocity: these shoot straight up.
        ParticleOptions shooter = effect == WeatherEffect.THUNDER ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD;
        level.sendParticles(shooter, x, pos.getY() + 1.2, z, 0, 0, 1, 0, 1.4);
        level.sendParticles(shooter, x, pos.getY() + 1.2 + height / 2.0, z, 0, 0, 1, 0, 1.4);
    }
}
