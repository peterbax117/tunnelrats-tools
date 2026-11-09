package io.github.peterbax117.tunnelratstools.gametest;

import io.github.peterbax117.modtestharness.gametest.HarnessTemplates;
import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.TunnelRatsConfig;
import io.github.peterbax117.tunnelratstools.raindetector.RainDetectorBlock;
import io.github.peterbax117.tunnelratstools.raindetector.RainDetectorMode;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherEffect;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlock;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

import static io.github.peterbax117.modtestharness.gametest.GameTestSupport.survivalPlayer;

/**
 * Rain Detector tests. The detector reads the server-wide weather, so tests that wait for ticks run in their own
 * batch; the others set the weather and check the output in the same tick.
 */
@GameTestHolder(HarnessTemplates.NAMESPACE)
@PrefixGameTestTemplate(false)
public class RainDetectorGameTests
{
    private static final String TEMPLATE = HarnessTemplates.EMPTY_9X5X9;
    private static final BlockPos DETECTOR = new BlockPos(4, 1, 4);

    enum Weather { CLEAR, RAIN, THUNDER }

    /** Sets the weather and its visible strength at once, as if it had been that way for a while. */
    static void setWeather(ServerLevel level, Weather weather) {
        boolean rain = weather != Weather.CLEAR, thunder = weather == Weather.THUNDER;
        level.setWeatherParameters(rain ? 0 : 6000, rain ? 6000 : 0, rain, thunder);
        level.setRainLevel(rain ? 1 : 0);
        level.setThunderLevel(thunder ? 1 : 0);
    }

    private static BlockState detector(RainDetectorMode mode) {
        return ModRegistries.RAIN_DETECTOR.get().defaultBlockState().setValue(RainDetectorBlock.MODE, mode);
    }

    private static int output(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getSignal(helper.absolutePos(pos), Direction.NORTH);
    }

