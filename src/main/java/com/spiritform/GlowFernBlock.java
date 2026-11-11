package com.spiritform;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Папороть туманного лісу: світиться, як Сяйні ягоди. Зламана, віддає Перлину папороті
 * (див. loot_table/blocks/glow_fern.json).
 */
public class GlowFernBlock extends TallGrassBlock {

    public static final MapCodec<GlowFernBlock> CODEC = simpleCodec(GlowFernBlock::new);

    public GlowFernBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<TallGrassBlock> codec() {
        return (MapCodec<TallGrassBlock>) (MapCodec<?>) CODEC;
    }

    /** Найближча Папороть у межах horizontal блоків по горизонталі й vertical по вертикалі, або null. */
    public static BlockPos findNearest(LevelReader level, BlockPos from, int horizontal, int vertical) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -horizontal; dx <= horizontal; dx++) {
            for (int dz = -horizontal; dz <= horizontal; dz++) {
                double flat = dx * dx + dz * dz;
                if (flat >= bestDist) {
                    continue;
                }
                for (int dy = -vertical; dy <= vertical; dy++) {
                    p.set(from.getX() + dx, from.getY() + dy, from.getZ() + dz);
                    if (!level.hasChunkAt(p)) {
                        break;
                    }
                    if (level.getBlockState(p).is(SpiritBlocks.GLOW_FERN.get())) {
                        double d = flat + dy * dy;
                        if (d < bestDist) {
                            bestDist = d;
                            best = p.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + 0.3 + random.nextDouble() * 0.6,
                    pos.getZ() + random.nextDouble(), 0.0, 0.01, 0.0);
        }
    }
}
