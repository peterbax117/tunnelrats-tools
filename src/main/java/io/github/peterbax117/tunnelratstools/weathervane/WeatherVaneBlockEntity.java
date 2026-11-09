package io.github.peterbax117.tunnelratstools.weathervane;

import io.github.peterbax117.tunnelratstools.ModRegistries;
import io.github.peterbax117.tunnelratstools.TunnelRatsConfig;
import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WeatherVaneBlockEntity extends BlockEntity implements MenuProvider
{
    private WeatherEffect selected = WeatherEffect.CLEAR;
    private @Nullable UUID ownerId;
    private @Nullable String ownerName;
    private final ItemStackHandler catalyst = newCatalystHandler(this::setChanged);
    private final IItemHandler automationCatalyst = new InsertOnly(catalyst);
    private final Energy energy = new Energy(Math.max(0, TunnelRatsConfig.value(TunnelRatsConfig.TIME_WEATHER_ENERGY_CAPACITY)));
    // Replaces the config for this block only; set by tests. Not saved.
    private @Nullable WeatherVaneSettings settingsOverride;
    private final ContainerData menuData = new MenuData();

    public WeatherVaneBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.WEATHER_VANE_BLOCK_ENTITY.get(), pos, state);
    }

    /** The one catalyst slot: Sky Shards only. */
    public static ItemStackHandler newCatalystHandler(Runnable onChange) {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(ModRegistries.SKY_SHARD.get());
            }

            @Override
            protected void onContentsChanged(int slot) {
                onChange.run();
            }
        };
    }

    public WeatherVaneSettings settings() {
        return settingsOverride != null ? settingsOverride : WeatherVaneSettings.fromConfig();
    }

    public void setSettingsOverride(@Nullable WeatherVaneSettings settings) {
        this.settingsOverride = settings;
    }

    public WeatherEffect selected() {
        return selected;
    }

    public void setSelected(WeatherEffect effect) {
        if(selected != effect) {
            selected = effect;
            setChanged();
        }
    }

    public void setOwner(UUID id, String name) {
        ownerId = id;
        ownerName = name;
        setChanged();
    }

    public ItemStackHandler catalyst() {
        return catalyst;
    }

    /** Exposed to hoppers and pipes: inserts Sky Shards, never gives them back. */
    public IItemHandler automationCatalyst() {
        return automationCatalyst;
    }

    /** Exposed to energy networks: receives only. */
    public IEnergyStorage energy() {
        return energy;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public long cooldownRemainingTicks() {
        if(!(level instanceof ServerLevel server)) return 0;
        var cooldown = WeatherVaneCooldown.get(server.getServer());
        return cooldown.remainingTicks(WeatherVaneCooldown.now(server.getServer()), settings().cooldownSeconds());
    }

    /** Whether the selected effect could fire now, without changing anything. */
    public WeatherVaneStatus status() {
        return status(settings());
    }

    private WeatherVaneStatus status(WeatherVaneSettings settings) {
        if(level == null) return WeatherVaneStatus.DISABLED;
        WeatherVaneStatus status = WeatherVaneLogic.check(level, selected, settings, cooldownRemainingTicks());
        if(status == WeatherVaneStatus.READY && !canPay(settings)) return WeatherVaneStatus.NO_FUEL;
        return status;
    }

    /**
     * Fires the selected effect if every check passes: pays the cost, applies the effect, starts the shared
     * cooldown and announces it. {@code player} is the player who pressed Activate, or null for a redstone pulse.
     * Returns READY when it fired, otherwise the reason it did not.
     */
    public WeatherVaneStatus activate(@Nullable ServerPlayer player) {
        if(!(level instanceof ServerLevel server)) return WeatherVaneStatus.DISABLED;
        WeatherVaneSettings settings = settings();
        WeatherVaneStatus status = status(settings);
        if(status != WeatherVaneStatus.READY) return status;

        pay(settings);
        WeatherVaneLogic.apply(server, selected);
        WeatherVaneCooldown.get(server.getServer()).markUsed(WeatherVaneCooldown.now(server.getServer()));
        if(settings.announceInChat()) {
            server.getServer().getPlayerList().broadcastSystemMessage(announcement(player), false);
        }
        TunnelRatsTools.LOGGER.info("Weather Vane at {} in {}: {} ({})", worldPosition, server.dimension().location(),
                selected.getSerializedName(), player != null ? player.getGameProfile().getName() : "redstone");
        return WeatherVaneStatus.READY;
    }

    /** The chat message for an activation: names the player, or for redstone the owner who placed the vane. */
    public Component announcement(@Nullable ServerPlayer player) {
        String prefix = "message." + TunnelRatsTools.MODID + ".weather_vane.";
        Component change = selected.changeDescription();
        if(player != null) return Component.translatable(prefix + "player", player.getDisplayName(), change);
        if(ownerName != null) return Component.translatable(prefix + "redstone_owner", ownerName, change);
        return Component.translatable(prefix + "redstone", worldPosition.toShortString(), change);
    }

    private boolean useEnergy(WeatherVaneSettings settings) {
        return settings.energyPerUse() > 0 && energy.getEnergyStored() >= settings.energyPerUse();
    }

    private boolean canPay(WeatherVaneSettings settings) {
        return !settings.requireCatalyst() || useEnergy(settings) || !catalyst.getStackInSlot(0).isEmpty();
    }

    private void pay(WeatherVaneSettings settings) {
        if(!settings.requireCatalyst()) return;
        if(useEnergy(settings)) energy.consume(settings.energyPerUse());
        else catalyst.extractItem(0, 1, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Effect", selected.getSerializedName());
        tag.put("Catalyst", catalyst.serializeNBT(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        if(ownerId != null) tag.putUUID("Owner", ownerId);
        if(ownerName != null) tag.putString("OwnerName", ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selected = WeatherEffect.byName(tag.getString("Effect"), WeatherEffect.CLEAR);
        if(tag.contains("Catalyst")) catalyst.deserializeNBT(registries, tag.getCompound("Catalyst"));
        energy.set(tag.getInt("Energy"));
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.contains("OwnerName") ? tag.getString("OwnerName") : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModRegistries.WEATHER_VANE.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WeatherVaneMenu(containerId, inventory, this);
    }

    private final class Energy extends EnergyStorage
    {
        Energy(int capacity) {
            super(capacity, capacity, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if(received > 0 && !simulate) setChanged();
            return received;
        }

        void consume(int amount) {
            energy -= amount;
            setChanged();
        }

        void set(int amount) {
            energy = Mth.clamp(amount, 0, capacity);
        }
    }

    private record InsertOnly(IItemHandler inner) implements IItemHandler
    {
        @Override
        public int getSlots() {
            return inner.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inner.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inner.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inner.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inner.isItemValid(slot, stack);
        }
    }

    /** Server side values the GUI shows. Synced as 16 bit values, so large numbers use two slots. */
    private final class MenuData implements ContainerData
    {
        @Override
        public int get(int index) {
            WeatherVaneSettings settings = settings();
            return switch(index) {
                case WeatherVaneMenu.DATA_SELECTED -> selected.ordinal();
                case WeatherVaneMenu.DATA_STATUS -> status(settings).ordinal();
                case WeatherVaneMenu.DATA_FLAGS -> WeatherVaneMenu.flags(settings);
                default -> {
                    int pair = (index - WeatherVaneMenu.DATA_FIRST_PAIR) / 2;
                    long value = switch(pair) {
                        case 0 -> energy.getEnergyStored();
                        case 1 -> energy.getMaxEnergyStored();
                        case 2 -> settings.energyPerUse();
                        case 3 -> (cooldownRemainingTicks() + 19) / 20;
                        default -> 0;
                    };
                    yield WeatherVaneMenu.half((int) value, (index - WeatherVaneMenu.DATA_FIRST_PAIR) % 2 == 1);
                }
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return WeatherVaneMenu.DATA_COUNT;
        }
    }
}
