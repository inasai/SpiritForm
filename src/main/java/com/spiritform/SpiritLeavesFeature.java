package com.spiritform;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Розсипає по кронах дерев Лісу Ночі особливе листя (воно віддає Яблуко Духів). */
public class SpiritLeavesFeature extends Feature<NoneFeatureConfiguration> {

    public SpiritLeavesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        // Шукає найближче листя під точкою (верхівка крони)
        for (int dy = 1; dy <= 10; dy++) {
            BlockPos pos = origin.below(dy);
            BlockState old = level.getBlockState(pos);
            if (old.getBlock() instanceof LeavesBlock && !(old.getBlock() instanceof SpiritLeavesBlock)) {
                replace(level, pos, old);
                // Іноді ще одне-два поруч
                for (int i = 0; i < 2; i++) {
                    if (context.random().nextBoolean()) {
                        BlockPos next = pos.offset(context.random().nextInt(3) - 1, context.random().nextInt(2) - 1,
                                context.random().nextInt(3) - 1);
                        BlockState nextOld = level.getBlockState(next);
                        if (nextOld.getBlock() instanceof LeavesBlock && !(nextOld.getBlock() instanceof SpiritLeavesBlock)) {
                            replace(level, next, nextOld);
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static void replace(WorldGenLevel level, BlockPos pos, BlockState old) {
        BlockState state = SpiritBlocks.SPIRIT_LEAVES.get().defaultBlockState();
        if (old.hasProperty(LeavesBlock.DISTANCE)) {
            state = state.setValue(LeavesBlock.DISTANCE, old.getValue(LeavesBlock.DISTANCE));
        }
        if (old.hasProperty(LeavesBlock.PERSISTENT)) {
            state = state.setValue(LeavesBlock.PERSISTENT, old.getValue(LeavesBlock.PERSISTENT));
        }
        level.setBlock(pos, state, 2);
    }
}
