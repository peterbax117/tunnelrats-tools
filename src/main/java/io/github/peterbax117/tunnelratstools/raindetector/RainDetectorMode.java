package io.github.peterbax117.tunnelratstools.raindetector;

import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

/** What a Rain Detector reacts to. Right-click cycles rain, thunder, clear. */
public enum RainDetectorMode implements StringRepresentable
{
    /** 15 while it rains or thunders. */
    RAIN("rain"),
    /** 15 only while it thunders. */
    THUNDER("thunder"),
    /** 15 while it does not rain: the inverse of RAIN. */
    CLEAR("clear");

    private final String name;

    RainDetectorMode(String name) {
        this.name = name;
    }

    public RainDetectorMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean active(Level level) {
        return switch(this) {
            case RAIN -> level.isRaining();
            case THUNDER -> level.isThundering();
            case CLEAR -> !level.isRaining();
        };
    }

    public Component displayName() {
        return Component.translatable("mode." + TunnelRatsTools.MODID + ".rain_detector." + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
