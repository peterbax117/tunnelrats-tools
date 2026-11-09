package io.github.peterbax117.tunnelratstools;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config (config/tunnelrats_tools-common.toml), shared by server and clients.
 *
 * Turning a tool off never removes its blocks or items from the registries, so worlds that contain them still
 * load. A disabled tool keeps its entries; its recipes and its use are disabled instead (see ModRegistries).
 */
public final class TunnelRatsConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Time and weather control block: changes the time of day or the weather on use.")
                .translation(key("timeWeather"))
                .push("timeWeather");
    }

    public static final ModConfigSpec.BooleanValue TIME_WEATHER_ENABLED = BUILDER
            .comment("Enable the time and weather control block. When false, the block stays registered so worlds",
                    "keep loading, but its recipe and its use are disabled.")
            .translation(key("timeWeather.enabled"))
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue TIME_WEATHER_ALLOW_TIME = BUILDER
            .comment("Allow the block to change the time of day (sunrise, noon, sunset, midnight).")
            .translation(key("timeWeather.allowTime"))
            .define("allowTime", true);

    public static final ModConfigSpec.BooleanValue TIME_WEATHER_ALLOW_WEATHER = BUILDER
            .comment("Allow the block to change the weather (clear, rain, thunder).")
            .translation(key("timeWeather.allowWeather"))
            .define("allowWeather", true);

    public static final ModConfigSpec.IntValue TIME_WEATHER_COOLDOWN_SECONDS = BUILDER
            .comment("Seconds a block must wait between two uses. 0 disables the cooldown.")
            .translation(key("timeWeather.cooldownSeconds"))
            .defineInRange("cooldownSeconds", 60, 0, 86400);

    public static final ModConfigSpec.BooleanValue TIME_WEATHER_ANNOUNCE_IN_CHAT = BUILDER
            .comment("Post a chat message naming the player who changed the time or the weather.")
            .translation(key("timeWeather.announceInChat"))
            .define("announceInChat", true);

    public static final ModConfigSpec.BooleanValue TIME_WEATHER_REQUIRE_CATALYST = BUILDER
            .comment("Each use costs one catalyst item (or Forge Energy when the block is powered).",
                    "When false, using the block is free.")
            .translation(key("timeWeather.requireCatalyst"))
            .define("requireCatalyst", true);

    static {
        BUILDER.pop();
        BUILDER.comment("Storage crate: a large container that keeps its contents when broken.")
                .translation(key("crate"))
                .push("crate");
    }

    public static final ModConfigSpec.BooleanValue CRATE_ENABLED = BUILDER
            .comment("Enable the storage crate. When false, crates stay registered so worlds keep loading and",
                    "placed crates keep their contents, but crate recipes and the chest upgrade are disabled.")
            .translation(key("crate.enabled"))
            .define("enabled", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private TunnelRatsConfig() {}

    private static String key(String path) {
        return TunnelRatsTools.MODID + ".configuration." + path;
    }
}
