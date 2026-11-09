package io.github.peterbax117.tunnelratstools.client;

import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client entry point: makes the config editable in game from the Mods list. */
@Mod(value = TunnelRatsTools.MODID, dist = Dist.CLIENT)
public final class TunnelRatsToolsClient
{
    public TunnelRatsToolsClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
