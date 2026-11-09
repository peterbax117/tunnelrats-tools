package io.github.peterbax117.tunnelratstools.weathervane;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Changes the time of day or the weather. Right-click opens the GUI; a redstone pulse fires the selected effect once
 * on its rising edge (POWERED records the last input, like a dispenser's TRIGGERED).
 */
public class WeatherVaneBlock extends BaseEntityBlock
{
    public static final MapCodec<WeatherVaneBlock> CODEC = simpleCodec(WeatherVaneBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public WeatherVaneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Placing a vane next to a lever that is already on does not fire it.
        return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeatherVaneBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof WeatherVaneBlockEntity vane) {
            player.openMenu(vane, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if(placer instanceof Player player && level.getBlockEntity(pos) instanceof WeatherVaneBlockEntity vane) {
            vane.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if(level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if(powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if(powered && level.getBlockEntity(pos) instanceof WeatherVaneBlockEntity vane) {
            vane.activate(null);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WeatherVaneBlockEntity vane) {
            ItemStack shards = vane.catalyst().getStackInSlot(0);
            if(!shards.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), shards.copy());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
