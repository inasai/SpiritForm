package com.spiritform;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Кола Полудниць навколо центру Рівнин: кожне наступне коло вужче, останнє майже біля самого Польовика.
 * Працює лише в межах свого чанка (місце змінних квітів визначається зерном світу й координатами), тож не залежить від порядку генерації.
 * У чанку з центром з'являється Польовик.
 */
public class PoludnytsiaRingsFeature extends Feature<NoneFeatureConfiguration> {

    private static final double HALF_WIDTH = 0.75;
    private static final float DENSITY = 0.5F;

    public PoludnytsiaRingsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ServerLevel server = level.getLevel();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        long seed = server.getSeed();
        BlockState flower = SpiritBlocks.POLUDNYTSIA.get().defaultBlockState();
        boolean placed = false;

        int reach = PlainsSites.MAX_RADIUS + 2;
        for (int cx = Math.floorDiv(minX - reach, PlainsSites.CELL); cx <= Math.floorDiv(minX + 15 + reach, PlainsSites.CELL); cx++) {
            for (int cz = Math.floorDiv(minZ - reach, PlainsSites.CELL); cz <= Math.floorDiv(minZ + 15 + reach, PlainsSites.CELL); cz++) {
                PlainsSites.Site site = PlainsSites.site(server, cx, cz);
                if (site == null || !site.valid()) {
                    continue;
                }
                BlockPos c = site.center();
                for (int x = minX; x < minX + 16; x++) {
                    for (int z = minZ; z < minZ + 16; z++) {
                        double dx = x - c.getX();
                        double dz = z - c.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > PlainsSites.MAX_RADIUS + 1 || !onRing(dist)) {
                            continue;
                        }
                        RandomSource random = new LegacyRandomSource(seed ^ (x * 341873128712L) ^ (z * 132897987541L) ^ 0x504F4C55L);
                        random.nextInt();
                        if (random.nextFloat() >= DENSITY) {
                            continue;
                        }
                        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                        BlockPos pos = new BlockPos(x, y, z);
                        if (level.isEmptyBlock(pos) && level.getBiome(pos).is(SpiritBiomes.PLAINS) && flower.canSurvive(level, pos)) {
                            level.setBlock(pos, flower, 2);
                            placed = true;
                        }
                    }
                }
                if ((c.getX() >> 4) == (minX >> 4) && (c.getZ() >> 4) == (minZ >> 4)) {
                    spawnPolovyk(level, c);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean onRing(double dist) {
        if (dist < 1.6) {
            return true;
        }
        for (int r : PlainsSites.RINGS) {
            if (Math.abs(dist - r) < HALF_WIDTH) {
                return true;
            }
        }
        return false;
    }

    private static void spawnPolovyk(WorldGenLevel level, BlockPos c) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
        Polovyk polovyk = SpiritCreatures.POLOVYK.get().create(level.getLevel());
        if (polovyk == null) {
            return;
        }
        BlockPos at = new BlockPos(c.getX(), y + 2, c.getZ());
        polovyk.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        polovyk.setHome(at);
        polovyk.setPersistenceRequired();
        polovyk.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(polovyk);
    }
}
