package io.github.peterbax117.tunnelratstools.raindetector;

import io.github.peterbax117.tunnelratstools.TunnelRatsConfig;
import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Redstone signal from the weather. Reads the dimension's weather (Level.isRaining and isThundering), not the biome
 * or the sky above the block, so it also works in deserts, in snowy biomes and under a roof. Outputs 0 in every
 * mode in dimensions without weather (Nether, End) and while disabled in the config. Checks every 20 ticks.
 */
public class RainDetectorBlock extends Block
{
    public static final EnumProperty<RainDetectorMode> MODE = EnumProperty.create("mode", RainDetectorMode.class);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final int POLL_TICKS = 20;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public RainDetectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, RainDetectorMode.RAIN).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    /** The signal this mode gives in this level right now. */
    public static int signal(Level level, RainDetectorMode mode) {
        if(!TunnelRatsConfig.value(TunnelRatsConfig.RAIN_DETECTOR_ENABLED) || !WeatherVaneLogic.hasWeather(level)) return 0;
        return mode.active(level) ? 15 : 0;
    }

    /** Recomputes the output; neighbors are updated only when it changes. */
    public static void updateSignal(BlockState state, Level level, BlockPos pos) {
        boolean powered = signal(level, state.getValue(MODE)) > 0;
        if(powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if(!level.isClientSide && !oldState.is(this)) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateSignal(state, level, pos);
        level.scheduleTick(pos, this, POLL_TICKS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if(level.isClientSide) return InteractionResult.SUCCESS;
        RainDetectorMode mode = state.getValue(MODE).next();
        boolean powered = signal(level, mode) > 0;
        level.setBlock(pos, state.setValue(MODE, mode).setValue(POWERED, powered), Block.UPDATE_ALL);
        player.displayClientMessage(Component.translatable("message." + TunnelRatsTools.MODID + ".rain_detector.mode",
                mode.displayName()), true);
        return InteractionResult.CONSUME;
    }
}
