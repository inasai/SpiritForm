package com.spiritform;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/**
 * Де в Потойбіччі стоять Дерева Мавки. Місця визначаються лише зерном світу за сіткою (без збережених даних),
 * тому будь-яка частина генерації світу може запитати "чи поруч дерево Мавки", не залежно від порядку чанків.
 */
public final class MavkaTrees {

    /** Розмір клітинки сітки в блоках: у кожній клітинці не більше одного дерева. */
    public static final int CELL = 160;
    /** Шанс, що в клітинці є дерево. */
    private static final float CHANCE = 0.7F;
    /** Відступ центру дерева від краю клітинки (щоб сусідні дерева не злипалися). */
    private static final int MARGIN = 40;
    /** Радіус чистої землі навколо дерева. */
    public static final int CLEAR_RADIUS = 10;

    private MavkaTrees() {
    }

    /** Центр дерева в клітинці (x, z) або null, якщо там дерева немає. Y завжди 0. */
    public static BlockPos center(long seed, int cellX, int cellZ) {
        RandomSource random = new LegacyRandomSource(
                seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L) ^ 0x4D41564BL);
        random.nextInt();
        if (random.nextFloat() >= CHANCE) {
            return null;
        }
        int x = cellX * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
        int z = cellZ * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
        // Біля місця Титанічного дерева звичайних Дерев Мавки немає
        if (TitanTrees.isNearSite(seed, x, z, 80)) {
            return null;
        }
        return new BlockPos(x, 0, z);
    }

    /** Чи є центр дерева Мавки рівно в цьому чанку; повертає його або null. */
    public static BlockPos centerInChunk(long seed, int chunkX, int chunkZ) {
        int cellX = Math.floorDiv(chunkX * 16, CELL);
        int cellZ = Math.floorDiv(chunkZ * 16, CELL);
        BlockPos c = center(seed, cellX, cellZ);
        if (c != null && (c.getX() >> 4) == chunkX && (c.getZ() >> 4) == chunkZ) {
            return c;
        }
        return null;
    }

    /** Чи ближче точка (x, z) до центру якогось дерева Мавки, ніж radius блоків. */
    public static boolean isNear(long seed, int x, int z, int radius) {
        int minCx = Math.floorDiv(x - radius, CELL);
        int maxCx = Math.floorDiv(x + radius, CELL);
        int minCz = Math.floorDiv(z - radius, CELL);
        int maxCz = Math.floorDiv(z + radius, CELL);
        long limit = (long) radius * radius;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
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

    public static double dist(BlockPos a, BlockPos b) {
        return Mth.sqrt((float) a.distSqr(b));
    }
}
