package io.github.peterbax117.tunnelratstools.gametest;

import io.github.peterbax117.modtestharness.gametest.HarnessTemplates;
import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherEffect;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlockEntity;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneData;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneLogic;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneSettings;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Charge, release and time fast-forward. These span ticks and use the server-wide pending state, so each test that
 * waits runs in its own batch and resets the state at its start and end.
 */
@GameTestHolder(HarnessTemplates.NAMESPACE)
@PrefixGameTestTemplate(false)
public class WeatherVaneSequenceGameTests
{
    private static final String TEMPLATE = HarnessTemplates.EMPTY_9X5X9;
    private static final BlockPos VANE = new BlockPos(4, 1, 4);

    static WeatherVaneSettings sequence(double chargeSeconds, boolean animateTime, double animationSeconds, boolean showEffects) {
        return new WeatherVaneSettings(true, true, true, 0, false, false, 1000, chargeSeconds, animateTime, animationSeconds, showEffects);
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static void resetState(GameTestHelper helper) {
        WeatherVaneData.get(server(helper)).reset(server(helper));
    }

    private static void setDaylightCycle(GameTestHelper helper, boolean on) {
        helper.getLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(on, server(helper));
    }

    /** Records the day time every tick after activation, indexed by ticks since activation. */
    private static List<Long> recordDayTime(GameTestHelper helper, long activationGameTime) {
        List<Long> values = new ArrayList<>();
        helper.onEachTick(() -> {
            int k = (int) (helper.getLevel().getGameTime() - activationGameTime);
            while(values.size() <= k) values.add(null);
            values.set(k, helper.getLevel().getDayTime());
        });
        return values;
    }

    @GameTest(template = TEMPLATE, batch = "vane_fast_forward", timeoutTicks = 60)
    public static void fastForwardRisesEveryTickAndLandsOnTarget(GameTestHelper helper) {
        resetState(helper);
        setDaylightCycle(helper, false);
        ServerLevel level = helper.getLevel();
        long start = 5 * 24000L + 10000L, target = 6 * 24000L + 6000L;
        level.setDayTime(start);
        WeatherVaneBlockEntity vane = WeatherVaneGameTests.placeVane(helper, VANE, sequence(0, true, 0.5, false));
        vane.setSelected(WeatherEffect.NOON);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "activation");
        helper.assertValueEqual(level.getDayTime(), start, "time does not jump at activation");
        long activation = level.getGameTime();
        List<Long> values = recordDayTime(helper, activation);

        int duration = 10; // 0.5 seconds
        helper.runAfterDelay(duration + 5, () -> {
            setDaylightCycle(helper, true);
            long previous = start;
            for(int k = 1; k < duration; k++) {
                Long value = values.get(k);
                helper.assertTrue(value != null && value > previous && value < target,
                        "tick " + k + ": day time " + value + " should rise from " + previous + " toward " + target);
                previous = value;
            }
            for(int k = duration; k < values.size(); k++) {
                helper.assertValueEqual(values.get(k), target, "day time at tick " + k);
            }
            resetState(helper);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "vane_fast_forward_day", timeoutTicks = 60)
    public static void fastForwardKeepsDayCount(GameTestHelper helper) {
        resetState(helper);
        setDaylightCycle(helper, false);
        ServerLevel level = helper.getLevel();
        // Just after sunrise on day 5: the next sunrise is on day 6, almost a full day ahead.
        long start = 5 * 24000L + 23500L, target = 6 * 24000L + 23000L;
        level.setDayTime(start);
        WeatherVaneBlockEntity vane = WeatherVaneGameTests.placeVane(helper, VANE, sequence(0, true, 0.5, false));
        vane.setSelected(WeatherEffect.SUNRISE);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "activation");
        List<Long> values = recordDayTime(helper, level.getGameTime());
        helper.runAfterDelay(15, () -> {
            setDaylightCycle(helper, true);
            for(Long value : values) {
                if(value == null) continue;
                helper.assertTrue(value >= start && value / 24000 >= start / 24000, "day time " + value + " went below the start " + start);
            }
            helper.assertValueEqual(level.getDayTime(), target, "final day time");
            helper.assertValueEqual(level.getDayTime() / 24000, 6L, "day count after the fast-forward");
            resetState(helper);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void animateTimeFalseIsInstant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(1000);
        WeatherVaneBlockEntity vane = WeatherVaneGameTests.placeVane(helper, VANE, sequence(0, false, 3.0, false));
        vane.setSelected(WeatherEffect.SUNSET);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "activation");
        helper.assertValueEqual(level.getDayTime(), 12000L, "day time right after activation");
        helper.assertFalse(WeatherVaneData.get(server(helper)).animating(), "no fast-forward should run");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargeSecondsZeroIsImmediate(GameTestHelper helper) {
        RainDetectorGameTests.setWeather(helper.getLevel(), RainDetectorGameTests.Weather.CLEAR);
        WeatherVaneBlockEntity vane = WeatherVaneGameTests.placeVane(helper, VANE, sequence(0, false, 3.0, false));
        vane.setSelected(WeatherEffect.RAIN);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "activation");
        helper.assertTrue(helper.getLevel().getLevelData().isRaining(), "rain should start inside activate()");
        helper.assertFalse(WeatherVaneData.get(server(helper)).charging(), "no charge should be pending");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "vane_charge", timeoutTicks = 60)
    public static void chargeDelaysTheEffect(GameTestHelper helper) {
        resetState(helper);
        ServerLevel level = helper.getLevel();
        RainDetectorGameTests.setWeather(level, RainDetectorGameTests.Weather.RAIN);
        WeatherVaneSettings settings = new WeatherVaneSettings(true, true, true, 0, false, true, 1000, 0.5, false, 1.0, false);
        WeatherVaneBlockEntity vane = WeatherVaneGameTests.placeVane(helper, VANE, settings);
        vane.catalyst().setStackInSlot(0, new ItemStack(ModRegistries.SKY_SHARD.get(), 1));
        vane.setSelected(WeatherEffect.CLEAR);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "activation");
        helper.assertTrue(vane.catalyst().getStackInSlot(0).isEmpty(), "the shard is paid when the charge starts");
        helper.assertTrue(level.getLevelData().isRaining(), "rain continues while charging");

        long activation = level.getGameTime();
        int duration = 10; // 0.5 seconds
        List<Boolean> raining = new ArrayList<>();
        helper.onEachTick(() -> {
            int k = (int) (level.getGameTime() - activation);
            while(raining.size() <= k) raining.add(null);
            raining.set(k, level.getLevelData().isRaining());
        });
        helper.runAfterDelay(duration + 4, () -> {
            for(int k = 1; k < raining.size(); k++) {
                if(raining.get(k) == null) continue;
                helper.assertValueEqual(raining.get(k), k < duration, "raining " + k + " ticks after activation");
            }
            resetState(helper);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "vane_busy", timeoutTicks = 60)
    public static void busyWhileChargingAndChangingTheSky(GameTestHelper helper) {
        resetState(helper);
        WeatherVaneBlockEntity first = WeatherVaneGameTests.placeVane(helper, VANE, sequence(0.5, true, 0.5, false));
        WeatherVaneBlockEntity second = WeatherVaneGameTests.placeVane(helper, new BlockPos(2, 1, 2), sequence(0, false, 1.0, false));
        first.setSelected(WeatherEffect.NOON);
        second.setSelected(WeatherEffect.CLEAR);
        helper.assertValueEqual(first.activate(null), WeatherVaneStatus.READY, "first activation");
        helper.assertValueEqual(second.activate(null), WeatherVaneStatus.CHARGING, "another vane while charging");
        helper.assertValueEqual(first.activate(null), WeatherVaneStatus.CHARGING, "the same vane while charging");
        // Charge: 10 ticks, then the fast-forward: 10 ticks.
        helper.runAfterDelay(14, () -> helper.assertValueEqual(second.activate(null), WeatherVaneStatus.ANIMATING, "another vane during the fast-forward"));
        helper.runAfterDelay(25, () -> {
            helper.assertValueEqual(second.activate(null), WeatherVaneStatus.READY, "after the fast-forward");
            resetState(helper);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "vane_break", timeoutTicks = 60)
    public static void brokenVaneStillReleasesPaidEffect(GameTestHelper helper) {
        resetState(helper);
        ServerLevel level = helper.getLevel();
        RainDetectorGameTests.setWeather(level, RainDetectorGameTests.Weather.RAIN);
        WeatherVaneSettings settings = new WeatherVaneSettings(true, true, true, 0, false, true, 1000, 0.5, false, 1.0, true);
        WeatherVaneBlockEntity vane = WeatherVaneGameTests.placeVane(helper, VANE, settings);
        vane.catalyst().setStackInSlot(0, new ItemStack(ModRegistries.SKY_SHARD.get(), 2));
        vane.setSelected(WeatherEffect.CLEAR);
        helper.assertValueEqual(vane.activate(null), WeatherVaneStatus.READY, "activation");
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.setBlock(VANE, Blocks.AIR))
                .thenExecute(() -> helper.assertItemEntityPresent(ModRegistries.SKY_SHARD.get(), VANE, 2.0))
                .thenExecuteAfter(12, () -> {
                    helper.assertFalse(level.getLevelData().isRaining(), "the paid effect applies although the vane was broken");
                    helper.assertValueEqual(WeatherVaneData.get(server(helper)).busyStatus(), WeatherVaneStatus.READY, "nothing pending");
                    resetState(helper);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void interruptedFastForwardFinishesOnLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registries = level.registryAccess();
        WeatherVaneData before = WeatherVaneData.load(new CompoundTag(), registries);
        long from = 3 * 24000L + 1000L, to = 3 * 24000L + 18000L;
        before.startAnimation(from, to, 60);
        CompoundTag saved = before.save(new CompoundTag(), registries);

        WeatherVaneData after = WeatherVaneData.load(saved, registries);
        helper.assertValueEqual(after.busyStatus(), WeatherVaneStatus.ANIMATING, "loaded state");
        level.setDayTime(from + 500);
        after.tick(server(helper));
        helper.assertValueEqual(level.getDayTime(), to, "day time after the first tick following a load");
        helper.assertValueEqual(after.busyStatus(), WeatherVaneStatus.READY, "nothing pending after recovery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void interruptedChargeAppliesOnLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registries = level.registryAccess();
        WeatherVaneData before = WeatherVaneData.load(new CompoundTag(), registries);
        before.begin(level, helper.absolutePos(VANE), WeatherEffect.MIDNIGHT, sequence(5.0, true, 3.0, false));
        helper.assertValueEqual(before.busyStatus(), WeatherVaneStatus.CHARGING, "charge started");
        CompoundTag saved = before.save(new CompoundTag(), registries);

        WeatherVaneData after = WeatherVaneData.load(saved, registries);
        helper.assertValueEqual(after.busyStatus(), WeatherVaneStatus.CHARGING, "loaded state");
        long now = 2 * 24000L + 4000L;
        level.setDayTime(now);
        after.tick(server(helper));
        helper.assertValueEqual(level.getDayTime(), WeatherVaneLogic.nextDayTime(now, 18000), "the paid effect applies at once");
        helper.assertValueEqual(after.busyStatus(), WeatherVaneStatus.READY, "no fast-forward after recovery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "vane_effects", timeoutTicks = 120)
    public static void effectsRunWithShowEffectsOnAndOff(GameTestHelper helper) {
        resetState(helper);
        ServerLevel level = helper.getLevel();
        RainDetectorGameTests.setWeather(level, RainDetectorGameTests.Weather.CLEAR);
        WeatherVaneBlockEntity shown = WeatherVaneGameTests.placeVane(helper, VANE, sequence(0.25, true, 0.5, true));
        WeatherVaneBlockEntity hidden = WeatherVaneGameTests.placeVane(helper, new BlockPos(2, 1, 2), sequence(0.25, true, 0.5, false));
        shown.setSelected(WeatherEffect.THUNDER);
        hidden.setSelected(WeatherEffect.MIDNIGHT);
        WeatherVaneData data = WeatherVaneData.get(server(helper));
        helper.startSequence()
                .thenExecute(() -> helper.assertValueEqual(shown.activate(null), WeatherVaneStatus.READY, "activation with effects"))
                // Charge (5 ticks) and the release beam (20 ticks) with particles and sounds.
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(level.getLevelData().isThundering(), "thunder released with effects on");
                    helper.assertValueEqual(data.busyStatus(), WeatherVaneStatus.READY, "nothing pending");
                    helper.assertValueEqual(hidden.activate(null), WeatherVaneStatus.READY, "activation without effects");
                })
                .thenExecuteAfter(20, () -> {
                    // The daylight cycle runs on the test server, so allow the few ticks since the fast-forward ended.
                    long timeOfDay = level.getDayTime() % 24000;
                    helper.assertTrue(timeOfDay >= 18000 && timeOfDay < 18100, "midnight released with effects off, time of day " + timeOfDay);
                    helper.assertValueEqual(data.busyStatus(), WeatherVaneStatus.READY, "nothing pending");
                    resetState(helper);
                })
                .thenSucceed();
    }
}
