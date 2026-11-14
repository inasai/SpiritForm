package com.spiritform.worldgen;

import com.spiritform.registry.SpiritBiomes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Центри Рівнин Потойбіччя: місце Польовика й кіл Полудниць. Місця задає сітка за зерном світу (як у TitanTrees),
 * а придатність (центр і точки навколо лежать у Рівнинах) перевіряється один раз і запам'ятовується.
 */
public final class PlainsSites {

    public static final int CELL = 320;
    private static final float CHANCE = 0.85F;
    private static final int MARGIN = 110;
    /** Радіуси кіл Полудниць від найбільшого до найменшого: кожне нове коло вужче за попереднє. */
    public static final int[] RINGS = {76, 66, 56, 47, 38, 30, 23, 17, 12, 8, 4};
    public static final int MAX_RADIUS = 80;

    public record Site(BlockPos center, boolean valid) {
    }

    private static final Map<Long, Site> SITES = new ConcurrentHashMap<>();

    private PlainsSites() {
    }

    public static BlockPos center(long seed, int cellX, int cellZ) {
        RandomSource random = new LegacyRandomSource(
                seed ^ (cellX * 6700417000L) ^ (cellZ * 2147483647L) ^ 0x504C4149L);
        random.nextInt();
        if (random.nextFloat() >= CHANCE) {
            return null;
        }
        int x = cellX * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
        int z = cellZ * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
        return new BlockPos(x, 0, z);
    }

    public static Site site(ServerLevel level, int cellX, int cellZ) {
        long seed = level.getSeed();
        BlockPos c = center(seed, cellX, cellZ);
        if (c == null) {
            return null;
        }
        long key = seed * 31L + cellX * 1_000_003L + cellZ * 998_244_353L;
        return SITES.computeIfAbsent(key, k -> evaluate(level, c));
    }

    private static Site evaluate(ServerLevel level, BlockPos c) {
        ServerChunkCache cache = level.getChunkSource();
        ChunkGenerator generator = cache.getGenerator();
        RandomState random = cache.randomState();
        int x = c.getX();
        int z = c.getZ();
        int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
        int[][] probes = {{0, 0}, {40, 0}, {-40, 0}, {0, 40}, {0, -40}, {28, 28}, {-28, 28}, {28, -28}, {-28, -28}};
        boolean valid = floor > level.getSeaLevel();
        for (int i = 0; valid && i < probes.length; i++) {
            Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                    QuartPos.fromBlock(x + probes[i][0]), QuartPos.fromBlock(floor),
                    QuartPos.fromBlock(z + probes[i][1]), random.sampler());
            valid = biome.is(SpiritBiomes.PLAINS);
        }
        return new Site(new BlockPos(x, floor, z), valid);
    }
}
