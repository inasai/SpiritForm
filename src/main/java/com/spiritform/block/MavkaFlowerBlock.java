package com.spiritform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Квітка Мавки. Поставлена на землю, у радіусі 20 блоків прискорює ріст усіх рослин: щосекунди вона дає
 * їм додаткові випадкові тіки, тож вони ростуть приблизно вчетверо швидше.
 */
public class MavkaFlowerBlock extends FlowerBlock {

    public static final int RADIUS = 20;
    /** Скільки випадкових точок перевіряється кожні 10 тіків. */
    private static final int SAMPLES = 600;
    private static final int INTERVAL = 10;

    public MavkaFlowerBlock(Properties properties) {
        super(MobEffects.REGENERATION, 5.0F, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < SAMPLES; i++) {
            int dx = random.nextInt(2 * RADIUS + 1) - RADIUS;
            int dz = random.nextInt(2 * RADIUS + 1) - RADIUS;
            if (dx * dx + dz * dz > RADIUS * RADIUS) {
                continue;
            }
            BlockPos target = pos.offset(dx, random.nextInt(9) - 3, dz);
            if (!level.isLoaded(target)) {
                continue;
            }
            BlockState targetState = level.getBlockState(target);
            if (targetState.isRandomlyTicking() && growsOverTime(targetState)) {
                targetState.randomTick(level, target, random);
            }
        }
        level.scheduleTick(pos, this, INTERVAL);
    }

    private static boolean growsOverTime(BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();
        return block instanceof BonemealableBlock || block instanceof BushBlock || block instanceof SugarCaneBlock
                || block instanceof CactusBlock || block instanceof GrowingPlantBlock || block instanceof BambooStalkBlock;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 0.4 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
        }
    }
}
