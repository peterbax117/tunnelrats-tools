package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Weather Vane GUI. The client sends only vanilla menu button clicks (ids 0 to 6 select an effect, 7 activates);
 * the server checks every click again here, so a modified client cannot pick a disallowed effect or fire it.
 */
public class WeatherVaneMenu extends AbstractContainerMenu
{
    public static final int ACTIVATE_BUTTON = WeatherEffect.values().length;

    static final int DATA_SELECTED = 0;
    static final int DATA_STATUS = 1;
    static final int DATA_FLAGS = 2;
    static final int DATA_FIRST_PAIR = 3;
    private static final int PAIR_ENERGY = 0, PAIR_CAPACITY = 1, PAIR_COST = 2, PAIR_COOLDOWN = 3;
    static final int DATA_COUNT = DATA_FIRST_PAIR + 2 * 4;

    private static final int FLAG_ENABLED = 1, FLAG_TIME = 2, FLAG_WEATHER = 4, FLAG_CATALYST = 8;

    public static final int CATALYST_X = 8, CATALYST_Y = 59;
    public static final int INVENTORY_Y = 114;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable WeatherVaneBlockEntity blockEntity;

    /** Client side, from the open-screen packet. */
    public WeatherVaneMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), WeatherVaneBlockEntity.newCatalystHandler(() -> {}),
                new SimpleContainerData(DATA_COUNT), null);
    }

    /** Server side. */
    public WeatherVaneMenu(int containerId, Inventory inventory, WeatherVaneBlockEntity blockEntity) {
        this(containerId, inventory, blockEntity.getBlockPos(), blockEntity.catalyst(), blockEntity.menuData(), blockEntity);
    }

    private WeatherVaneMenu(int containerId, Inventory inventory, BlockPos pos, IItemHandler catalyst, ContainerData data,
                            @Nullable WeatherVaneBlockEntity blockEntity) {
        super(ModRegistries.WEATHER_VANE_MENU.get(), containerId);
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        this.data = data;
        this.blockEntity = blockEntity;

        addSlot(new SlotItemHandler(catalyst, 0, CATALYST_X, CATALYST_Y));
        for(int row = 0; row < 3; row++) {
            for(int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for(int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if(blockEntity == null || !(player instanceof ServerPlayer serverPlayer)) return false;
        if(!stillValid(player) || blockEntity.isRemoved()
                || blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) != blockEntity) return false;
        if(id == ACTIVATE_BUTTON) return blockEntity.activate(serverPlayer) == WeatherVaneStatus.READY;
        WeatherEffect effect = WeatherEffect.byIndex(id);
        if(effect == null || !blockEntity.settings().allows(effect)) return false;
        blockEntity.setSelected(effect);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModRegistries.WEATHER_VANE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = 1, playerEnd = slots.size();
        if(index == 0) {
            if(!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else if(slots.get(0).mayPlace(stack)) {
            if(!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    // Values for the screen (client side copies of the server's data).

    public WeatherEffect selected() {
        WeatherEffect effect = WeatherEffect.byIndex(data.get(DATA_SELECTED));
        return effect != null ? effect : WeatherEffect.CLEAR;
    }

    public WeatherVaneStatus status() {
        return WeatherVaneStatus.byIndex(data.get(DATA_STATUS));
    }

    public boolean allows(WeatherEffect effect) {
        int flags = data.get(DATA_FLAGS);
        return (flags & FLAG_ENABLED) != 0 && (flags & (effect.isTime() ? FLAG_TIME : FLAG_WEATHER)) != 0;
    }

    public boolean requiresCatalyst() {
        return (data.get(DATA_FLAGS) & FLAG_CATALYST) != 0;
    }

    public int energy() {
        return pair(PAIR_ENERGY);
    }

    public int energyCapacity() {
        return pair(PAIR_CAPACITY);
    }

    public int energyPerUse() {
        return pair(PAIR_COST);
    }

    public int cooldownSeconds() {
        return pair(PAIR_COOLDOWN);
    }

    private int pair(int pair) {
        int index = DATA_FIRST_PAIR + pair * 2;
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) & 0xFFFF) << 16;
    }

    static int half(int value, boolean high) {
        return high ? (value >>> 16) & 0xFFFF : value & 0xFFFF;
    }

    static int flags(WeatherVaneSettings settings) {
        return (settings.enabled() ? FLAG_ENABLED : 0) | (settings.allowTime() ? FLAG_TIME : 0)
                | (settings.allowWeather() ? FLAG_WEATHER : 0) | (settings.requireCatalyst() ? FLAG_CATALYST : 0);
    }
}
