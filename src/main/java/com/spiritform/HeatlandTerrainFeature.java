package com.spiritform;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Рельєф і поверхня Жарокраю: випалена земля (базальт, чорнокамінь, магма, обсидіан, позолочений чорнокамінь),
 * озера лави замість води, конуси вулканів (великого й малих) із кратерами, камера магми з тунелями всередині
 * та лавові річки від жерла. Кожен чанк змінює лише власні колонки; усе залежить від зерна й центрів,
 * тож шви між чанками збігаються. Ознака стоїть у всіх біомах Потойбіччя, а поза Жарокраєм і вулканами нічого не робить.
 */
public class HeatlandTerrainFeature extends Feature<NoneFeatureConfiguration> {

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    private static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
    private static final BlockState SMOOTH_BASALT = Blocks.SMOOTH_BASALT.defaultBlockState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState GILDED = Blocks.GILDED_BLACKSTONE.defaultBlockState();
    private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();

    public HeatlandTerrainFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ServerLevel server = level.getLevel();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        long seed = server.getSeed();

        List<HeatlandSites.Volcano> volcanoes = HeatlandSites.mainNear(server, minX, minZ, minX + 15, minZ + 15);
        List<HeatlandSites.Cone> cones = new ArrayList<>(HeatlandSites.smallNear(server, minX, minZ, minX + 15, minZ + 15));
        for (HeatlandSites.Volcano v : volcanoes) {
            cones.add(v.cone());
        }

        boolean any = !cones.isEmpty() || !volcanoes.isEmpty();
        if (!any) {
            for (int i = 0; i < 4 && !any; i++) {
                for (int j = 0; j < 4 && !any; j++) {
                    BlockPos probe = new BlockPos(minX + i * 4 + 2, level.getSeaLevel(), minZ + j * 4 + 2);
                    any = level.getBiome(probe).is(SpiritBiomes.HEATLAND);
                }
            }
        }
        if (!any) {
            return false;
        }

        // Точки річок і порожнини, що торкаються цього чанка.
        List<HeatlandSites.RiverPoint> rivers = new ArrayList<>();
        List<HeatlandSites.Sphere> spheres = new ArrayList<>();
        for (HeatlandSites.Volcano v : volcanoes) {
            for (HeatlandSites.RiverPoint p : v.rivers()) {
                if (p.x() >= minX - 7 && p.x() <= minX + 22 && p.z() >= minZ - 7 && p.z() <= minZ + 22) {
                    rivers.add(p);
                }
            }
            for (HeatlandSites.Sphere s : v.spheres()) {
                if (s.x() + s.r() >= minX && s.x() - s.r() <= minX + 15
                        && s.z() + s.r() >= minZ && s.z() - s.r() <= minZ + 15) {
                    spheres.add(s);
                }
            }
        }

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int sea = server.getSeaLevel();
        boolean placed = false;

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                int surfY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                m.set(x, floorY, z);
                boolean heat = level.getBiome(m).is(SpiritBiomes.HEATLAND);

                HeatlandSites.Cone best = null;
                int coneTop = HeatlandSites.NONE;
                for (HeatlandSites.Cone c : cones) {
                    int t = c.top(x, z);
                    if (t != HeatlandSites.NONE && t > coneTop) {
                        coneTop = t;
                        best = c;
                    }
                }
                boolean volcanic = best != null;

                RiverHit hit = null;
                if (!rivers.isEmpty()) {
                    hit = nearest(rivers, x, z);
                }

                if (!heat && !volcanic && hit == null) {
                    continue;
                }
                placed = true;

                // 1. Озера лави замість води (глибина до 8 блоків).
                boolean lake = false;
                if (heat && surfY > floorY && surfY <= sea) {
                    if (surfY - floorY > 8) {
                        for (int y = floorY + 1; y <= surfY - 8; y++) {
                            put(level, m, x, y, z, rock(x, y, z, seed));
                        }
                        floorY = surfY - 8;
                    }
                    for (int y = floorY + 1; y <= surfY; y++) {
                        put(level, m, x, y, z, LAVA);
                    }
                    put(level, m, x, floorY, z, hash100(x, floorY, z, seed) < 40 ? OBSIDIAN : BASALT);
                    lake = true;
                }
                int top = lake ? surfY : floorY;

