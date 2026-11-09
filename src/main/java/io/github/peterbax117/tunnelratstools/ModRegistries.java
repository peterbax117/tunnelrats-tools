package io.github.peterbax117.tunnelratstools;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All registries for the mod. Empty for now; the planned entries are the time and weather control block (block,
 * item, block entity, menu, catalyst item) and the storage crate (block per tier, items, block entity, menu,
 * chest-to-crate upgrade item).
 *
 * Design rule: everything is always registered, even when a tool is disabled in {@link TunnelRatsConfig}.
 * Removing registry entries would make existing worlds that contain the blocks fail to load or lose them.
 * A disabled tool keeps its entries; its recipes and its use are turned off instead.
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

    // Lists every item of the mod. The icon is a vanilla placeholder until the mod has its own items.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TunnelRatsTools.MODID))
                    .icon(() -> new ItemStack(Items.CLOCK))
                    .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    private ModRegistries() {}

    static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
