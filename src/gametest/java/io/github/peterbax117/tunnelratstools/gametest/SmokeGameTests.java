package io.github.peterbax117.tunnelratstools.gametest;

import io.github.peterbax117.modtestharness.gametest.HarnessTemplates;
import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.TunnelRatsConfig;
import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Server-side smoke tests. They pass in every test setup: standalone, integrations and crafttweaker. */
@GameTestHolder(HarnessTemplates.NAMESPACE)
@PrefixGameTestTemplate(false)
public class SmokeGameTests
{
    private static final String TEMPLATE = HarnessTemplates.EMPTY_9X5X9;

    @GameTest(template = TEMPLATE)
    public static void modIsLoadedWithItsRegistries(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(TunnelRatsTools.MODID), "Mod should be loaded");
        helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(ModRegistries.MAIN_TAB.getId()),
                "Creative tab " + ModRegistries.MAIN_TAB.getId() + " should be registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void commonConfigHasAgreedDefaults(GameTestHelper helper) {
        helper.assertTrue(TunnelRatsConfig.SPEC.isLoaded(), "Common config should be loaded");

        helper.assertValueEqual(TunnelRatsConfig.TIME_WEATHER_ENABLED.getDefault(), true, "timeWeather.enabled default");
        helper.assertValueEqual(TunnelRatsConfig.TIME_WEATHER_ALLOW_TIME.getDefault(), true, "timeWeather.allowTime default");
        helper.assertValueEqual(TunnelRatsConfig.TIME_WEATHER_ALLOW_WEATHER.getDefault(), true, "timeWeather.allowWeather default");
        helper.assertValueEqual(TunnelRatsConfig.TIME_WEATHER_COOLDOWN_SECONDS.getDefault(), 60, "timeWeather.cooldownSeconds default");
        helper.assertValueEqual(TunnelRatsConfig.TIME_WEATHER_ANNOUNCE_IN_CHAT.getDefault(), true, "timeWeather.announceInChat default");
        helper.assertValueEqual(TunnelRatsConfig.TIME_WEATHER_REQUIRE_CATALYST.getDefault(), true, "timeWeather.requireCatalyst default");
        helper.assertValueEqual(TunnelRatsConfig.CRATE_ENABLED.getDefault(), true, "crate.enabled default");

        // The loaded values are readable at runtime (a local config file may have changed them).
        helper.assertTrue(TunnelRatsConfig.TIME_WEATHER_COOLDOWN_SECONDS.get() >= 0, "cooldownSeconds should not be negative");
        TunnelRatsConfig.CRATE_ENABLED.get();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void optionalModsMatchTestSetup(GameTestHelper helper) {
        // Set by build.gradle from -Pstandalone / -Pcrafttweaker. Guards the standalone-first rule: the standalone
        // run must have no optional mods, or it would not test the configuration every player has.
        String setup = System.getProperty("tunnelrats_tools.test.setup", "unknown");
        ModList mods = ModList.get();
        switch(setup) {
            case "standalone" -> {
                for(String mod : List.of("jei", "jade", "crafttweaker")) {
                    helper.assertFalse(mods.isLoaded(mod), mod + " must not be loaded in the standalone setup");
                }
            }
            case "integrations" -> {
                helper.assertTrue(mods.isLoaded("jei"), "JEI should be loaded in the integrations setup");
                helper.assertTrue(mods.isLoaded("jade"), "Jade should be loaded in the integrations setup");
            }
            case "crafttweaker" -> helper.assertTrue(mods.isLoaded("crafttweaker"), "CraftTweaker should be loaded");
            default -> helper.fail("Unknown test setup '" + setup + "'");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void craftTweakerScriptsRun(GameTestHelper helper) {
        // src/crafttweaker/scripts/tunnelrats_tools_test.zs adds feather + gold block -> diamond. Without CraftTweaker
        // that combination must not craft anything. Replace the probe with real recipe changes once the mod has items.
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.FEATHER), new ItemStack(Items.GOLD_BLOCK)));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        boolean crafttweaker = ModList.get().isLoaded("crafttweaker");
        helper.assertValueEqual(recipe.isPresent(), crafttweaker, "feather + gold block recipe present");
        if(crafttweaker) {
            ItemStack result = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(Items.DIAMOND), "Script recipe should make a diamond, made " + result);
        }
        helper.succeed();
    }
}
