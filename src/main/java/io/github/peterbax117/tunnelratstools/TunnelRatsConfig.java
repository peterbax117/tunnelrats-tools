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
        BUILDER.comment("Weather Vane: changes the time of day or the weather on use.")
                .translation(key("timeWeather"))
                .push("timeWeather");
    }

    public static final ModConfigSpec.BooleanValue TIME_WEATHER_ENABLED = BUILDER
            .comment("Enable the Weather Vane and the Sky Shard. When false, both stay registered so worlds",
                    "keep loading, but their recipes are disabled and the block refuses to activate.")
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
            .comment("Seconds between two uses, shared by every Weather Vane on the server. 0 disables the cooldown.")
            .translation(key("timeWeather.cooldownSeconds"))
            .defineInRange("cooldownSeconds", 60, 0, 86400);

    public static final ModConfigSpec.BooleanValue TIME_WEATHER_ANNOUNCE_IN_CHAT = BUILDER
            .comment("Post a chat message naming the player who changed the time or the weather.")
            .translation(key("timeWeather.announceInChat"))
            .define("announceInChat", true);

    public static final ModConfigSpec.BooleanValue TIME_WEATHER_REQUIRE_CATALYST = BUILDER
            .comment("Each use costs one Sky Shard, or Forge Energy when the internal buffer holds enough.",
                    "When false, using the block is free.")
            .translation(key("timeWeather.requireCatalyst"))
            .define("requireCatalyst", true);

    // 10,000 FE is about two minutes of output from a small early generator (80 FE/t), so the energy path costs
    // real infrastructure, comparable to crafting a Sky Shard by hand.
    public static final ModConfigSpec.IntValue TIME_WEATHER_ENERGY_PER_USE = BUILDER
            .comment("Forge Energy one use costs when the internal buffer holds enough. Otherwise one Sky Shard",
                    "is used. 0 turns the energy path off, so only Sky Shards are accepted.")
            .translation(key("timeWeather.energyPerUse"))
            .defineInRange("energyPerUse", 10000, 0, Integer.MAX_VALUE);

    // Ten uses of buffer: a power network can charge it between uses, and it stays small enough that a full
    // block is not a battery worth stealing.
    public static final ModConfigSpec.IntValue TIME_WEATHER_ENERGY_CAPACITY = BUILDER
            .comment("Size of the Weather Vane's internal Forge Energy buffer. Applies to blocks loaded after a change.")
            .translation(key("timeWeather.energyCapacity"))
            .defineInRange("energyCapacity", 100000, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
        BUILDER.comment("Rain Detector: a redstone signal from the dimension's weather.")
                .translation(key("rainDetector"))
                .push("rainDetector");
    }

    public static final ModConfigSpec.BooleanValue RAIN_DETECTOR_ENABLED = BUILDER
            .comment("Enable the Rain Detector. When false, detectors stay registered so worlds keep loading, but",
                    "they output no signal and their recipe is disabled.")
            .translation(key("rainDetector.enabled"))
            .define("enabled", true);

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

    /** The configured value, or the default before the config file is loaded (for example in early client code). */
    public static <T> T value(ModConfigSpec.ConfigValue<T> option) {
        return SPEC.isLoaded() ? option.get() : option.getDefault();
    }

    private static String key(String path) {
        return TunnelRatsTools.MODID + ".configuration." + path;
    }
}