                // 2. Конус, кратер.
                boolean carved = false;
                if (volcanic) {
                    if (coneTop > floorY) {
                        for (int y = floorY + 1; y <= coneTop; y++) {
                            put(level, m, x, y, z, y > coneTop - 3 ? surface(x, z, seed) : rock(x, y, z, seed));
                        }
                        top = coneTop;
                    } else if (best.dist(x, z) < best.rim()) {
                        for (int y = coneTop + 1; y <= floorY; y++) {
                            put(level, m, x, y, z, AIR);
                        }
                        put(level, m, x, coneTop, z, surface(x, z, seed));
                        top = coneTop;
                        carved = true;
                    }
                    if (best.dist(x, z) < best.rim() && top < best.lakeLevel()) {
                        for (int y = top + 1; y <= best.lakeLevel(); y++) {
                            put(level, m, x, y, z, LAVA);
                        }
                        top = best.lakeLevel();
                        carved = true;
                    }
                }

                // 3. Порожнини всередині вулкана.
                for (HeatlandSites.Sphere s : spheres) {
                    double dx = x - s.x();
                    double dz = z - s.z();
                    double d2 = dx * dx + dz * dz;
                    double r2 = (double) s.r() * s.r();
                    if (d2 > r2) {
                        continue;
                    }
                    double dy = Math.sqrt(r2 - d2);
                    int y0 = (int) Math.ceil(s.y() - dy);
                    int y1 = (int) Math.floor(s.y() + dy);
                    for (int y = Math.max(y0, level.getMinBuildHeight() + 5); y <= y1 && y <= top; y++) {
                        m.set(x, y, z);
                        BlockState st = level.getBlockState(m);
                        if (st.isAir() || st.is(Blocks.LAVA)) {
                            continue;
                        }
                        level.setBlock(m, y <= s.lavaY() ? LAVA : AIR, 2);
                    }
                }

                // 4. Лавові річки.
                if (hit != null) {
                    HeatlandSites.RiverPoint p = hit.point;
                    int L = p.level();
                    if (hit.dist <= p.halfWidth()) {
                        for (int y = top + 1; y < L; y++) {
                            put(level, m, x, y, z, BASALT);
                        }
                        for (int y = L + 1; y <= top; y++) {
                            put(level, m, x, y, z, AIR);
                        }
                        BlockState bed = LAVA;
                        if (p.crust()) {
                            bed = hash100(x, L, z, seed) < 22 ? MAGMA : BLACKSTONE;
                        }
                        put(level, m, x, L, z, bed);
                        put(level, m, x, L - 1, z, BASALT);
                        top = L;
                        carved = true;
                    } else if (hit.dist <= p.halfWidth() + 1.6 && top < L + 1) {
                        for (int y = top + 1; y <= L + 1; y++) {
                            put(level, m, x, y, z, y == L + 1 ? BLACKSTONE : BASALT);
                        }
                        top = L + 1;
                        carved = true;
                    }
                }

