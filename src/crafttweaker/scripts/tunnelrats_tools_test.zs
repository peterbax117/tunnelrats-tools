// Sample pack-maker script for the -Pcrafttweaker test run: replaces the Sky Shard recipe with a different one.
// Checked by RecipeGameTests.skyShardRecipe and SmokeGameTests.craftTweakerScriptsRun.
craftingTable.remove(<item:tunnelrats_tools:sky_shard>);
craftingTable.addShapeless("tunnelrats_tools_easy_sky_shard", <item:tunnelrats_tools:sky_shard>, [<item:minecraft:feather>, <item:minecraft:gold_block>]);
