package io.github.peterbax117.tunnelratstools.gametest;

import io.github.peterbax117.modtestharness.gametest.HarnessTemplates;
import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherEffect;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlock;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlockEntity;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneData;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneLogic;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneMenu;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneSettings;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.github.peterbax117.modtestharness.gametest.GameTestSupport.survivalPlayer;

/**
 * Weather Vane tests. Time and weather are server-wide, so tests that change them check the result in the same tick.
 * Vanes get per-block settings (no shared cooldown unless the test is about it), and tests that span ticks while
 * depending on the shared cooldown or the weather run in their own batch.
 */
@GameTestHolder(HarnessTemplates.NAMESPACE)
@PrefixGameTestTemplate(false)
public class WeatherVaneGameTests
{
    private static final String TEMPLATE = HarnessTemplates.EMPTY_9X5X9;
    private static final BlockPos VANE = new BlockPos(4, 1, 4);
    private static final int ENERGY_PER_USE = 1000;

    /** No charge, instant time changes, no particles: the effect applies inside activate(). */
    static WeatherVaneSettings settings(boolean requireCatalyst, int cooldownSeconds) {
        return new WeatherVaneSettings(true, true, true, cooldownSeconds, false, requireCatalyst, ENERGY_PER_USE, 0, false, 1.0, false);
    }

    static WeatherVaneBlockEntity placeVane(GameTestHelper helper, BlockPos pos, WeatherVaneSettings settings) {
        helper.setBlock(pos, ModRegistries.WEATHER_VANE.get());
        WeatherVaneBlockEntity vane = helper.getBlockEntity(pos);
        vane.setSettingsOverride(settings);
        return vane;
    }

    private static ServerLevel overworld(GameTestHelper helper) {
        return helper.getLevel().getServer().overworld();
    }

    @GameTest(template = TEMPLATE)
    public static void nextDayTimeOnlyMovesForward(GameTestHelper helper) {
        helper.assertValueEqual(WeatherVaneLogic.nextDayTime(130000, 6000), 150000L, "noon after day 5, 10000");
        helper.assertValueEqual(WeatherVaneLogic.nextDayTime(130000, 12000), 132000L, "sunset later the same day");
        helper.assertValueEqual(WeatherVaneLogic.nextDayTime(126000, 6000), 150000L, "exactly at noon: the next noon");
        helper.assertValueEqual(WeatherVaneLogic.nextDayTime(0, 23000), 23000L, "sunrise on day 0");
        helper.assertValueEqual(WeatherVaneLogic.nextDayTime(23500, 23000), 47000L, "just after sunrise: the next one");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void timeMovesForwardToEachTarget(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, settings(false, 0));
        ServerLevel level = overworld(helper);
        long start = 5 * 24000L + 10000L;
        Map<WeatherEffect, Long> expected = Map.of(
                WeatherEffect.SUNRISE, 5 * 24000L + 23000L,
                WeatherEffect.NOON, 6 * 24000L + 6000L,
                WeatherEffect.SUNSET, 5 * 24000L + 12000L,
                WeatherEffect.MIDNIGHT, 5 * 24000L + 18000L);
        for(var entry : expected.entrySet()) {
            level.setDayTime(start);
            vane.setSelected(entry.getKey());
            helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, entry.getKey() + " activation");
            long now = level.getDayTime();
            helper.assertValueEqual(now, entry.getValue(), entry.getKey() + " day time");
            helper.assertTrue(now / 24000 >= start / 24000, "Day count must not decrease");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void weatherSetToEachState(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, settings(false, 0));
        ServerLevelData data = (ServerLevelData) overworld(helper).getLevelData();

        vane.setSelected(WeatherEffect.THUNDER);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "thunder activation");
        helper.assertTrue(data.isRaining() && data.isThundering() && data.getThunderTime() > 0, "Thunder should be set");

