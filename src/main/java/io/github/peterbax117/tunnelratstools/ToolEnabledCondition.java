package io.github.peterbax117.tunnelratstools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Recipe condition {@code tunnelrats_tools:tool_enabled}: true while the named tool is enabled in the common config.
 * Disabled tools keep their registry entries; this condition removes their recipes instead.
 *
 * <pre>"neoforge:conditions": [{"type": "tunnelrats_tools:tool_enabled", "tool": "time_weather"}]</pre>
 */
public record ToolEnabledCondition(Tool tool) implements ICondition
{
    public static final MapCodec<ToolEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Tool.CODEC.fieldOf("tool").forGetter(ToolEnabledCondition::tool)
    ).apply(instance, ToolEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return tool.enabled();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    public enum Tool implements StringRepresentable
    {
        TIME_WEATHER("time_weather", TunnelRatsConfig.TIME_WEATHER_ENABLED),
        RAIN_DETECTOR("rain_detector", TunnelRatsConfig.RAIN_DETECTOR_ENABLED),
        CRATE("crate", TunnelRatsConfig.CRATE_ENABLED);

        public static final Codec<Tool> CODEC = StringRepresentable.fromEnum(Tool::values);

        private final String name;
        private final ModConfigSpec.BooleanValue option;

        Tool(String name, ModConfigSpec.BooleanValue option) {
            this.name = name;
            this.option = option;
        }

        public boolean enabled() {
            return TunnelRatsConfig.value(option);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
