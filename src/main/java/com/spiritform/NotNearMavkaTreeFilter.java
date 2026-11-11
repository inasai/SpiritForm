package com.spiritform;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Фільтр розміщення: не дає рослинності й іншим деревам з'явитися біля Дерева Мавки.
 * radius = скільки блоків від центру дерева лишається порожнім для цієї фічі (для великих дерев більше).
 */
public class NotNearMavkaTreeFilter extends PlacementFilter {

    public static final MapCodec<NotNearMavkaTreeFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(com.mojang.serialization.Codec.INT.fieldOf("radius").forGetter(f -> f.radius))
            .apply(instance, NotNearMavkaTreeFilter::new));

    private final int radius;

    public NotNearMavkaTreeFilter(int radius) {
        this.radius = radius;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        if (MavkaTrees.isNear(context.getLevel().getSeed(), pos.getX(), pos.getZ(), radius)) {
            return false;
        }
        // Під Титанічним деревом голий ґрунт у радіусі 20 блоків, тому тут запас більший
        return TitanTrees.nearestValid(context.getLevel().getLevel(), pos.getX(), pos.getZ(),
                radius + 10) == null;
    }

    @Override
    public PlacementModifierType<?> type() {
        return SpiritWorldgen.NOT_NEAR_MAVKA_TREE.get();
    }
}
