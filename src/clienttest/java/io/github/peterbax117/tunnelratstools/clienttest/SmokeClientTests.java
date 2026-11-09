package io.github.peterbax117.tunnelratstools.clienttest;

import io.github.peterbax117.modtestharness.client.ClientTest;
import io.github.peterbax117.modtestharness.client.ClientTestContext;
import io.github.peterbax117.modtestharness.client.ClientTestHolder;
import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.List;

import static io.github.peterbax117.modtestharness.client.ClientTestContext.check;

/** Client smoke tests, in every client test setup (standalone and integrations). */
@ClientTestHolder
public final class SmokeClientTests
{
    @ClientTest(value = "optional mods on the client match the test setup", order = 1)
    public static void optionalModsMatchTestSetup(ClientTestContext ctx) {
        String setup = System.getProperty("tunnelrats_tools.test.setup", "unknown");
        boolean standalone = setup.equals("standalone");
        for(String mod : List.of("jei", "jade")) {
            check(ModList.get().isLoaded(mod) != standalone, mod + " loaded=" + ModList.get().isLoaded(mod) + " in setup " + setup);
        }
    }

    @ClientTest(value = "config screen opens from the Mods list", order = 2)
    public static void configScreenOpens(ClientTestContext ctx) {
        ModContainer container = ModList.get().getModContainerById(TunnelRatsTools.MODID).orElseThrow();
        IConfigScreenFactory factory = container.getCustomExtension(IConfigScreenFactory.class).orElse(null);
        check(factory != null, "Mod should register a config screen");

        // With a single config file NeoForge skips its file list and opens that file's section screen directly.
        ctx.onClient(() -> ctx.mc().setScreen(factory.createScreen(container, null)));
        var screen = ctx.awaitScreen(ConfigurationScreen.ConfigurationSectionScreen.class);
        String title = ctx.onClient(() -> screen.getTitle().getString());
        check(title.equals("TunnelRat's Tools: Common Settings"), "Config screen title was " + title);
        ctx.onClient(() -> ctx.mc().setScreen(null));
    }

    @ClientTest(value = "creative tab has its translated title", order = 3)
    public static void creativeTabTitle(ClientTestContext ctx) {
        CreativeModeTab tab = ctx.onClient(() -> ModRegistries.MAIN_TAB.get());
        Component title = tab.getDisplayName();
        check(title.getString().equals("TunnelRat's Tools"), "Creative tab title was " + title.getString());
    }
}
