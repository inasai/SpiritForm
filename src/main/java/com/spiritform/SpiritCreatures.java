package com.spiritform;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * УСІ ІСТОТИ МОДА В ОДНОМУ МІСЦІ. Для кожної нової істоти додається один такий блок (див. нижче "ІСТОТА: ...")
 * у трьох місцях цього файлу: реєстрація, характеристики й місце появи. Докладно: docs/CREATURES_UK.md.
 * Істоти з'являються лише там, де їх прописано у файлі біома (data/spirit_form/worldgen/biome/beyond.json).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, bus = Bus.MOD)
public final class SpiritCreatures {

    // ===== 1. РЕЄСТРАЦІЯ =====

    // ----- ІСТОТА: TemplateCreature -----
    public static final DeferredHolder<EntityType<?>, EntityType<TemplateCreature>> TEMPLATE_CREATURE =
            SpiritEntities.ENTITY_TYPES.register("template_creature", () -> EntityType.Builder
                    .of(TemplateCreature::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F) // ширина і висота хітбокса
                    .clientTrackingRange(8)
                    .build("template_creature"));

    /** Яйце виклику для перевірки (є у творчій вкладці). */
    public static final DeferredItem<Item> TEMPLATE_CREATURE_EGG = SpiritItems.ITEMS.registerItem(
            "template_creature_spawn_egg",
            properties -> new DeferredSpawnEggItem(TEMPLATE_CREATURE, 0x8CFFC8, 0x203C32, properties),
            new Item.Properties());

    private SpiritCreatures() {
    }

    /** Викликається з конструктора мода, щоб реєстрація відбулася вчасно. */
    public static void init() {
    }

    // ===== 2. ХАРАКТЕРИСТИКИ =====

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TEMPLATE_CREATURE.get(), TemplateCreature.createAttributes().build());
    }

    // ===== 3. МІСЦЕ ПОЯВИ (де саме істота може стати: на землі, у темряві тощо) =====

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TEMPLATE_CREATURE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
    }
}
