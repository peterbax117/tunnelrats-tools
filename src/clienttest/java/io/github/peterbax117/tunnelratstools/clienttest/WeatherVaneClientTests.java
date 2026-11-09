package io.github.peterbax117.tunnelratstools.clienttest;

import io.github.peterbax117.modtestharness.client.ClientTest;
import io.github.peterbax117.modtestharness.client.ClientTestContext;
import io.github.peterbax117.modtestharness.client.ClientTestHolder;
import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import io.github.peterbax117.tunnelratstools.client.WeatherVaneScreen;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherEffect;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlockEntity;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneSettings;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneStatus;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static io.github.peterbax117.modtestharness.client.ClientTestContext.check;

/** Weather Vane GUI in a real client, and recipe images for the listing pages. */
@ClientTestHolder
public final class WeatherVaneClientTests
{
    private static final BlockPos STAND = new BlockPos(0, -60, 0);
    private static final BlockPos VANE = new BlockPos(2, -60, 0);

    @ClientTest(value = "Weather Vane GUI selects and fires an effect (saves gui-weather_vane.png)", order = 10)
    public static void guiSelectsAndFires(ClientTestContext ctx) {
        ctx.teleport(STAND, -90, 0);
        ctx.fill(VANE, VANE, ModRegistries.WEATHER_VANE.get().defaultBlockState());
        ctx.onServerDo(p -> {
            WeatherVaneBlockEntity vane = vane(p);
            // Cooldown 0 so the test does not depend on earlier activations in this world.
            vane.setSettingsOverride(new WeatherVaneSettings(true, true, true, 0, true, true, 10000));
            vane.catalyst().setStackInSlot(0, new ItemStack(ModRegistries.SKY_SHARD.get(), 3));
            vane.energy().receiveEnergy(40000, false);
            vane.setSelected(WeatherEffect.CLEAR);
            p.openMenu(vane, VANE);
        });
        WeatherVaneScreen screen = ctx.awaitScreen(WeatherVaneScreen.class);

        press(ctx, screen, "Noon");
        ctx.await("server to select Noon", () -> ctx.onServer(p -> vane(p).selected() == WeatherEffect.NOON));
        ctx.await("GUI to show Noon, ready", () -> screen.getMenu().selected() == WeatherEffect.NOON
                && screen.getMenu().status() == WeatherVaneStatus.READY && screen.getMenu().energy() == 40000);
        ctx.screenshotContainer("gui-weather_vane");

        press(ctx, screen, "Activate");
        ctx.await("energy to pay for the use", () -> ctx.onServer(p -> vane(p).energy().getEnergyStored() == 30000));
        check(ctx.onServer(p -> vane(p).catalyst().getStackInSlot(0).getCount()) == 3, "Energy should pay, not a Sky Shard");
        long timeOfDay = ctx.onServer(p -> p.serverLevel().getDayTime() % 24000);
        check(timeOfDay >= 6000 && timeOfDay < 6200, "Time should be just after noon, was " + timeOfDay);
        ctx.closeContainer();
    }

    @ClientTest(value = "recipe images for the Weather Vane, Sky Shard and Rain Detector", order = 11)
    public static void recipeScreenshots(ClientTestContext ctx) {
        recipe(ctx, "weather_vane", ModRegistries.WEATHER_VANE_ITEM.get());
        recipe(ctx, "rain_detector", ModRegistries.RAIN_DETECTOR_ITEM.get());
        recipe(ctx, "sky_shard", ModRegistries.SKY_SHARD.get());
    }

    private static void recipe(ClientTestContext ctx, String id, Item expected) {
        ItemStack result = ctx.showCraftingRecipe(TunnelRatsTools.loc(id));
        check(result.is(expected), "Recipe " + id + " should craft " + expected + ", got " + result);
        ctx.screenshotCraftingGrid("recipe-" + id);
        ctx.closeContainer();
    }

    private static WeatherVaneBlockEntity vane(ServerPlayer player) {
        return (WeatherVaneBlockEntity) player.serverLevel().getBlockEntity(VANE);
    }

    /** Presses the GUI button with this label, as a click would. */
    private static void press(ClientTestContext ctx, WeatherVaneScreen screen, String label) {
        ctx.onClient(() -> screen.children().stream()
                .filter(child -> child instanceof Button button && button.getMessage().getString().equals(label))
                .map(Button.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("No button " + label))
                .onPress());
    }
}
