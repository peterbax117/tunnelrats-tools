package io.github.peterbax117.tunnelratstools;

import com.mojang.serialization.MapCodec;
import io.github.peterbax117.tunnelratstools.raindetector.RainDetectorBlock;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlock;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneBlockEntity;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * All registries for the mod. Names and ids live here and in the lang file only.
 *
 * Design rule: everything is always registered, even when a tool is disabled in {@link TunnelRatsConfig}.
 * Removing registry entries would make existing worlds that contain the blocks fail to load or lose them.
 * A disabled tool keeps its entries; its recipes ({@link ToolEnabledCondition}) and its use are turned off instead.
 */
public final class ModRegistries
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TunnelRatsTools.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TunnelRatsTools.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TunnelRatsTools.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TunnelRatsTools.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TunnelRatsTools.MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, TunnelRatsTools.MODID);

    // Weather Vane: time and weather control block, and its catalyst.
    public static final DeferredBlock<WeatherVaneBlock> WEATHER_VANE = BLOCKS.register("weather_vane",
            () -> new WeatherVaneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.COPPER)));
    public static final DeferredItem<BlockItem> WEATHER_VANE_ITEM = ITEMS.registerSimpleBlockItem(WEATHER_VANE,
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SKY_SHARD = ITEMS.registerSimpleItem("sky_shard", new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeatherVaneBlockEntity>> WEATHER_VANE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("weather_vane", () -> BlockEntityType.Builder.of(WeatherVaneBlockEntity::new, WEATHER_VANE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<WeatherVaneMenu>> WEATHER_VANE_MENU =
            MENUS.register("weather_vane", () -> IMenuTypeExtension.create(WeatherVaneMenu::new));

    // Rain Detector: redstone signal from the weather.
    public static final DeferredBlock<RainDetectorBlock> RAIN_DETECTOR = BLOCKS.register("rain_detector",
            () -> new RainDetectorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.5F)
                    .sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> RAIN_DETECTOR_ITEM = ITEMS.registerSimpleBlockItem(RAIN_DETECTOR);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ToolEnabledCondition>> TOOL_ENABLED_CONDITION =
            CONDITION_CODECS.register("tool_enabled", () -> ToolEnabledCondition.CODEC);

    // Lists every item of the mod.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TunnelRatsTools.MODID))
                    .icon(() -> new ItemStack(WEATHER_VANE_ITEM.get()))
                    .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    private ModRegistries() {}

    static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        CONDITION_CODECS.register(modEventBus);
        modEventBus.addListener(ModRegistries::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WEATHER_VANE_BLOCK_ENTITY.get(),
                (vane, side) -> vane.automationCatalyst());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WEATHER_VANE_BLOCK_ENTITY.get(),
                (vane, side) -> vane.energy());
    }
}
