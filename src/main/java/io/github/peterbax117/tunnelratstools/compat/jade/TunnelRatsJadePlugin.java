package io.github.peterbax117.tunnelratstools.compat.jade;

import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherEffect;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlock;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlockEntity;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade overlay for the Weather Vane: selected effect, charging or changing the sky, and the shared cooldown. Jade finds this class through the
 * annotation, so it is never loaded when Jade is not installed.
 */
@WailaPlugin
public class TunnelRatsJadePlugin implements IWailaPlugin
{
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(WeatherVaneProvider.INSTANCE, WeatherVaneBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(WeatherVaneProvider.INSTANCE, WeatherVaneBlock.class);
    }

    enum WeatherVaneProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor>
    {
        INSTANCE;

        private static final ResourceLocation UID = TunnelRatsTools.loc("weather_vane");

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if(accessor.getBlockEntity() instanceof WeatherVaneBlockEntity vane) {
                data.putString("Effect", vane.selected().getSerializedName());
                data.putLong("Cooldown", (vane.cooldownRemainingTicks() + 19) / 20);
                data.putString("Phase", vane.busyStatus().name());
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if(!data.contains("Effect")) return;
            WeatherEffect effect = WeatherEffect.byName(data.getString("Effect"), WeatherEffect.CLEAR);
            tooltip.add(Component.translatable("gui.tunnelrats_tools.weather_vane.selected", effect.displayName()));
            long cooldown = data.getLong("Cooldown");
            WeatherVaneStatus phase = WeatherVaneStatus.byName(data.getString("Phase"));
            if(phase != WeatherVaneStatus.READY) tooltip.add(phase.message());
            if(cooldown > 0) tooltip.add(Component.translatable("status.tunnelrats_tools.weather_vane.cooldown", cooldown));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
