package com.spiritform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Блок Ловця Душ (ліхтар) і його блок-сутність, що тримає спійманого моба. */
public final class SpiritBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SpiritFormMod.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SpiritFormMod.MODID);

    /** Світиться, як факел душ (рівень 10), лише якщо всередині моб. */
    public static final DeferredBlock<SoulCatcherBlock> SOUL_CATCHER = BLOCKS.registerBlock(
            "soul_catcher",
            SoulCatcherBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0F)
                    .sound(SoundType.LANTERN)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(SoulCatcherBlock.LIT) ? 10 : 0));

    /** Відлуння Потойбіччя: лишається там, де ввімкнено Відлуння Смерті. Крізь нього можна проходити, зламати не можна. */
    public static final DeferredBlock<EchoBlock> ECHO_BLOCK = BLOCKS.registerBlock(
            "echo_of_the_beyond",
            EchoBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .noCollission()
                    .noOcclusion()
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 10));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoulCatcherBlockEntity>> SOUL_CATCHER_BE =
            BLOCK_ENTITIES.register("soul_catcher", () ->
                    BlockEntityType.Builder.of(SoulCatcherBlockEntity::new, SOUL_CATCHER.get()).build(null));

    private SpiritBlocks() {
    }
}
