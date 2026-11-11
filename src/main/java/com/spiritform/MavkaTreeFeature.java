package com.spiritform;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Дерево Мавки: гігантське дерево заввишки 50 блоків. Навколо нього в радіусі 10 блоків голий ґрунт
 * (решту рослинності не пускає NotNearMavkaTreeFilter). Біля дерева з'являється Мавка.
 * Місце дерева визначає MavkaTrees; ця фіча спрацьовує в тому чанку, де лежить центр дерева.
 * Уся будова вкладається в 16 блоків від центру, бо генерація може писати лише в сусідні чанки.
 */
public class MavkaTreeFeature extends Feature<NoneFeatureConfiguration> {

    public static final ResourceKey<Biome> FOREST = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "beyond_forest"));

    /** Біом річок лісу: дерева й Титан вважають його частиною лісу, щоб річка не розривала їх. */
    public static final ResourceKey<Biome> RIVER = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "beyond_river"));

    public static boolean isForest(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(SpiritBiomes.FOREST_LIKE);
    }

    public static final int HEIGHT = 50;
    private static final int CANOPY_RX = 13;
    private static final int CANOPY_RY = 7;

    public MavkaTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockPos spot = MavkaTrees.centerInChunk(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4);
        if (spot == null) {
            return false;
        }
        int cx = spot.getX();
        int cz = spot.getZ();

        // Усі точки навколо мають бути в лісовому біомі, на суші й без різких перепадів
        int[][] probes = {{0, 0}, {12, 0}, {-12, 0}, {0, 12}, {0, -12}, {8, 8}, {-8, -8}, {8, -8}, {-8, 8}};
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int[] p : probes) {
            int x = cx + p[0];
            int z = cz + p[1];
            int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            if (floor != level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z)) {
                return false; // вода
            }
            if (!isForest(level.getBiome(new BlockPos(x, floor, z)))) {
                return false;
            }
            minY = Math.min(minY, floor);
            maxY = Math.max(maxY, floor);
        }
        if (maxY - minY > 9 || minY <= level.getSeaLevel() + 1) {
            return false;
        }
        int centerFloor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
        int baseY = centerFloor; // перший вільний блок над землею в центрі
        if (baseY + HEIGHT + CANOPY_RY + 4 >= level.getMaxBuildHeight()) {
            return false;
        }

        clearGround(level, random, cx, cz);
        buildRoots(level, random, cx, cz, baseY);
        buildTrunk(level, cx, cz, baseY);
        buildBranchesAndCanopy(level, random, cx, cz, baseY);
        spawnMavka(level, random, cx, cz, baseY);
        return true;
    }

    // ===== Земля =====

    private static void clearGround(WorldGenLevel level, RandomSource random, int cx, int cz) {
        int r = MavkaTrees.CLEAR_RADIUS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                pos.set(x, top, z);
                BlockState surface = level.getBlockState(pos);
                if (surface.isAir() || !surface.getFluidState().isEmpty()) {
                    continue;
                }
                // Голий ґрунт: де росла трава й підстилка, там тепер грубий ґрунт із вкрапленнями коріння
                BlockState ground = random.nextInt(9) == 0 ? Blocks.ROOTED_DIRT.defaultBlockState()
                        : Blocks.COARSE_DIRT.defaultBlockState();
                if (surface.is(Blocks.GRASS_BLOCK) || surface.is(Blocks.DIRT) || surface.is(Blocks.PODZOL)
                        || surface.is(Blocks.COARSE_DIRT) || surface.is(Blocks.MOSS_BLOCK)) {
                    level.setBlock(pos, ground, 2);
                }
            }
        }
    }

    private static void buildRoots(WorldGenLevel level, RandomSource random, int cx, int cz, int baseY) {
        BlockState wood = Blocks.DARK_OAK_WOOD.defaultBlockState();
        int count = 7 + random.nextInt(3);
        float offset = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < count; i++) {
            float angle = offset + i * Mth.TWO_PI / count + (random.nextFloat() - 0.5F) * 0.4F;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            int length = 7 + random.nextInt(3);
            for (int t = 3; t <= length; t++) {
                int x = cx + Math.round(cos * t);
                int z = cz + Math.round(sin * t);
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                // Корінь вище над землею біля стовбура й нижче до краю
                int height = Math.max(1, 3 - (t - 3) / 2);
                for (int y = ground - 2; y < ground + height; y++) {
                    level.setBlock(new BlockPos(x, y, z), wood, 2);
                }
                // Товщий біля стовбура
                if (t <= 5) {
                    int x2 = x + (Math.abs(cos) < Math.abs(sin) ? 1 : 0);
                    int z2 = z + (Math.abs(cos) < Math.abs(sin) ? 0 : 1);
                    int g2 = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x2, z2);
                    for (int y = g2 - 2; y < g2 + height - 1; y++) {
                        level.setBlock(new BlockPos(x2, y, z2), wood, 2);
                    }
                }
            }
        }
    }

    // ===== Стовбур =====

    private static double trunkRadius(int h) {
        if (h < 8) {
            return 4.4 - h * 0.18; // розширення біля землі
        }
        if (h < 30) {
            return 3.0 - (h - 8) * 0.018;
        }
        return 2.6 - (h - 30) * 0.05; // звужується до верхівки
    }

    private static void buildTrunk(WorldGenLevel level, int cx, int cz, int baseY) {
        BlockState wood = Blocks.DARK_OAK_WOOD.defaultBlockState();
        for (int h = -4; h <= HEIGHT; h++) {
            double r = trunkRadius(Math.max(h, 0));
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dz * dz <= r * r + 0.5) {
                        level.setBlock(new BlockPos(cx + dx, baseY + h, cz + dz), wood, 2);
                    }
                }
            }
        }
    }

    // ===== Гілки й крона =====

    private static BlockState leaves(RandomSource random) {
        BlockState state = random.nextInt(14) == 0 ? Blocks.AZALEA_LEAVES.defaultBlockState()
                : Blocks.DARK_OAK_LEAVES.defaultBlockState();
        return state.setValue(LeavesBlock.PERSISTENT, true);
    }

    private static void leaf(WorldGenLevel level, RandomSource random, BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
            level.setBlock(pos, leaves(random), 2);
        }
    }

    private static void blob(WorldGenLevel level, RandomSource random, int bx, int by, int bz, int rx, int ry) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rx; dz <= rx; dz++) {
                    double d = (double) (dx * dx) / (rx * rx) + (double) (dy * dy) / (ry * ry)
                            + (double) (dz * dz) / (rx * rx);
                    if (d <= 1.0 && (d < 0.7 || random.nextInt(3) != 0)) {
                        leaf(level, random, new BlockPos(bx + dx, by + dy, bz + dz));
                    }
                }
            }
        }
    }

    private static void buildBranchesAndCanopy(WorldGenLevel level, RandomSource random, int cx, int cz, int baseY) {
        BlockState wood = Blocks.DARK_OAK_WOOD.defaultBlockState();
        int branches = 15;
        float offset = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < branches; i++) {
            int startH = 22 + random.nextInt(25);
            float angle = offset + i * Mth.TWO_PI / branches + (random.nextFloat() - 0.5F) * 0.5F;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            float limit = 14.0F - (startH - 22) * 0.25F; // верхні гілки коротші
            float length = Math.max(6.0F, Math.min(limit, 7.0F + random.nextFloat() * 6.0F));
            float rise = 0.3F + random.nextFloat() * 0.35F;
            double startR = trunkRadius(startH);
            int ex = cx;
            int ey = baseY + startH;
            int ez = cz;
            for (float t = (float) startR - 1.0F; t <= length; t += 0.6F) {
                int x = cx + Math.round(cos * t);
                int z = cz + Math.round(sin * t);
                int y = baseY + startH + Math.round(rise * t);
                level.setBlock(new BlockPos(x, y, z), wood, 2);
                if (t < length * 0.55F) {
                    level.setBlock(new BlockPos(x, y + 1, z), wood, 2);
                    level.setBlock(new BlockPos(x + (Math.abs(cos) < Math.abs(sin) ? 1 : 0), y,
                            z + (Math.abs(cos) < Math.abs(sin) ? 0 : 1)), wood, 2);
                }
                ex = x;
                ey = y;
                ez = z;
            }
            blob(level, random, ex, ey + 1, ez, 4 + random.nextInt(2), 3);
        }

        // Велика крона
        int top = baseY + HEIGHT - 3;
        for (int dx = -CANOPY_RX; dx <= CANOPY_RX; dx++) {
            for (int dy = -CANOPY_RY; dy <= CANOPY_RY; dy++) {
                for (int dz = -CANOPY_RX; dz <= CANOPY_RX; dz++) {
                    double d = (double) (dx * dx) / (CANOPY_RX * CANOPY_RX)
                            + (double) (dy * dy) / (CANOPY_RY * CANOPY_RY)
                            + (double) (dz * dz) / (CANOPY_RX * CANOPY_RX);
                    if (d < 0.75 || (d <= 1.0 && random.nextInt(3) != 0)) {
                        leaf(level, random, new BlockPos(cx + dx, top + dy, cz + dz));
                    }
                }
            }
        }
        // Лише верхівка стовбура над кроною, щоб дерево мало шпиль
        for (int h = HEIGHT - 2; h <= HEIGHT + 3; h++) {
            level.setBlock(new BlockPos(cx, baseY + h, cz), wood, 2);
        }
        blob(level, random, cx, baseY + HEIGHT + 3, cz, 3, 2);
        // Звисаюче листя по краю крони
        for (int i = 0; i < 70; i++) {
            float a = random.nextFloat() * Mth.TWO_PI;
            float rad = (CANOPY_RX - 1) * (0.6F + random.nextFloat() * 0.4F);
            int x = cx + Math.round(Mth.cos(a) * rad);
            int z = cz + Math.round(Mth.sin(a) * rad);
            int y = top - 3 - random.nextInt(2);
            int len = 2 + random.nextInt(5);
            for (int k = 0; k < len; k++) {
                leaf(level, random, new BlockPos(x, y - k, z));
            }
        }
    }

    // ===== Мавка =====

    /** Скільки Мавок з'являється біля Дерева Мавки. */
    private static final int MAVKA_COUNT = 3;

    private static void spawnMavka(WorldGenLevel level, RandomSource random, int cx, int cz, int baseY) {
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < MAVKA_COUNT; i++) {
            spawnOneMavka(level, random, cx, cz, baseY, start + i * Mth.TWO_PI / MAVKA_COUNT);
        }
    }

    private static void spawnOneMavka(WorldGenLevel level, RandomSource random, int cx, int cz, int baseY, float angle) {
        int x = cx + Math.round(Mth.cos(angle) * 6.0F);
        int z = cz + Math.round(Mth.sin(angle) * 6.0F);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        Mavka mavka = SpiritCreatures.MAVKA.get().create(level.getLevel());
        if (mavka == null) {
            return;
        }
        BlockPos at = new BlockPos(x, y, z);
        mavka.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        mavka.setTreePos(new BlockPos(cx, baseY, cz));
        mavka.setPersistenceRequired();
        mavka.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(mavka);
    }
}
