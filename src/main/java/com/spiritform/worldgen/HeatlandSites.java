package com.spiritform.worldgen;

import com.spiritform.registry.SpiritBiomes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Місця Жарокраю: Титанічний вулкан (за сіткою й зерном світу, як Титанічне дерево), малі конуси навколо
 * та все, що залежить лише від зерна й центру вулкана: лавові річки, камера магми й тунелі всередині.
 * Усе обчислюється один раз і запам'ятовується, тож чанки збігаються між собою.
 */
public final class HeatlandSites {

    public static final int CELL = 840;
    private static final int MARGIN = 200;
    public static final int RADIUS = 75;
    public static final int HEIGHT = TitanTrees.HEIGHT;
    public static final int RIM = 20;
    public static final int BOWL = 16;
    public static final int LAKE = 7;
    /** Як далеко від центру тягнуться лавові річки. */
    public static final int RIVER_REACH = 260;

    public static final int SMALL_CELL = 128;
    private static final float SMALL_CHANCE = 0.3F;
    public static final int SMALL_MAX_RADIUS = 24;

    public static final int NONE = Integer.MIN_VALUE;

    /** Конус вулкана (великого чи малого) з кратером. */
    public record Cone(int x, int z, int base, int radius, int height, int rim, int bowl, int lake,
                       boolean main, long salt) {

        public int lakeLevel() {
            return base + height - lake;
        }

        public double dist(int px, int pz) {
            double dx = px - x;
            double dz = pz - z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        /** Висота верху конуса в колонці або NONE, якщо колонка поза конусом. */
        public int top(int px, int pz) {
            double r = dist(px, pz);
            if (r >= radius) {
                return NONE;
            }
            double h;
            if (r < rim) {
                double t = r / rim;
                h = height - bowl + bowl * t * t;
            } else {
                double t = (r - rim) / (double) (radius - rim);
                h = height * Math.pow(1.0 - t, main ? 1.2 : 1.5);
                double ramp = Math.min(1.0, (r - rim) / 6.0);
                if (main) {
                    h = h * 0.72 + 0.28 * (Math.floor(h / 6.0) * 6.0);
                    h += (fbm(px / 9.0, pz / 9.0, salt) - 0.5) * 5.0 * (1.0 - t) * ramp;
                } else {
                    h += (fbm(px / 5.0, pz / 5.0, salt) - 0.5) * 2.0 * (1.0 - t) * ramp;
                }
            }
            return base + (int) Math.round(h);
        }
    }

    /** Точка лавової річки: рівень лави, піврозмах русла, чи застигла ділянка. */
    public record RiverPoint(int x, int z, int level, float halfWidth, boolean crust) {
    }

    /** Порожнина всередині вулкана; лава заповнює її нижню частину (y <= lavaY). */
    public record Sphere(int x, int y, int z, int r, int lavaY) {
    }

    public record Volcano(Cone cone, List<RiverPoint> rivers, List<Sphere> spheres) {
    }

    private static final Map<Long, Optional<Volcano>> MAIN = new ConcurrentHashMap<>();
    private static final Map<Long, Optional<Cone>> SMALL = new ConcurrentHashMap<>();

    private HeatlandSites() {
    }

    // ---------- шум без залежностей ----------

    static int hash(long x, long y, long z, long salt) {
        long h = salt ^ (x * 341873128712L) ^ (y * 132897987541L) ^ (z * 42317861L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (int) ((h >>> 8) & 0xFFFFFF);
    }

    static double noise(double x, double z, long salt) {
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        double fx = x - ix;
        double fz = z - iz;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = hash(ix, 0, iz, salt) / 16777216.0;
        double b = hash(ix + 1, 0, iz, salt) / 16777216.0;
        double c = hash(ix, 0, iz + 1, salt) / 16777216.0;
        double d = hash(ix + 1, 0, iz + 1, salt) / 16777216.0;
        return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
    }

    static double fbm(double x, double z, long salt) {
        return noise(x, z, salt) * 0.65 + noise(x * 2.1 + 17.3, z * 2.1 - 9.1, salt ^ 0x5DEECE66DL) * 0.35;
    }

    // ---------- великий вулкан ----------

    private static long key(long seed, int cx, int cz) {
        return seed * 31L + cx * 1_000_003L + cz * 998_244_353L;
    }

    /** Вулкан у клітинці сітки або null. */
    public static Volcano main(ServerLevel level, int cellX, int cellZ) {
        long seed = level.getSeed();
        return MAIN.computeIfAbsent(key(seed, cellX, cellZ),
                k -> Optional.ofNullable(evaluateMain(level, seed, cellX, cellZ))).orElse(null);
    }

    private static Volcano evaluateMain(ServerLevel level, long seed, int cellX, int cellZ) {
        RandomSource random = new LegacyRandomSource(
                seed ^ (cellX * 7340033000L) ^ (cellZ * 29996669L) ^ 0x4845544CL);
        random.nextInt();
        ServerChunkCache cache = level.getChunkSource();
        ChunkGenerator generator = cache.getGenerator();
        RandomState state = cache.randomState();
        int[][] probes = {{0, 0}, {55, 0}, {-55, 0}, {0, 55}, {0, -55}, {40, 40}, {-40, 40}, {40, -40}, {-40, -40}};
        for (int attempt = 0; attempt < 6; attempt++) {
            int x = cellX * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            int z = cellZ * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
            int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, state);
            if (floor <= level.getSeaLevel() + 1) {
                continue;
            }
            boolean ok = true;
            for (int i = 0; ok && i < probes.length; i++) {
                Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                        QuartPos.fromBlock(x + probes[i][0]), QuartPos.fromBlock(floor),
                        QuartPos.fromBlock(z + probes[i][1]), state.sampler());
                ok = biome.is(SpiritBiomes.HEATLAND);
            }
            if (!ok) {
                continue;
            }
            int base = Mth.clamp(floor, 66, 112);
            long salt = seed ^ (x * 31L) ^ (z * 17L) ^ 0x564F4C43L;
            Cone cone = new Cone(x, z, base, RADIUS, HEIGHT, RIM, BOWL, LAKE, true, salt);
            RandomSource rnd = new LegacyRandomSource(salt);
            rnd.nextInt();
            List<RiverPoint> rivers = new ArrayList<>();
            int count = 6 + rnd.nextInt(3);
            for (int i = 0; i < count; i++) {
                double a = (i + (rnd.nextDouble() - 0.5) * 0.5) * (Math.PI * 2 / count);
                trace(level, generator, state, cone, rnd, rivers,
                        cone.x() + Math.cos(a) * (cone.rim() - 6), cone.z() + Math.sin(a) * (cone.rim() - 6),
                        a, cone.lakeLevel(), 3.8F, 85, true);
            }
            return new Volcano(cone, rivers, buildSpheres(cone, rnd));
        }
        return null;
    }

