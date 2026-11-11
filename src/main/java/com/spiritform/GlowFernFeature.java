package com.spiritform;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Папороть туманного лісу: один кущ (іноді з сусідами). Біля нього в 60% випадків зʼявляється Потерча,
 * що вестиме до нього гравця.
 */
public class GlowFernFeature extends Feature<NoneFeatureConfiguration> {

    public GlowFernFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockState fern = SpiritBlocks.GLOW_FERN.get().defaultBlockState();
        if (!canPlace(level, origin)) {
            return false;
        }
        level.setBlock(origin, fern, 2);
        int extra = random.nextInt(3);
        for (int i = 0; i < extra; i++) {
            BlockPos p = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ());
            p = new BlockPos(p.getX(), y, p.getZ());
            if (canPlace(level, p)) {
                level.setBlock(p, fern, 2);
            }
        }
        if (random.nextFloat() < 0.6F) {
            spawnPotercha(level, random, origin);
        }
        return true;
    }

    private static boolean canPlace(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(BlockTags.DIRT);
    }

    private static void spawnPotercha(WorldGenLevel level, RandomSource random, BlockPos fern) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = 4.0 + random.nextDouble() * 3.0;
        int x = fern.getX() + (int) Math.round(Math.cos(angle) * dist);
        int z = fern.getZ() + (int) Math.round(Math.sin(angle) * dist);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        Potercha potercha = SpiritCreatures.POTERCHA.get().create(level.getLevel());
        if (potercha == null) {
            return;
        }
        potercha.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        potercha.setHome(fern);
        potercha.setPersistenceRequired();
        potercha.finalizeSpawn(level, level.getCurrentDifficultyAt(fern), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(potercha);
    }
}
