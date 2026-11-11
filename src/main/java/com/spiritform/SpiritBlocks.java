package com.spiritform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
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

    /** Голови Вісників: трофеї. Створюються з відповідного серця й черепа скелета. */
    public static final DeferredBlock<HeraldHeadBlock> STORM_HEAD = BLOCKS.registerBlock(
            "storm_herald_head",
            HeraldHeadBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0F)
                    .sound(SoundType.BONE_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<HeraldHeadBlock> HUNGER_HEAD = BLOCKS.registerBlock(
            "hunger_herald_head",
            HeraldHeadBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0F)
                    .sound(SoundType.BONE_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<HeraldHeadBlock> COLD_HEAD = BLOCKS.registerBlock(
            "cold_herald_head",
            HeraldHeadBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0F)
                    .sound(SoundType.BONE_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<HeraldHeadBlock> DEATH_HEAD = BLOCKS.registerBlock(
            "death_herald_head",
            HeraldHeadBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0F)
                    .sound(SoundType.BONE_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    // ===== Рослини й листя біомів Потойбіччя =====

    /** Квітка Мавки: прискорює ріст рослин у радіусі 20 блоків. */
    public static final DeferredBlock<MavkaFlowerBlock> MAVKA_FLOWER = BLOCKS.registerBlock(
            "mavka_flower",
            MavkaFlowerBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).lightLevel(state -> 7));

    public static final DeferredBlock<PottedMavkaFlowerBlock> POTTED_MAVKA_FLOWER = BLOCKS.registerBlock(
            "potted_mavka_flower",
            PottedMavkaFlowerBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).lightLevel(state -> 7));

    /** Полудниця: квітка Рівнин Потойбіччя, схожа на сонце. */
    public static final DeferredBlock<PoludnytsiaBlock> POLUDNYTSIA = BLOCKS.registerBlock(
            "poludnytsia",
            PoludnytsiaBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).lightLevel(state -> 10));

    /** Річниця: водяна рослина річок, дає Силу Морів. */
    public static final DeferredBlock<RichnytsiaBlock> RICHNYTSIA = BLOCKS.registerBlock(
            "richnytsia",
            RichnytsiaBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.WET_GRASS)
                    .lightLevel(state -> 6)
                    .pushReaction(PushReaction.DESTROY));

    /** Особливе листя Лісу Ночі: ламається рукою, віддає Яблуко Духів. */
    public static final DeferredBlock<SpiritLeavesBlock> SPIRIT_LEAVES = BLOCKS.registerBlock(
            "spirit_leaves",
            SpiritLeavesBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES).lightLevel(state -> 5));

    /** Папороть туманного лісу: світиться, віддає Перлину папороті. */
    public static final DeferredBlock<GlowFernBlock> GLOW_FERN = BLOCKS.registerBlock(
            "glow_fern",
            GlowFernBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.FERN).lightLevel(state -> 9));

    /** Попіл Жар-птиці: плаский блок на місці її загибелі (див. PhoenixAshBlock). */
    public static final DeferredBlock<PhoenixAshBlock> PHOENIX_ASH = BLOCKS.registerBlock(
            "phoenix_ash",
            PhoenixAshBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.3F)
                    .forceSolidOn()
                    .sound(SoundType.SAND)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(PhoenixAshBlock.LIT) ? 10 : 0)
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoulCatcherBlockEntity>> SOUL_CATCHER_BE =
            BLOCK_ENTITIES.register("soul_catcher", () ->
                    BlockEntityType.Builder.of(SoulCatcherBlockEntity::new, SOUL_CATCHER.get()).build(null));

    private SpiritBlocks() {
    }
}