    private static void update(GameTestHelper helper, BlockPos pos) {
        RainDetectorBlock.updateSignal(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos));
    }

    @GameTest(template = TEMPLATE)
    public static void eachModeOutputForEachWeather(GameTestHelper helper) {
        Map<Weather, Map<RainDetectorMode, Integer>> expected = Map.of(
                Weather.CLEAR, Map.of(RainDetectorMode.RAIN, 0, RainDetectorMode.THUNDER, 0, RainDetectorMode.CLEAR, 15),
                Weather.RAIN, Map.of(RainDetectorMode.RAIN, 15, RainDetectorMode.THUNDER, 0, RainDetectorMode.CLEAR, 0),
                Weather.THUNDER, Map.of(RainDetectorMode.RAIN, 15, RainDetectorMode.THUNDER, 15, RainDetectorMode.CLEAR, 0));
        for(var weather : expected.entrySet()) {
            setWeather(helper.getLevel(), weather.getKey());
            for(var mode : weather.getValue().entrySet()) {
                helper.setBlock(DETECTOR, detector(mode.getKey()));
                update(helper, DETECTOR);
                helper.assertValueEqual(output(helper, DETECTOR), mode.getValue(), mode.getKey() + " mode in " + weather.getKey());
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rightClickCyclesModes(GameTestHelper helper) {
        setWeather(helper.getLevel(), Weather.CLEAR);
        helper.setBlock(DETECTOR, detector(RainDetectorMode.RAIN));
        FakePlayer player = survivalPlayer(helper, ItemStack.EMPTY, new BlockPos(4, 1, 2), 0, 0);
        for(RainDetectorMode next : List.of(RainDetectorMode.THUNDER, RainDetectorMode.CLEAR, RainDetectorMode.RAIN)) {
            helper.useBlock(DETECTOR, player);
            helper.assertValueEqual(helper.getBlockState(DETECTOR).getValue(RainDetectorBlock.MODE), next, "mode after right-click");
            // The output follows the new mode at once: in clear weather only CLEAR mode is on.
            helper.assertValueEqual(output(helper, DETECTOR), next == RainDetectorMode.CLEAR ? 15 : 0, next + " mode output");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void disabledDetectorOutputsNothing(GameTestHelper helper) {
        setWeather(helper.getLevel(), Weather.RAIN);
        helper.setBlock(DETECTOR, detector(RainDetectorMode.RAIN));
        TunnelRatsConfig.RAIN_DETECTOR_ENABLED.set(false);
        try {
            update(helper, DETECTOR);
            helper.assertValueEqual(output(helper, DETECTOR), 0, "disabled detector output in rain");
        } finally {
            TunnelRatsConfig.RAIN_DETECTOR_ENABLED.set(true);
        }
        update(helper, DETECTOR);
        helper.assertValueEqual(output(helper, DETECTOR), 15, "enabled again");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void noOutputWithoutWeather(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for(var dimension : List.of(Level.NETHER, Level.END)) {
            ServerLevel level = server.getLevel(dimension);
            helper.assertTrue(level != null, dimension.location() + " should exist on the test server");
            for(RainDetectorMode mode : RainDetectorMode.values()) {
                helper.assertValueEqual(RainDetectorBlock.signal(level, mode), 0, mode + " mode in " + dimension.location());
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "rain_detector_neighbors")
    public static void weatherChangeUpdatesNeighbors(GameTestHelper helper) {
        setWeather(helper.getLevel(), Weather.CLEAR);
        helper.setBlock(DETECTOR, detector(RainDetectorMode.RAIN));
        BlockPos lamp = DETECTOR.east();
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        helper.startSequence()
                .thenExecute(() -> {
                    setWeather(helper.getLevel(), Weather.RAIN);
                    update(helper, DETECTOR);
                    helper.assertTrue(helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp should light when rain starts");
                    setWeather(helper.getLevel(), Weather.CLEAR);
                    update(helper, DETECTOR);
                })
                // A redstone lamp turns off 4 ticks after losing power.
                .thenExecuteAfter(6, () -> helper.assertFalse(helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "lamp should go off when rain stops"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "rain_detector_polling", timeoutTicks = 80)
    public static void pollingPicksUpWeatherChange(GameTestHelper helper) {
        setWeather(helper.getLevel(), Weather.CLEAR);
        helper.setBlock(DETECTOR, detector(RainDetectorMode.RAIN));
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> {
                    helper.assertValueEqual(output(helper, DETECTOR), 0, "output before rain");
                    setWeather(helper.getLevel(), Weather.RAIN);
                })
                .thenWaitUntil(() -> helper.assertValueEqual(output(helper, DETECTOR), 15, "output once the detector polls"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, batch = "rain_detector_weather_vane", timeoutTicks = 400)
    public static void rainDetectorDrivesWeatherVaneToClear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        setWeather(level, Weather.RAIN);
        BlockPos vanePos = new BlockPos(3, 1, 4);
        // Cooldown 0, so a second trigger would succeed if the falling edge fired one.
        WeatherVaneBlockEntity vane = WeatherVaneGameTests.placeVane(helper, vanePos, WeatherVaneGameTests.settings(true, 0));
        vane.catalyst().setStackInSlot(0, new ItemStack(ModRegistries.SKY_SHARD.get(), 2));
        vane.setSelected(WeatherEffect.CLEAR);
        helper.setBlock(vanePos.east(), detector(RainDetectorMode.RAIN));
        BlockPos detectorPos = vanePos.east();

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(vane.catalyst().getStackInSlot(0).getCount(), 1, "shards after the detector fired"))
                .thenExecute(() -> helper.assertFalse(level.getLevelData().isRaining(), "the vane should have cleared the weather"))
                // Rain fades over a few seconds; the detector turns off when it does (the falling edge).
                .thenWaitUntil(() -> helper.assertValueEqual(output(helper, detectorPos), 0, "detector output after the rain stops"))
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertValueEqual(vane.catalyst().getStackInSlot(0).getCount(), 1, "shards: the falling edge must not fire");
                    helper.assertFalse(helper.getBlockState(vanePos).getValue(WeatherVaneBlock.POWERED), "vane should record no power");
                    helper.assertFalse(level.getLevelData().isRaining(), "weather should stay clear");
                })
                .thenSucceed();
    }
}
