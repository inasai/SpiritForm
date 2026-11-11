package com.spiritform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Де в Потойбіччі ростуть Титанічні дерева (100 блоків). Місця задає сітка за зерном світу, як у MavkaTrees,
 * а придатність місця (ліс, суша, висота) перевіряється один раз за допомогою генератора й запам'ятовується.
 */
public final class TitanTrees {

    public static final int CELL = 640;
    private static final float CHANCE = 0.55F;
    private static final int MARGIN = 170;
    public static final int HEIGHT = 100;
    /** У цьому радіусі від стовбура форма Привида не діє. */
    public static final int FORM_BLOCK_RADIUS = 20;
    /** Радіус голого ґрунту навколо дерева. */
    public static final int CLEAR_RADIUS = 20;

    /** Результат перевірки місця: чи росте там дерево і на якій висоті його основа. */
    public record Site(BlockPos center, boolean valid, int baseY) {
    }

    private static final Map<Long, Site> SITES = new ConcurrentHashMap<>();

    private TitanTrees() {
    }

    /** Центр дерева в клітинці сітки (Y = 0) або null. */
    public static BlockPos center(long seed, int cellX, int cellZ) {
        RandomSource random = new LegacyRandomSource(
                seed ^ (cellX * 7919000321L) ^ (cellZ * 104729000117L) ^ 0x54495441L);
        random.nextInt();
        if (random.nextFloat() >= CHANCE) {
            return null;
        }
        int x = cellX * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
        int z = cellZ * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
        return new BlockPos(x, 0, z);
    }

    /** Чи є поблизу місце дерева за сіткою (без перевірки придатності). Для виключення Дерев Мавки. */
    public static boolean isNearSite(long seed, int x, int z, int radius) {
        long limit = (long) radius * radius;
        for (int cx = Math.floorDiv(x - radius, CELL); cx <= Math.floorDiv(x + radius, CELL); cx++) {
            for (int cz = Math.floorDiv(z - radius, CELL); cz <= Math.floorDiv(z + radius, CELL); cz++) {
                BlockPos c = center(seed, cx, cz);
                if (c != null) {
                    long dx = c.getX() - x;
                    long dz = c.getZ() - z;
                    if (dx * dx + dz * dz <= limit) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Перевірене місце дерева в клітинці або null, якщо в клітинці дерева немає. */
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
        int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
        boolean valid = floor == surface
                && floor > level.getSeaLevel() + 2
                && floor + HEIGHT + 30 < level.getMaxBuildHeight();
        int[][] probes = {{0, 0}, {20, 0}, {-20, 0}, {0, 20}, {0, -20}};
        for (int i = 0; valid && i < probes.length; i++) {
            int px = x + probes[i][0];
            int pz = z + probes[i][1];
            Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                    QuartPos.fromBlock(px), QuartPos.fromBlock(floor), QuartPos.fromBlock(pz), random.sampler());
            valid = MavkaTreeFeature.isForest(biome);
        }
        return new Site(new BlockPos(x, floor, z), valid, floor);
    }

    /** Дерево, чий центр ближче за radius до точки (x, z), або null. */
    public static Site nearestValid(ServerLevel level, int x, int z, int radius) {
        long limit = (long) radius * radius;
        for (int cx = Math.floorDiv(x - radius, CELL); cx <= Math.floorDiv(x + radius, CELL); cx++) {
            for (int cz = Math.floorDiv(z - radius, CELL); cz <= Math.floorDiv(z + radius, CELL); cz++) {
                BlockPos c = center(level.getSeed(), cx, cz);
                if (c == null) {
                    continue;
                }
                long dx = c.getX() - x;
                long dz = c.getZ() - z;
                if (dx * dx + dz * dz > limit) {
                    continue;
                }
                Site site = site(level, cx, cz);
                if (site != null && site.valid()) {
                    return site;
                }
            }
        }
        return null;
    }

    /** Форма Привида не діє біля Титанічного дерева (лише у Потойбіччі). */
    public static boolean blocksForm(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(SpiritBeyond.KEY)) {
            return false;
        }
        return nearestValid(level, player.getBlockX(), player.getBlockZ(), FORM_BLOCK_RADIUS) != null;
    }
}