    private static int ground(ServerLevel level, ChunkGenerator generator, RandomState state, Cone cone, int x, int z) {
        int g = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, state) - 1;
        int c = cone.top(x, z);
        return c == NONE ? g : Math.max(g, c);
    }

    private static void trace(ServerLevel level, ChunkGenerator generator, RandomState state, Cone cone,
                              RandomSource rnd, List<RiverPoint> out, double sx, double sz, double heading,
                              int startLevel, float hw0, int steps, boolean trunk) {
        double x = sx;
        double z = sz;
        double a = heading;
        int lvl = startLevel;
        int crustLeft = 0;
        boolean crust = false;
        for (int s = 0; s < steps; s++) {
            float t = s / (float) steps;
            float hw = Math.max(1.5F, hw0 * (1.0F - 0.5F * t));
            int gx = (int) Math.round(x);
            int gz = (int) Math.round(z);
            if (trunk && cone.dist(gx, gz) < cone.rim() + 2) {
                lvl = startLevel;
            } else {
                lvl = Math.min(lvl, ground(level, generator, state, cone, gx, gz) - 1);
            }
            if (s >= 18) {
                if (crustLeft <= 0) {
                    crust = rnd.nextFloat() < 0.35F;
                    crustLeft = 5 + rnd.nextInt(10);
                }
                crustLeft--;
            } else {
                crust = false;
            }
            out.add(new RiverPoint(gx, gz, lvl, hw, crust));
            if (s > 10 && lvl <= level.getSeaLevel()) {
                break;
            }
            int gl = ground(level, generator, state, cone,
                    (int) (x + Math.cos(a + 0.5) * 12), (int) (z + Math.sin(a + 0.5) * 12));
            int gr = ground(level, generator, state, cone,
                    (int) (x + Math.cos(a - 0.5) * 12), (int) (z + Math.sin(a - 0.5) * 12));
            if (gl + 2 < gr) {
                a += 0.1;
            } else if (gr + 2 < gl) {
                a -= 0.1;
            }
            a += (rnd.nextDouble() - 0.5) * 0.3;
            a = Mth.clamp(a, heading - 1.1, heading + 1.1);
            x += Math.cos(a) * 3;
            z += Math.sin(a) * 3;
            if (trunk && hw > 2.4F && (s == 26 || s == 50) && rnd.nextFloat() < 0.65F) {
                double side = rnd.nextBoolean() ? 1 : -1;
                trace(level, generator, state, cone, rnd, out, x, z, a + side * (0.5 + rnd.nextDouble() * 0.4),
                        lvl, hw * 0.62F, 38, false);
            }
        }
    }

    private static List<Sphere> buildSpheres(Cone cone, RandomSource rnd) {
        List<Sphere> list = new ArrayList<>();
        int cy = cone.base() + 16;
        list.add(new Sphere(cone.x(), cy, cone.z(), 11, cy - 6));
        for (int i = 0; i < 5; i++) {
            double a = (i + rnd.nextDouble() * 0.6) * (Math.PI * 2 / 5);
            double x = cone.x() + Math.cos(a) * 6;
            double z = cone.z() + Math.sin(a) * 6;
            double y = cy;
            for (int s = 0; s < 24; s++) {
                int r = 2 + rnd.nextInt(3);
                list.add(new Sphere((int) Math.round(x), (int) Math.round(y), (int) Math.round(z), r,
                        (int) Math.round(y - r * 0.5)));
                a += (rnd.nextDouble() - 0.5) * 0.4;
                x += Math.cos(a) * 3;
                z += Math.sin(a) * 3;
                y += rnd.nextDouble() * 0.9 - 0.25;
            }
        }
        return list;
    }

    /** Великі вулкани, чий вплив (конус і річки) сягає прямокутника чанка. */
    public static List<Volcano> mainNear(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        List<Volcano> list = new ArrayList<>();
        int reach = RADIUS + RIVER_REACH;
        for (int cx = Math.floorDiv(minX - reach, CELL); cx <= Math.floorDiv(maxX + reach, CELL); cx++) {
            for (int cz = Math.floorDiv(minZ - reach, CELL); cz <= Math.floorDiv(maxZ + reach, CELL); cz++) {
                Volcano v = main(level, cx, cz);
                if (v == null) {
                    continue;
                }
                int dx = Math.max(Math.max(minX - v.cone().x(), v.cone().x() - maxX), 0);
                int dz = Math.max(Math.max(minZ - v.cone().z(), v.cone().z() - maxZ), 0);
                if ((long) dx * dx + (long) dz * dz <= (long) reach * reach) {
                    list.add(v);
                }
            }
        }
        return list;
    }

    // ---------- малі конуси ----------

    public static Cone small(ServerLevel level, int cellX, int cellZ) {
        long seed = level.getSeed();
        return SMALL.computeIfAbsent(key(seed, cellX, cellZ) ^ 0x534D414CL,
                k -> Optional.ofNullable(evaluateSmall(level, seed, cellX, cellZ))).orElse(null);
    }

    private static Cone evaluateSmall(ServerLevel level, long seed, int cellX, int cellZ) {
        RandomSource random = new LegacyRandomSource(
                seed ^ (cellX * 15485863000L) ^ (cellZ * 32452843L) ^ 0x534D434FL);
        random.nextInt();
        if (random.nextFloat() >= SMALL_CHANCE) {
            return null;
        }
        int x = cellX * SMALL_CELL + SMALL_MAX_RADIUS + random.nextInt(SMALL_CELL - 2 * SMALL_MAX_RADIUS);
        int z = cellZ * SMALL_CELL + SMALL_MAX_RADIUS + random.nextInt(SMALL_CELL - 2 * SMALL_MAX_RADIUS);
        int radius = 10 + random.nextInt(SMALL_MAX_RADIUS - 9);
        int height = 10 + random.nextInt(17);
        ServerChunkCache cache = level.getChunkSource();
        ChunkGenerator generator = cache.getGenerator();
        RandomState state = cache.randomState();
        int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, state);
        if (floor <= level.getSeaLevel() + 1) {
            return null;
        }
        Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(floor), QuartPos.fromBlock(z), state.sampler());
        if (!biome.is(SpiritBiomes.HEATLAND)) {
            return null;
        }
        for (Volcano v : mainNear(level, x, z, x, z)) {
            if (v.cone().dist(x, z) < v.cone().radius() + radius + 12) {
                return null;
            }
        }
        int bowl = Math.max(4, height / 4);
        int rim = Math.max(3, radius / 4);
        return new Cone(x, z, floor, radius, height, rim, bowl, Math.max(2, bowl - 2), false,
                seed ^ (x * 13L) ^ (z * 7L));
    }

    public static List<Cone> smallNear(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        List<Cone> list = new ArrayList<>();
        int reach = SMALL_MAX_RADIUS + 1;
        for (int cx = Math.floorDiv(minX - reach, SMALL_CELL); cx <= Math.floorDiv(maxX + reach, SMALL_CELL); cx++) {
            for (int cz = Math.floorDiv(minZ - reach, SMALL_CELL); cz <= Math.floorDiv(maxZ + reach, SMALL_CELL); cz++) {
                Cone c = small(level, cx, cz);
                if (c != null && c.x() + c.radius() >= minX && c.x() - c.radius() <= maxX
                        && c.z() + c.radius() >= minZ && c.z() - c.radius() <= maxZ) {
                    list.add(c);
                }
            }
        }
        return list;
    }

    /** Найближчий вулкан до точки в межах cells клітинок. */
    public static Volcano nearest(ServerLevel level, double fx, double fz, int cells) {
        Volcano best = null;
        double bestDist = Double.MAX_VALUE;
        int cx0 = Math.floorDiv((int) fx, CELL);
        int cz0 = Math.floorDiv((int) fz, CELL);
        for (int cx = cx0 - cells; cx <= cx0 + cells; cx++) {
            for (int cz = cz0 - cells; cz <= cz0 + cells; cz++) {
                Volcano v = main(level, cx, cz);
                if (v == null) {
                    continue;
                }
                double dx = v.cone().x() - fx;
                double dz = v.cone().z() - fz;
                double d = dx * dx + dz * dz;
                if (d < bestDist) {
                    bestDist = d;
                    best = v;
                }
            }
        }
        return best;
    }

    /** Клітинка над центром гнізда Жар-птиці: край кратера, під кутом, що залежить від зерна. */
    public static BlockPos nestPos(Volcano v) {
        Cone c = v.cone();
        double angle = hash(c.x(), 7, c.z(), c.salt()) / 16777216.0 * Math.PI * 2.0;
        int x = c.x() + (int) Math.round(Math.cos(angle) * (c.rim() + 3));
        int z = c.z() + (int) Math.round(Math.sin(angle) * (c.rim() + 3));
        return new BlockPos(x, c.top(x, z) + 1, z);
    }

    public static BlockPos summit(Volcano v) {
        return new BlockPos(v.cone().x(), v.cone().base() + v.cone().height(), v.cone().z());
    }
}
