package io.github.peterbax117.tunnelratstools.gametest;

import io.github.peterbax117.modtestharness.gametest.HarnessTemplates;
import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.ToolEnabledCondition;
import io.github.peterbax117.tunnelratstools.TunnelRatsConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Recipes craft with vanilla items, and the tool_enabled condition follows the config. */
@GameTestHolder(HarnessTemplates.NAMESPACE)
@PrefixGameTestTemplate(false)
public class RecipeGameTests
{
    private static final String TEMPLATE = HarnessTemplates.EMPTY_9X5X9;

    private static ItemStack craft(GameTestHelper helper, Item... grid) {
        List<ItemStack> stacks = new ArrayList<>();
        for(Item item : grid) stacks.add(item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
        CraftingInput input = CraftingInput.of(3, 3, stacks);
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    @GameTest(template = TEMPLATE)
    public static void weatherVaneRecipe(GameTestHelper helper) {
        ItemStack result = craft(helper,
                Items.GOLD_INGOT, Items.DAYLIGHT_DETECTOR, Items.GOLD_INGOT,
                Items.LAPIS_BLOCK, Items.CLOCK, Items.LAPIS_BLOCK,
                Items.OBSIDIAN, Items.DIAMOND, Items.OBSIDIAN);
        helper.assertTrue(result.is(ModRegistries.WEATHER_VANE_ITEM.get()), "Recipe should make a Weather Vane, made " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void skyShardRecipe(GameTestHelper helper) {
        ItemStack result = craft(helper,
                Items.GLOWSTONE_DUST, Items.LAPIS_LAZULI, Items.GLOWSTONE_DUST,
                Items.LAPIS_LAZULI, Items.AMETHYST_SHARD, Items.LAPIS_LAZULI,
                Items.GLOWSTONE_DUST, Items.LAPIS_LAZULI, Items.GLOWSTONE_DUST);
        if(ModList.get().isLoaded("crafttweaker")) {
            // The -Pcrafttweaker run's sample script replaces this recipe.
            helper.assertTrue(result.isEmpty(), "CraftTweaker script should have removed the Sky Shard recipe");
        } else {
            helper.assertTrue(result.is(ModRegistries.SKY_SHARD.get()) && result.getCount() == 2, "Recipe should make 2 Sky Shards, made " + result);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rainDetectorRecipe(GameTestHelper helper) {
        ItemStack result = craft(helper,
                Items.GLASS, Items.GLASS, Items.GLASS,
                Items.REDSTONE, Items.WATER_BUCKET, Items.REDSTONE,
                Items.SMOOTH_STONE_SLAB, Items.SMOOTH_STONE_SLAB, Items.SMOOTH_STONE_SLAB);
        helper.assertTrue(result.is(ModRegistries.RAIN_DETECTOR_ITEM.get()), "Recipe should make a Rain Detector, made " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolConditionFollowsConfig(GameTestHelper helper) {
        var timeWeather = new ToolEnabledCondition(ToolEnabledCondition.Tool.TIME_WEATHER);
        var rainDetector = new ToolEnabledCondition(ToolEnabledCondition.Tool.RAIN_DETECTOR);
        helper.assertTrue(timeWeather.test(ICondition.IContext.EMPTY), "time_weather enabled by default");
        helper.assertTrue(rainDetector.test(ICondition.IContext.EMPTY), "rain_detector enabled by default");
        TunnelRatsConfig.TIME_WEATHER_ENABLED.set(false);
        TunnelRatsConfig.RAIN_DETECTOR_ENABLED.set(false);
        try {
            helper.assertFalse(timeWeather.test(ICondition.IContext.EMPTY), "time_weather disabled");
            helper.assertFalse(rainDetector.test(ICondition.IContext.EMPTY), "rain_detector disabled");
        } finally {
            TunnelRatsConfig.TIME_WEATHER_ENABLED.set(true);
            TunnelRatsConfig.RAIN_DETECTOR_ENABLED.set(true);
        }
        helper.succeed();
    }
}