                // 5. Випалена поверхня там, де конус не будував нового верху.
                if ((heat || volcanic) && !lake && !carved && !(volcanic && coneTop > floorY)) {
                    scorch(level, m, x, z, floorY, seed);
                }
            }
        }
        for (HeatlandSites.Volcano v : volcanoes) {
            placeNest(level, m, v, minX, minZ, seed);
        }
        return placed;
    }

    /** Гніздо Жар-птиці на краю кратера: ложе з бородавчастих блоків і світлогриба, низький борт, скриня з нагородою. */
    private static void placeNest(WorldGenLevel level, BlockPos.MutableBlockPos m, HeatlandSites.Volcano v,
                                  int minX, int minZ, long seed) {
        BlockPos nest = HeatlandSites.nestPos(v);
        if (Math.abs(nest.getX() - (minX + 8)) > 11 || Math.abs(nest.getZ() - (minZ + 8)) > 11) {
            return;
        }
        int floor = nest.getY() - 1;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = nest.getX() + dx;
                int z = nest.getZ() + dz;
                if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) {
                    continue;
                }
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 3.2) {
                    continue;
                }
                for (int y = floor - 2; y < floor; y++) {
                    m.set(x, y, z);
                    if (level.getBlockState(m).isAir() || !level.getFluidState(m).isEmpty()) {
                        put(level, m, x, y, z, BASALT);
                    }
                }
                put(level, m, x, floor, z, d <= 1.6 ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.NETHER_WART_BLOCK.defaultBlockState());
                for (int y = nest.getY(); y <= nest.getY() + 4; y++) {
                    put(level, m, x, y, z, AIR);
                }
                if (d > 2.3) {
                    put(level, m, x, nest.getY(), z, Blocks.NETHER_WART_BLOCK.defaultBlockState());
                    if (HeatlandSites.hash(x, 3, z, seed) % 100 < 45) {
                        put(level, m, x, nest.getY() + 1, z, Blocks.NETHER_WART_BLOCK.defaultBlockState());
                    }
                }
            }
        }
        if (nest.getX() >= minX && nest.getX() <= minX + 15 && nest.getZ() >= minZ && nest.getZ() <= minZ + 15) {
            put(level, m, nest.getX(), nest.getY(), nest.getZ(),
                    Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING,
                            net.minecraft.core.Direction.SOUTH));
            net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level,
                    level.getRandom(), nest.immutable(), net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.LOOT_TABLE,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "chests/phoenix_nest")));
        }
    }

    private record RiverHit(HeatlandSites.RiverPoint point, double dist) {
    }

    private static RiverHit nearest(List<HeatlandSites.RiverPoint> points, int x, int z) {
        HeatlandSites.RiverPoint best = null;
        long bd = Long.MAX_VALUE;
        for (HeatlandSites.RiverPoint p : points) {
            long dx = p.x() - x;
            long dz = p.z() - z;
            long d = dx * dx + dz * dz;
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        if (best == null) {
            return null;
        }
        double dist = Math.sqrt(bd);
        return dist <= best.halfWidth() + 1.6 ? new RiverHit(best, dist) : null;
    }

    /** Заміна верхніх шарів землі: чим крутіший схил, тим глибше (скелі стають базальтовими). */
    private static void scorch(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int z, int floorY, long seed) {
        int minN = floorY;
        minN = Math.min(minN, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + 1, z) - 1);
        minN = Math.min(minN, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x - 1, z) - 1);
        minN = Math.min(minN, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z + 1) - 1);
        minN = Math.min(minN, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z - 1) - 1);
        int depth = Math.max(3, Math.min(20, floorY - minN + 2));
        for (int y = floorY; y > floorY - depth && y > level.getMinBuildHeight() + 5; y--) {
            m.set(x, y, z);
            BlockState st = level.getBlockState(m);
            if (st.isAir() || !st.getFluidState().isEmpty() || st.is(Blocks.BEDROCK)) {
                continue;
            }
            level.setBlock(m, y == floorY ? surface(x, z, seed) : rock(x, y, z, seed), 2);
        }
    }

    private static int hash100(int x, int y, int z, long seed) {
        return HeatlandSites.hash(x, y, z, seed) % 100;
    }

    /** Звичайна порода: базальт і чорнокамінь, рідкісні жили позолоченого. */
    private static BlockState rock(int x, int y, int z, long seed) {
        int h = HeatlandSites.hash(x, y, z, seed ^ 0x524F434BL) % 1000;
        if (h < 12) {
            return GILDED;
        }
        return h < 600 ? BASALT : BLACKSTONE;
    }

    /** Верхній блок: обсидіанові поля, плями магми, чорнокамінь і базальт. */
    private static BlockState surface(int x, int z, long seed) {
        double a = HeatlandSites.fbm(x / 48.0, z / 48.0, seed ^ 0x4F425349L);
        if (a > 0.64) {
            return OBSIDIAN;
        }
        double b = HeatlandSites.fbm(x / 30.0, z / 30.0, seed ^ 0x4D41474DL);
        if (b > 0.63) {
            return MAGMA;
        }
        int h = HeatlandSites.hash(x, 0, z, seed ^ 0x53555246L) % 1000;
        if (h < 12) {
            return GILDED;
        }
        if (h < 100) {
            return SMOOTH_BASALT;
        }
        return h < 450 ? BASALT : BLACKSTONE;
    }

    private static void put(WorldGenLevel level, BlockPos.MutableBlockPos m, int x, int y, int z, BlockState state) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return;
        }
        m.set(x, y, z);
        level.setBlock(m, state, 2);
    }
}
