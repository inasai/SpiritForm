package com.spiritform.block;

import com.spiritform.entity.Phoenix;
import com.spiritform.registry.SpiritBlocks;
import com.spiritform.registry.SpiritCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Попіл: плаский блок, що лишається на місці вбитої Жар-птиці. Видобувається лопатою чи рукою. Якщо поставити його
 * поруч із лавою (чи в саму лаву) й клацнути пером Жар-птиці, через 3 хвилини блок зникає, а на його місці
 * відроджується спокійна Жар-птиця, яку вже можна приручити Яблуком Духів.
 */
public class PhoenixAshBlock extends Block {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    /** 3 хвилини. */
    public static final int RITUAL_TICKS = 3600;

    public PhoenixAshBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    private static boolean lavaNear(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (level.getFluidState(pos.relative(d)).is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(SpiritCreatures.PHOENIX_FEATHER.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(LIT)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!lavaNear(level, pos)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.phoenix_ash.needs_lava"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.setBlock(pos, state.setValue(LIT, true), 3);
        level.scheduleTick(pos, this, RITUAL_TICKS);
        level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("message.spirit_form.phoenix_ash.begins"), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        level.removeBlock(pos, false);
        Phoenix phoenix = SpiritCreatures.PHOENIX.get().create(level);
        if (phoenix == null) {
            return;
        }
        phoenix.moveTo(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        phoenix.markReborn(pos.above(2));
        phoenix.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        level.addFreshEntity(phoenix);
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 60, 0.8, 0.8, 0.8, 0.08);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1.5F, 1.4F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + random.nextDouble(), pos.getY() + 0.15,
                    pos.getZ() + random.nextDouble(), 0.0, 0.04, 0.0);
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.LAVA, pos.getX() + random.nextDouble(), pos.getY() + 0.2,
                        pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
            }
        }
    }

    /** Кладе Попіл на місці загибелі (на землі чи на дні лавового озера); якщо ніде, випадає предметом. */
    public static void placeAt(ServerLevel level, Vec3 deathPos) {
        BlockPos.MutableBlockPos m = BlockPos.containing(deathPos).mutable();
        int minY = level.getMinBuildHeight();
        while (m.getY() > minY && level.isEmptyBlock(m)) {
            m.move(Direction.DOWN);
        }
        if (level.getFluidState(m).is(FluidTags.LAVA)) {
            while (m.getY() > minY && level.getFluidState(m).is(FluidTags.LAVA)) {
                m.move(Direction.DOWN);
            }
        }
        m.move(Direction.UP);
        BlockPos target = m.immutable();
        BlockState ash = SpiritBlocks.PHOENIX_ASH.get().defaultBlockState();
        boolean free = level.isEmptyBlock(target) || level.getFluidState(target).is(FluidTags.LAVA);
        if (free && target.getY() > minY && ash.canSurvive(level, target)) {
            level.setBlock(target, ash, 3);
        } else {
            level.addFreshEntity(new ItemEntity(level, deathPos.x, deathPos.y, deathPos.z,
                    new ItemStack(SpiritBlocks.PHOENIX_ASH.get())));
        }
    }
}
