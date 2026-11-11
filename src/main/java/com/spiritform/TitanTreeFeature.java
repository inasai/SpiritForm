package com.spiritform;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Титанічне дерево: 100 блоків заввишки. Коріння вилазить із землі й утворює арки, довкола стовбура йде гвинтові
 * сходи (плити по піввисоти), на гілках стоять маленькі будиночки зі скринями, а в листі на верхівці хатинка.
 * Дерево більше за один чанк, тому кожен чанк, до якого воно сягає, будує лише свою частину. Усі випадкові
 * рішення залежать лише від зерна й центру дерева, тож шви між чанками збігаються.
 */
public class TitanTreeFeature extends Feature<NoneFeatureConfiguration> {

    public static final ResourceKey<LootTable> OUTPOST_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "chests/titan_outpost"));
    public static final ResourceKey<LootTable> TOP_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "chests/titan_top"));

    /** Найвища висота стовбура над основою. */
    private static final int TOP = 94;
    private static final int REACH = 45;

    public TitanTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ServerLevel server = level.getLevel();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        boolean built = false;
        for (int cx = Math.floorDiv(minX - REACH, TitanTrees.CELL); cx <= Math.floorDiv(minX + 15 + REACH, TitanTrees.CELL); cx++) {
            for (int cz = Math.floorDiv(minZ - REACH, TitanTrees.CELL); cz <= Math.floorDiv(minZ + 15 + REACH, TitanTrees.CELL); cz++) {
                TitanTrees.Site site = TitanTrees.site(server, cx, cz);
                if (site == null || !site.valid()) {
                    continue;
                }
                BlockPos c = site.center();
                if (c.getX() + REACH < minX || c.getX() - REACH > minX + 15
                        || c.getZ() + REACH < minZ || c.getZ() - REACH > minZ + 15) {
                    continue;
                }
                new Builder(level, site, minX, minZ).build();
                built = true;
            }
        }
        return built;
    }

    static double trunkRadius(int h) {
        if (h < 10) {
            return 8.5 - 0.35 * Math.max(h, 0);
        }
        if (h < 60) {
            return 5.0 - (h - 10) * 0.02;
        }
        return 4.0 - (h - 60) * 0.03;
    }

    /** Одне будівництво: знає свій чанк і пише лише в нього. */
    private static final class Builder {
        private final WorldGenLevel level;
        private final int cx;
        private final int cz;
        private final int baseY;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private final long seed;
        private final BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

        private final BlockState wood = Blocks.DARK_OAK_WOOD.defaultBlockState();

        /** Будиночок на гілці: центр, і напрямок до стовбура. */
        private record Outpost(int x, int y, int z, int towardX, int towardZ) {
        }

        Builder(WorldGenLevel level, TitanTrees.Site site, int chunkMinX, int chunkMinZ) {
            this.level = level;
            this.cx = site.center().getX();
            this.cz = site.center().getZ();
            this.baseY = site.baseY();
            this.minX = chunkMinX;
            this.maxX = chunkMinX + 15;
            this.minZ = chunkMinZ;
            this.maxZ = chunkMinZ + 15;
            this.seed = level.getSeed() ^ (cx * 341873128712L) ^ (cz * 132897987541L) ^ 0x7177414EL;
        }

        void build() {
            clearGround();
            trunk();
            roots();
            List<Outpost> outposts = branches();
            leaves(outposts);
            double endAngle = stairs();
            for (Outpost o : outposts) {
                outpost(o);
            }
            topHut(endAngle);
            if ((cx >> 4) == (minX >> 4) && (cz >> 4) == (minZ >> 4)) {
                spawnMavkas();
            }
        }

        // ===== Допоміжне =====

        boolean in(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        void set(int x, int y, int z, BlockState state) {
            if (in(x, z)) {
                level.setBlock(mp.set(x, y, z), state, 2);
            }
        }

        BlockState get(int x, int y, int z) {
            return level.getBlockState(mp.set(x, y, z));
        }

        int ground(int x, int z) {
            return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        }

        float hash(int x, int y, int z, int salt) {
            long h = seed ^ (x * 341873128712L) ^ (y * 132897987541L) ^ (z * 0x9E3779B97F4A7C15L) ^ salt;
            h ^= h >>> 33;
            h *= 0xff51afd7ed558ccdL;
            h ^= h >>> 33;
            h *= 0xc4ceb9fe1a85ec53L;
            h ^= h >>> 33;
            return (h >>> 40) / 16777216.0F;
        }

        RandomSource random(int salt) {
            RandomSource r = new LegacyRandomSource(seed ^ (salt * 0x9E3779B97F4A7C15L));
            r.nextInt();
            return r;
        }

        boolean touches(double x, double z, double r) {
            return x + r >= minX && x - r <= maxX + 1 && z + r >= minZ && z - r <= maxZ + 1;
        }

        void ball(double x, double y, double z, double r, BlockState state) {
            if (!touches(x, z, r)) {
                return;
            }
            int ri = (int) Math.ceil(r);
            int bx = (int) Math.round(x);
            int by = (int) Math.round(y);
            int bz = (int) Math.round(z);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dy = -ri; dy <= ri; dy++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        if (dx * dx + dy * dy + dz * dz <= r * r) {
                            set(bx + dx, by + dy, bz + dz, state);
                        }
                    }
                }
            }
        }

        void leaf(int x, int y, int z) {
            if (!in(x, z)) {
                return;
            }
            BlockState existing = get(x, y, z);
            if (existing.isAir() || existing.canBeReplaced()) {
                float f = hash(x, y, z, 3);
                BlockState state = f < 0.04F ? Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState()
                        : f < 0.10F ? Blocks.AZALEA_LEAVES.defaultBlockState()
                        : Blocks.DARK_OAK_LEAVES.defaultBlockState();
                set(x, y, z, state.setValue(LeavesBlock.PERSISTENT, true));
            }
        }

        void blob(int bx, int by, int bz, int rx, int ry) {
            if (!touches(bx, bz, rx)) {
                return;
            }
            for (int dx = -rx; dx <= rx; dx++) {
                for (int dy = -ry; dy <= ry; dy++) {
                    for (int dz = -rx; dz <= rx; dz++) {
                        double d = (double) (dx * dx) / (rx * rx) + (double) (dy * dy) / (ry * ry)
                                + (double) (dz * dz) / (rx * rx);
                        if (d <= 1.0 && d >= 0.3 && (d < 0.65 || hash(bx + dx, by + dy, bz + dz, 5) < 0.6F)) {
                            leaf(bx + dx, by + dy, bz + dz);
                        }
                    }
                }
            }
        }

        // ===== Земля й стовбур =====

        void clearGround() {
            int r = TitanTrees.CLEAR_RADIUS;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int dx = x - cx;
                    int dz = z - cz;
                    if (dx * dx + dz * dz > r * r) {
                        continue;
                    }
                    int top = ground(x, z) - 1;
                    BlockState s = get(x, top, z);
                    if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.PODZOL)
                            || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.MOSS_BLOCK)) {
                        set(x, top, z, hash(x, 0, z, 1) < 0.1F ? Blocks.ROOTED_DIRT.defaultBlockState()
                                : Blocks.COARSE_DIRT.defaultBlockState());
                    }
                }
            }
        }

        void trunk() {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int dx = x - cx;
                    int dz = z - cz;
                    int d2 = dx * dx + dz * dz;
                    if (d2 > 90) {
                        continue;
                    }
                    int g = ground(x, z);
                    for (int y = Math.min(g, baseY) - 2; y <= baseY + TOP; y++) {
                        double r = trunkRadius(y - baseY);
                        if (d2 <= r * r + 0.6) {
                            set(x, y, z, wood);
                        }
                    }
                }
            }
        }

        // ===== Коріння-арки =====

        void roots() {
            RandomSource rnd = random(11);
            int n = 9;
            double[] ang = new double[n];
            double[] reach = new double[n];
            double[] peak = new double[n];
            for (int i = 0; i < n; i++) {
                ang[i] = i * Math.PI * 2.0 / n + (rnd.nextFloat() - 0.5) * 0.35;
                reach[i] = 20.0 + rnd.nextFloat() * 3.0;
                peak[i] = 10.0 + rnd.nextFloat() * 2.5;
            }
            for (int i = 0; i < n; i++) {
                if (!touches(cx + Math.cos(ang[i]) * 14, cz + Math.sin(ang[i]) * 14, 14)) {
                    continue;
                }
                int steps = 170;
                for (int k = 0; k <= steps; k++) {
                    double t = (double) k / steps;
                    double r = 5.5 + (reach[i] - 5.5) * t;
                    double y = baseY + peak[i] * Math.pow(Math.sin(Math.PI * t), 0.7) - 3.2 * t * t * t;
                    double x = cx + Math.cos(ang[i]) * r;
                    double z = cz + Math.sin(ang[i]) * r;
                    double rad = 1.4 + 2.2 * Math.pow(1.0 - t, 1.2);
                    ball(x, y, z, rad, wood);
                    // Кінець арки врізається в землю, навіть якщо там нижче
                    if (t > 0.75) {
                        int ix = (int) Math.round(x);
                        int iz = (int) Math.round(z);
                        if (in(ix, iz)) {
                            int g = ground(ix, iz);
                            for (int yy = g - 2; yy <= (int) Math.round(y); yy++) {
                                set(ix, yy, iz, wood);
                            }
                        }
                    }
                }
            }
        }

        // ===== Гілки =====

        List<Outpost> branches() {
            RandomSource rnd = random(21);
            List<Outpost> result = new ArrayList<>();
            int count = 8;
            for (int k = 0; k < count; k++) {
                int h = 44 + k * 6 + rnd.nextInt(3) - 1;
                double ang = 0.4 + k * 2.39996;
                double len = 24.0 - (h - 44) * 0.18;
                double r0 = trunkRadius(h) - 1.0;
                double end = trunkRadius(h) + len;
                double cos = Math.cos(ang);
                double sin = Math.sin(ang);
                double rise = 0.1;
                if (touches(cx + cos * (r0 + end) / 2, cz + sin * (r0 + end) / 2, (end - r0) / 2 + 3)) {
                    for (double s = r0; s <= end; s += 0.5) {
                        int x = (int) Math.round(cx + cos * s);
                        int y = baseY + h + (int) Math.round(rise * (s - r0));
                        int z = (int) Math.round(cz + sin * s);
                        if (!touches(x, z, 2)) {
                            continue;
                        }
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    set(x + dx, y + dy, z + dz, wood);
                                }
                            }
                        }
                    }
                }
                int ex = (int) Math.round(cx + cos * end);
                int ey = baseY + h + (int) Math.round(rise * (end - r0));
                int ez = (int) Math.round(cz + sin * end);
                int tx = Math.abs(cos) >= Math.abs(sin) ? (cos > 0 ? -1 : 1) : 0;
                int tz = Math.abs(cos) >= Math.abs(sin) ? 0 : (sin > 0 ? -1 : 1);
                result.add(new Outpost(ex, ey, ez, tx, tz));
            }
            return result;
        }

        // ===== Крона =====

        void leaves(List<Outpost> outposts) {
            int cy = baseY + 86;
            int rx = 30;
            int ry = 13;
            for (int x = Math.max(minX, cx - rx); x <= Math.min(maxX, cx + rx); x++) {
                for (int z = Math.max(minZ, cz - rx); z <= Math.min(maxZ, cz + rx); z++) {
                    double dx = x - cx;
                    double dz = z - cz;
                    double hd = (dx * dx + dz * dz) / (rx * rx);
                    if (hd >= 1.1) {
                        continue;
                    }
                    int lowest = Integer.MAX_VALUE;
                    for (int y = cy - ry; y <= cy + ry; y++) {
                        double dy = y - cy;
                        double d = hd + dy * dy / (ry * ry);
                        double lump = 0.25 * Math.sin(x * 0.21) * Math.cos(z * 0.17 + y * 0.3);
                        double noise = (hash(x, y, z, 7) - 0.5) * 0.3 + lump;
                        // Крона порожниста (лише оболонка), інакше сотні тисяч листків гальмують відображення
                        if (d >= 0.45 && d < 0.8 + noise) {
                            leaf(x, y, z);
                            lowest = Math.min(lowest, y);
                        }
                    }
                    if (lowest != Integer.MAX_VALUE && hash(x, 0, z, 9) < 0.1F) {
                        int len = 2 + (int) (hash(x, 1, z, 9) * 4);
                        for (int k = 1; k <= len; k++) {
                            leaf(x, lowest - k, z);
                        }
                    }
                }
            }
            for (Outpost o : outposts) {
                blob(o.x(), o.y() + 3, o.z(), 6, 4);
            }
        }

        // ===== Сходи =====

        /** Гвинтові сходи від землі до платформи. Повертає кут, на якому вони закінчуються. */
        double stairs() {
            double theta = random(31).nextFloat() * Math.PI * 2.0;
            int steps = 192;
            BlockState bottom = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            BlockState top = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            double endAngle = theta;
            for (int i = 0; i < steps; i++) {
                int y = baseY + i / 2;
                double r = trunkRadius(y - baseY) + 1.0;
                theta += 1.0 / (r + 1.0);
                endAngle = theta;
                double cos = Math.cos(theta);
                double sin = Math.sin(theta);
                if (!touches(cx + cos * (r + 1), cz + sin * (r + 1), 4)) {
                    continue;
                }
                for (int w = 0; w <= 2; w++) {
                    int x = (int) Math.round(cx + cos * (r + w));
                    int z = (int) Math.round(cz + sin * (r + w));
                    // Місце над сходинкою звільняємо (листя, гілки)
                    for (int k = 1; k <= 3; k++) {
                        set(x, y + k, z, Blocks.AIR.defaultBlockState());
                    }
                    set(x, y, z, i % 2 == 0 ? bottom : top);
                }
                int fx = (int) Math.round(cx + cos * (r + 3));
                int fz = (int) Math.round(cz + sin * (r + 3));
                set(fx, y + 1, fz, Blocks.DARK_OAK_FENCE.defaultBlockState());
                if (i % 16 == 8) {
                    set(fx, y + 2, fz, Blocks.LANTERN.defaultBlockState());
                }
            }
            return endAngle;
        }

        // ===== Будиночки =====

        void chest(int x, int y, int z, int towardX, int towardZ, ResourceKey<LootTable> table) {
            if (!in(x, z)) {
                return;
            }
            Direction facing = Direction.fromDelta(towardX, 0, towardZ);
            if (facing == null) {
                facing = Direction.NORTH;
            }
            BlockPos pos = new BlockPos(x, y, z);
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity chest) {
                chest.setLootTable(table, (long) (hash(x, y, z, 13) * Integer.MAX_VALUE));
            }
        }

        void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (!in(x, z)) {
                        continue;
                    }
                    for (int y = y0; y <= y1; y++) {
                        set(x, y, z, state);
                    }
                }
            }
        }

        void outpost(Outpost o) {
            int yf = o.y() + 1;
            int x = o.x();
            int z = o.z();
            if (!touches(x, z, 5)) {
                return;
            }
            BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
            box(x - 3, yf, z - 3, x + 3, yf, z + 3, planks);
            box(x - 3, yf + 1, z - 3, x + 3, yf + 4, z + 3, Blocks.AIR.defaultBlockState());
            // Стіни (кільце), кути з колод
            box(x - 3, yf + 1, z - 3, x + 3, yf + 3, z - 3, planks);
            box(x - 3, yf + 1, z + 3, x + 3, yf + 3, z + 3, planks);
            box(x - 3, yf + 1, z - 3, x - 3, yf + 3, z + 3, planks);
            box(x + 3, yf + 1, z - 3, x + 3, yf + 3, z + 3, planks);
            BlockState log = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
            for (int sx = -3; sx <= 3; sx += 6) {
                for (int sz = -3; sz <= 3; sz += 6) {
                    box(x + sx, yf + 1, z + sz, x + sx, yf + 3, z + sz, log);
                }
            }
            // Двері з боку стовбура
            int dx = x + o.towardX() * 3;
            int dz = z + o.towardZ() * 3;
            box(dx, yf + 1, dz, dx, yf + 2, dz, Blocks.AIR.defaultBlockState());
            // Дах
            box(x - 3, yf + 4, z - 3, x + 3, yf + 4, z + 3, Blocks.SPRUCE_SLAB.defaultBlockState());
            // Всередині
            set(x, yf + 3, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            chest(x - o.towardX() * 2, yf + 1, z - o.towardZ() * 2, o.towardX(), o.towardZ(), OUTPOST_LOOT);
        }

        void topHut(double endAngle) {
            int yf = baseY + 95;
            int half = 6;
            if (!touches(cx, cz, half + 2)) {
                return;
            }
            BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
            BlockState dark = Blocks.DARK_OAK_PLANKS.defaultBlockState();
            // Листя довкола платформи прибираємо
            box(cx - half, yf + 1, cz - half, cx + half, yf + 5, cz + half, Blocks.AIR.defaultBlockState());
            box(cx - half, yf, cz - half, cx + half, yf, cz + half, planks);
            // Огорожа по краю, окрім місця, де закінчуються сходи
            for (int x = cx - half; x <= cx + half; x++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    boolean edge = Math.abs(x - cx) == half || Math.abs(z - cz) == half;
                    if (!edge) {
                        continue;
                    }
                    double a = Math.atan2(z - cz, x - cx);
                    double diff = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(a - endAngle)));
                    if (diff > 32.0) {
                        set(x, yf + 1, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
                    }
                }
            }
            // Стіни хатинки (7x7), двері з боку сходів
            box(cx - 3, yf + 1, cz - 3, cx + 3, yf + 4, cz + 3, planks);
            box(cx - 2, yf + 1, cz - 2, cx + 2, yf + 4, cz + 2, Blocks.AIR.defaultBlockState());
            BlockState log = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
            for (int sx = -3; sx <= 3; sx += 6) {
                for (int sz = -3; sz <= 3; sz += 6) {
                    box(cx + sx, yf + 1, cz + sz, cx + sx, yf + 4, cz + sz, log);
                }
            }
            double cos = Math.cos(endAngle);
            double sin = Math.sin(endAngle);
            int sx = Math.abs(cos) >= Math.abs(sin) ? (cos > 0 ? 1 : -1) : 0;
            int sz = Math.abs(cos) >= Math.abs(sin) ? 0 : (sin > 0 ? 1 : -1);
            box(cx + sx * 3, yf + 1, cz + sz * 3, cx + sx * 3, yf + 2, cz + sz * 3, Blocks.AIR.defaultBlockState());
            // Вікна на трьох інших стінах
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (d[0] == sx && d[1] == sz) {
                    continue;
                }
                set(cx + d[0] * 3, yf + 2, cz + d[1] * 3, Blocks.GLASS.defaultBlockState());
            }
            // Дах пірамідою
            box(cx - 4, yf + 5, cz - 4, cx + 4, yf + 5, cz + 4, dark);
            box(cx - 3, yf + 6, cz - 3, cx + 3, yf + 6, cz + 3, dark);
            box(cx - 2, yf + 7, cz - 2, cx + 2, yf + 7, cz + 2, dark);
            box(cx - 1, yf + 8, cz - 1, cx + 1, yf + 8, cz + 1, dark);
            set(cx, yf + 9, cz, Blocks.LANTERN.defaultBlockState());
            // Обстановка
            box(cx - 2, yf + 1, cz - 2, cx + 2, yf + 1, cz + 2, Blocks.GREEN_CARPET.defaultBlockState());
            set(cx, yf + 4, cz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            chest(cx - 2, yf + 1, cz - 2, 1, 0, TOP_LOOT);
            chest(cx + 2, yf + 1, cz + 2, -1, 0, TOP_LOOT);
            set(cx + 2, yf + 1, cz - 2, Blocks.CRAFTING_TABLE.defaultBlockState());
            set(cx - 2, yf + 1, cz + 2, Blocks.BOOKSHELF.defaultBlockState());
            set(cx - 2, yf + 2, cz + 2, Blocks.BOOKSHELF.defaultBlockState());
            // Горщик із Квіткою Мавки: єдине місце, де її можна знайти
            set(cx, yf + 1, cz, SpiritBlocks.POTTED_MAVKA_FLOWER.get().defaultBlockState());
        }

        // ===== Мавки =====

        void spawnMavkas() {
            RandomSource rnd = random(41);
            ServerLevel server = level.getLevel();
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI * 2.0 / 10.0 + rnd.nextFloat() * 0.3;
                int x = cx + (int) Math.round(Math.cos(a) * 11);
                int z = cz + (int) Math.round(Math.sin(a) * 11);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                Mavka mavka = SpiritCreatures.MAVKA.get().create(server);
                if (mavka == null) {
                    continue;
                }
                BlockPos at = new BlockPos(x, y, z);
                mavka.moveTo(x + 0.5, y, z + 0.5, rnd.nextFloat() * 360.0F, 0.0F);
                mavka.setTreePos(new BlockPos(cx, baseY, cz), 20);
                mavka.setPersistenceRequired();
                mavka.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
                level.addFreshEntity(mavka);
            }
        }
    }
}
