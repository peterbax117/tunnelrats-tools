package io.github.peterbax117.tunnelratstools;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(TunnelRatsTools.MODID)
public final class TunnelRatsTools
{
    public static final String MODID = "tunnelrats_tools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TunnelRatsTools(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistries.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, TunnelRatsConfig.SPEC);
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
