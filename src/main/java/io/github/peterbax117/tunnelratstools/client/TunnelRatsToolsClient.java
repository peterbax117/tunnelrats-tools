package io.github.peterbax117.tunnelratstools.client;

import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client entry point: GUI screens, and the config made editable in game from the Mods list. */
@Mod(value = TunnelRatsTools.MODID, dist = Dist.CLIENT)
public final class TunnelRatsToolsClient
{
    public TunnelRatsToolsClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(TunnelRatsToolsClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistries.WEATHER_VANE_MENU.get(), WeatherVaneScreen::new);
    }
}