        vane.setSelected(WeatherEffect.RAIN);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "rain activation");
        helper.assertTrue(data.isRaining() && !data.isThundering() && data.getRainTime() > 0, "Rain should be set");

        vane.setSelected(WeatherEffect.CLEAR);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "clear activation");
        helper.assertTrue(!data.isRaining() && !data.isThundering() && data.getClearWeatherTime() > 0, "Weather should be clear");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void catalystConsumedOncePerActivation(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, settings(true, 0));
        vane.catalyst().setStackInSlot(0, new ItemStack(ModRegistries.SKY_SHARD.get(), 3));
        vane.setSelected(WeatherEffect.NOON);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "first activation");
        helper.assertValueEqual(vane.catalyst().getStackInSlot(0).getCount(), 2, "shards after one activation");
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "second activation");
        helper.assertValueEqual(vane.catalyst().getStackInSlot(0).getCount(), 1, "shards after two activations");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void noCatalystAndNoEnergyHasNoEffect(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, settings(true, 0));
        ServerLevel level = overworld(helper);
        level.setDayTime(1000);
        vane.setSelected(WeatherEffect.MIDNIGHT);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.NO_FUEL, "activation without fuel");
        helper.assertValueEqual(level.getDayTime(), 1000L, "day time");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void energyPathUsesEnergyAndKeepsShards(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, settings(true, 0));
        vane.catalyst().setStackInSlot(0, new ItemStack(ModRegistries.SKY_SHARD.get(), 1));
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(VANE), null);
        helper.assertTrue(energy != null, "Weather Vane should expose an energy storage");
        helper.assertValueEqual(energy.receiveEnergy(ENERGY_PER_USE + 500, false), ENERGY_PER_USE + 500, "energy received");
        helper.assertValueEqual(energy.extractEnergy(100, false), 0, "energy must not be extractable");

        vane.setSelected(WeatherEffect.SUNSET);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "activation on energy");
        helper.assertValueEqual(energy.getEnergyStored(), 500, "energy after one use");
        helper.assertValueEqual(vane.catalyst().getStackInSlot(0).getCount(), 1, "shards must not be used while energy suffices");

        // 500 FE is not enough for another use, so the shard pays.
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "activation on the shard");
        helper.assertValueEqual(energy.getEnergyStored(), 500, "energy unchanged when the shard pays");
        helper.assertTrue(vane.catalyst().getStackInSlot(0).isEmpty(), "shard used once energy runs out");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void freeWhenCatalystNotRequired(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, settings(false, 0));
        vane.catalyst().setStackInSlot(0, new ItemStack(ModRegistries.SKY_SHARD.get(), 1));
        vane.setSelected(WeatherEffect.NOON);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "free activation");
        helper.assertValueEqual(vane.catalyst().getStackInSlot(0).getCount(), 1, "free use keeps the shard");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void disallowedTimeAndWeatherAreRejected(GameTestHelper helper) {
        ServerLevel level = overworld(helper);
        WeatherVaneBlockEntity noTime = placeVane(helper, VANE,
                new WeatherVaneSettings(true, false, true, 0, false, false, ENERGY_PER_USE, 0, false, 1.0, false));
        level.setDayTime(1000);
        noTime.setSelected(WeatherEffect.NOON);
        helper.assertValueEqual(noTime.activate(null), WeatherVaneStatus.TIME_DISABLED, "time with allowTime=false");
        helper.assertValueEqual(level.getDayTime(), 1000L, "day time unchanged");
        noTime.setSelected(WeatherEffect.CLEAR);
        helper.assertValueEqual(noTime.activate(null), WeatherVaneStatus.READY, "weather with allowTime=false");

        WeatherVaneBlockEntity noWeather = placeVane(helper, new BlockPos(2, 1, 2),
                new WeatherVaneSettings(true, true, false, 0, false, false, ENERGY_PER_USE, 0, false, 1.0, false));
        noWeather.setSelected(WeatherEffect.RAIN);
        helper.assertValueEqual(noWeather.activate(null), WeatherVaneStatus.WEATHER_DISABLED, "weather with allowWeather=false");
        noWeather.setSelected(WeatherEffect.NOON);
        helper.assertValueEqual(noWeather.activate(null), WeatherVaneStatus.READY, "time with allowWeather=false");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void disabledVaneRefuses(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, new WeatherVaneSettings(false, true, true, 0, false, false, ENERGY_PER_USE, 0, false, 1.0, false));
        ServerLevel level = overworld(helper);
        level.setDayTime(1000);
        vane.setSelected(WeatherEffect.NOON);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.DISABLED, "activation with enabled=false");
        helper.assertValueEqual(level.getDayTime(), 1000L, "day time unchanged");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fixedTimeAndWeatherlessDimensionsRefuse(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for(var dimension : List.of(Level.NETHER, Level.END)) {
            ServerLevel level = server.getLevel(dimension);
            helper.assertTrue(level != null, dimension.location() + " should exist on the test server");
            helper.assertValueEqual(WeatherVaneLogic.dimensionStatus(level, WeatherEffect.NOON), WeatherVaneStatus.FIXED_TIME, dimension.location() + " time");
            helper.assertValueEqual(WeatherVaneLogic.dimensionStatus(level, WeatherEffect.RAIN), WeatherVaneStatus.NO_WEATHER, dimension.location() + " weather");
            helper.assertValueEqual(WeatherVaneLogic.check(level, WeatherEffect.CLEAR, settings(false, 0), 0, WeatherVaneStatus.READY), WeatherVaneStatus.NO_WEATHER, dimension.location() + " check");
        }
        helper.assertValueEqual(WeatherVaneLogic.dimensionStatus(server.overworld(), WeatherEffect.NOON), WeatherVaneStatus.READY, "overworld time");
        helper.assertValueEqual(WeatherVaneLogic.dimensionStatus(server.overworld(), WeatherEffect.THUNDER), WeatherVaneStatus.READY, "overworld weather");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void redstoneFiresOncePerRisingEdge(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, settings(true, 0));
        vane.catalyst().setStackInSlot(0, new ItemStack(ModRegistries.SKY_SHARD.get(), 5));
        vane.setSelected(WeatherEffect.NOON);
        BlockPos power = VANE.east();
        helper.startSequence()
                .thenExecute(() -> helper.setBlock(power, Blocks.REDSTONE_BLOCK))
                .thenExecute(() -> assertShards(helper, vane, 4, "after the first rising edge"))
                .thenIdle(5)
                // Another neighbor update while still powered must not fire again.
                .thenExecute(() -> helper.setBlock(VANE.west(), Blocks.STONE))
                .thenIdle(5)
                .thenExecute(() -> assertShards(helper, vane, 4, "while held powered"))
                .thenExecute(() -> helper.setBlock(power, Blocks.AIR))
                .thenIdle(5)
                .thenExecute(() -> {
                    assertShards(helper, vane, 4, "after the falling edge");
                    helper.assertFalse(helper.getBlockState(VANE).getValue(WeatherVaneBlock.POWERED), "vane should record no power");
                })
                .thenExecute(() -> helper.setBlock(power, Blocks.REDSTONE_BLOCK))
                .thenExecute(() -> assertShards(helper, vane, 3, "after the second rising edge"))
                .thenSucceed();
    }

    private static void assertShards(GameTestHelper helper, WeatherVaneBlockEntity vane, int expected, String when) {
        helper.assertValueEqual(vane.catalyst().getStackInSlot(0).getCount(), expected, "shards " + when);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void hopperFeedsShardsButCannotTakeThem(GameTestHelper helper) {
        BlockPos vanePos = new BlockPos(4, 2, 4);
        WeatherVaneBlockEntity vane = placeVane(helper, vanePos, settings(true, 0));
        helper.setBlock(vanePos.above(), Blocks.HOPPER);
        helper.setBlock(vanePos.below(), Blocks.HOPPER);
        HopperBlockEntity top = helper.getBlockEntity(vanePos.above());
        top.setItem(0, new ItemStack(Items.STONE, 1));
        top.setItem(1, new ItemStack(ModRegistries.SKY_SHARD.get(), 2));
        helper.runAfterDelay(40, () -> {
            assertShards(helper, vane, 2, "inserted by the hopper above");
            helper.assertValueEqual(top.getItem(0).getCount(), 1, "stone must not be inserted");
            HopperBlockEntity bottom = helper.getBlockEntity(vanePos.below());
            helper.assertTrue(bottom.isEmpty(), "hopper below must not pull shards out");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void menuValidatesEveryClick(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE,
                new WeatherVaneSettings(true, true, false, 0, true, false, ENERGY_PER_USE, 0, false, 1.0, false));
        FakePlayer near = survivalPlayer(helper, ItemStack.EMPTY, new BlockPos(4, 1, 2), 0, 0);
        WeatherVaneMenu menu = new WeatherVaneMenu(1, near.getInventory(), vane);

        helper.assertTrue(menu.clickMenuButton(near, WeatherEffect.MIDNIGHT.ordinal()), "allowed effect should be selectable");
        helper.assertValueEqual(vane.selected(), WeatherEffect.MIDNIGHT, "selected effect");
        helper.assertFalse(menu.clickMenuButton(near, WeatherEffect.THUNDER.ordinal()), "weather is disabled for this vane");
        helper.assertValueEqual(vane.selected(), WeatherEffect.MIDNIGHT, "rejected click keeps the selection");
        helper.assertFalse(menu.clickMenuButton(near, 99), "unknown button id");

        FakePlayer far = survivalPlayer(helper, ItemStack.EMPTY, new BlockPos(4, 1, 4), 0, 0);
        far.moveTo(far.getX() + 30, far.getY(), far.getZ());
        WeatherVaneMenu farMenu = new WeatherVaneMenu(2, far.getInventory(), vane);
        helper.assertFalse(farMenu.clickMenuButton(far, WeatherEffect.NOON.ordinal()), "player out of reach");
        helper.assertFalse(farMenu.clickMenuButton(far, WeatherVaneMenu.ACTIVATE_BUTTON), "activation out of reach");
        helper.assertValueEqual(vane.selected(), WeatherEffect.MIDNIGHT, "out of reach click keeps the selection");

        helper.assertTrue(menu.clickMenuButton(near, WeatherVaneMenu.ACTIVATE_BUTTON), "activation from the GUI");
        helper.setBlock(VANE, Blocks.AIR);
        helper.assertFalse(menu.clickMenuButton(near, WeatherEffect.NOON.ordinal()), "block no longer present");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void announcementNamesPlayerOrOwner(GameTestHelper helper) {
        WeatherVaneBlockEntity vane = placeVane(helper, VANE, settings(false, 0));
        vane.setSelected(WeatherEffect.RAIN);
        FakePlayer player = survivalPlayer(helper, ItemStack.EMPTY, new BlockPos(4, 1, 2), 0, 0);

        TranslatableContents byPlayer = (TranslatableContents) vane.announcement(player).getContents();
        helper.assertValueEqual(byPlayer.getKey(), "message.tunnelrats_tools.weather_vane.player", "button message");
        helper.assertTrue(((Component) byPlayer.getArgs()[0]).getString().contains(player.getGameProfile().getName()), "button message names the player");

        TranslatableContents unowned = (TranslatableContents) vane.announcement(null).getContents();
        helper.assertValueEqual(unowned.getKey(), "message.tunnelrats_tools.weather_vane.redstone", "redstone message without owner");

        vane.setOwner(UUID.randomUUID(), "TunnelRat117");
        TranslatableContents owned = (TranslatableContents) vane.announcement(null).getContents();
        helper.assertValueEqual(owned.getKey(), "message.tunnelrats_tools.weather_vane.redstone_owner", "redstone message");
        helper.assertValueEqual(owned.getArgs()[0], "TunnelRat117", "redstone message names the owner");
        TranslatableContents change = (TranslatableContents) ((Component) owned.getArgs()[1]).getContents();
        helper.assertValueEqual(change.getKey(), "effect.tunnelrats_tools.rain.change", "message says what changed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "weather_vane_cooldown", timeoutTicks = 100)
    public static void cooldownIsSharedAndExpires(GameTestHelper helper) {
        WeatherVaneData.get(helper.getLevel().getServer()).reset(helper.getLevel().getServer());
        WeatherVaneBlockEntity first = placeVane(helper, VANE, settings(false, 1));
        WeatherVaneBlockEntity second = placeVane(helper, new BlockPos(2, 1, 2), settings(false, 1));
        first.setSelected(WeatherEffect.NOON);
        second.setSelected(WeatherEffect.CLEAR);
        helper.assertValueEqual(first.activate(null), WeatherVaneStatus.READY, "first activation");
        helper.assertValueEqual(first.activate(null), WeatherVaneStatus.COOLDOWN, "same vane during the cooldown");
        helper.assertValueEqual(second.activate(null), WeatherVaneStatus.COOLDOWN, "another vane during the cooldown");
        helper.runAfterDelay(21, () -> {
            helper.assertValueEqual(second.activate(null), WeatherVaneStatus.READY, "after the cooldown");
            WeatherVaneData.get(helper.getLevel().getServer()).reset(helper.getLevel().getServer());
            helper.succeed();
        });
    }
}
