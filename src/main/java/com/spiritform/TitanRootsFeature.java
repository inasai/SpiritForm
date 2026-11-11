package com.spiritform;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Корені-вказівники: на землі лісу стирчать дугоподібні корені, зверненні товстим кінцем до найближчого Титанічного дерева
 * (ніби виросли з його боку). Чим далі від дерева, тим їх менше. Усе вкладається в 9 блоків від центру кореня.
 */
public class TitanRootsFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_DIST = 40;
    private static final int MAX_DIST = 260;

    public TitanRootsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        TitanTrees.Site site = TitanTrees.nearestValid(level.getLevel(), origin.getX(), origin.getZ(), MAX_DIST);
        if (site == null) {
            return false;
        }
        double dx = site.center().getX() - origin.getX();
        double dz = site.center().getZ() - origin.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < MIN_DIST || dist > MAX_DIST) {
            return false;
        }
        float chance = 0.1F + 0.5F * (1.0F - (float) ((dist - MIN_DIST) / (MAX_DIST - MIN_DIST)));
        if (random.nextFloat() > chance || MavkaTrees.isNear(level.getSeed(), origin.getX(), origin.getZ(), 14)) {
            return false;
        }

        double ux = dx / dist;
        double uz = dz / dist;
        boolean big = random.nextFloat() < 0.3F;
        int length = big ? 13 : 6;
        double height = big ? 3.6 : 1.7;
        double thick = big ? 1.7 : 0.9;   // радіус біля дерева
        double thin = big ? 0.7 : 0.4;    // радіус на дальньому кінці
        double bend = (random.nextDouble() - 0.5) * (big ? 2.0 : 1.0);

        BlockState wood = Blocks.DARK_OAK_WOOD.defaultBlockState();
        BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
        boolean placed = false;
        int steps = length * 2;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double along = (t - 0.5) * length;
            double sideways = Math.sin(t * Math.PI) * bend;
            double x = origin.getX() + 0.5 + ux * along - uz * sideways;
            double z = origin.getZ() + 0.5 + uz * along + ux * sideways;
            int bx = Mth.floor(x);
            int bz = Mth.floor(z);
            int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx, bz);
            if (ground != level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz)) {
                continue; // вода
            }
            double radius = Mth.lerp(t, thin, thick);
            double cy = ground + Math.sin(t * Math.PI) * height - 0.4;
            int r = (int) Math.ceil(radius);
            for (int ox = -r; ox <= r; ox++) {
                for (int oy = -r; oy <= r; oy++) {
                    for (int oz = -r; oz <= r; oz++) {
                        if (ox * ox + oy * oy + oz * oz > radius * radius + 0.3) {
                            continue;
                        }
                        BlockPos p = BlockPos.containing(x + ox, cy + oy, z + oz);
                        if (Math.abs(p.getX() - origin.getX()) > 9 || Math.abs(p.getZ() - origin.getZ()) > 9) {
                            continue;
                        }
                        BlockState old = level.getBlockState(p);
                        if (old.isAir() || old.canBeReplaced() || old.is(BlockTags.LEAVES)) {
                            level.setBlock(p, random.nextInt(9) == 0 ? moss : wood, 2);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
