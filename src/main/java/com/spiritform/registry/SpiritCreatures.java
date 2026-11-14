package com.spiritform.registry;

import com.spiritform.SpiritFormMod;
import com.spiritform.entity.Bahatko;
import com.spiritform.entity.Bisyk;
import com.spiritform.entity.Chort;
import com.spiritform.entity.Chuhaister;
import com.spiritform.entity.Hukha;
import com.spiritform.entity.Klunnyk;
import com.spiritform.entity.Lisovyk;
import com.spiritform.entity.Mavka;
import com.spiritform.entity.Nichnytsia;
import com.spiritform.entity.Perelisnyk;
import com.spiritform.entity.Phoenix;
import com.spiritform.entity.Polovyk;
import com.spiritform.entity.Potercha;
import com.spiritform.entity.Povitrulia;
import com.spiritform.entity.Skarbnyk;
import com.spiritform.entity.TemplateCreature;
import com.spiritform.entity.Vodianyk;
import com.spiritform.entity.Vodianytsia;
import com.spiritform.entity.Vovkulaka;
import com.spiritform.entity.Zlydeny;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
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

    // ----- ІСТОТА: Мавка (охоронниця Дерева Мавки) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Mavka>> MAVKA =
            SpiritEntities.ENTITY_TYPES.register("mavka", () -> EntityType.Builder
                    .of(Mavka::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F)
                    .clientTrackingRange(10)
                    .build("mavka"));

    public static final DeferredItem<Item> MAVKA_EGG = SpiritItems.ITEMS.registerItem(
            "mavka_spawn_egg",
            properties -> new DeferredSpawnEggItem(MAVKA, 0x5FA85A, 0x2B4A2F, properties),
            new Item.Properties());

    // ----- ІСТОТА: Чугайстер (лісовий велетень) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Chuhaister>> CHUHAISTER =
            SpiritEntities.ENTITY_TYPES.register("chuhaister", () -> EntityType.Builder
                    .of(Chuhaister::new, MobCategory.MONSTER)
                    .sized(2.0F, 5.4F)
                    .eyeHeight(4.7F)
                    .clientTrackingRange(16)
                    .build("chuhaister"));

    public static final DeferredItem<Item> CHUHAISTER_EGG = SpiritItems.ITEMS.registerItem(
            "chuhaister_spawn_egg",
            properties -> new DeferredSpawnEggItem(CHUHAISTER, 0x4A3A28, 0xC9A24A, properties),
            new Item.Properties());

    // ----- ІСТОТА: Хуха (великий лісовий кіт) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Hukha>> HUKHA =
            SpiritEntities.ENTITY_TYPES.register("hukha", () -> EntityType.Builder
                    .of(Hukha::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.2F)
                    .eyeHeight(1.0F)
                    .clientTrackingRange(8)
                    .build("hukha"));

    public static final DeferredItem<Item> HUKHA_EGG = SpiritItems.ITEMS.registerItem(
            "hukha_spawn_egg",
            properties -> new DeferredSpawnEggItem(HUKHA, 0x6E7B52, 0xE0B040, properties),
            new Item.Properties());

    // ----- ІСТОТА: Бісик (літаючий світлячок-пустун) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Bisyk>> BISYK =
            SpiritEntities.ENTITY_TYPES.register("bisyk", () -> EntityType.Builder
                    .of(Bisyk::new, MobCategory.AMBIENT)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .build("bisyk"));

    public static final DeferredItem<Item> BISYK_EGG = SpiritItems.ITEMS.registerItem(
            "bisyk_spawn_egg",
            properties -> new DeferredSpawnEggItem(BISYK, 0x1E2A22, 0xC8F55A, properties),
            new Item.Properties());

    // ----- ІСТОТА: Скарбник (сірий гоблін-злодій) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Skarbnyk>> SKARBNYK =
            SpiritEntities.ENTITY_TYPES.register("skarbnyk", () -> EntityType.Builder
                    .of(Skarbnyk::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.0F)
                    .clientTrackingRange(8)
                    .build("skarbnyk"));

    public static final DeferredItem<Item> SKARBNYK_EGG = SpiritItems.ITEMS.registerItem(
            "skarbnyk_spawn_egg",
            properties -> new DeferredSpawnEggItem(SKARBNYK, 0x7C838A, 0x6B4E32, properties),
            new Item.Properties());

    // ----- ІСТОТА: Клунник (старий дід біля лавових озер Жарокраю) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Klunnyk>> KLUNNYK =
            SpiritEntities.ENTITY_TYPES.register("klunnyk", () -> EntityType.Builder
                    .of(Klunnyk::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.7F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("klunnyk"));

    public static final DeferredItem<Item> KLUNNYK_EGG = SpiritItems.ITEMS.registerItem(
            "klunnyk_spawn_egg",
            properties -> new DeferredSpawnEggItem(KLUNNYK, 0x8A2A10, 0xF5B03A, properties),
            new Item.Properties());

    // ----- ІСТОТА: Жар-птиця (гігантський птах над кратером вулкана) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Phoenix>> PHOENIX =
            SpiritEntities.ENTITY_TYPES.register("phoenix", () -> EntityType.Builder
                    .of(Phoenix::new, MobCategory.CREATURE)
                    .sized(2.4F, 2.0F)
                    .fireImmune()
                    .passengerAttachments(2.3F)
                    .clientTrackingRange(10)
                    .build("phoenix"));

    public static final DeferredItem<Item> PHOENIX_EGG = SpiritItems.ITEMS.registerItem(
            "phoenix_spawn_egg",
            properties -> new DeferredSpawnEggItem(PHOENIX, 0xF5A623, 0xD0361A, properties),
            new Item.Properties());

    /** Перо Жар-птиці: поки воно в інвентарі вбивці, Гнів Перуна шкодить (див. PerunWrath). */
    public static final DeferredItem<Item> PHOENIX_FEATHER = SpiritItems.ITEMS.registerSimpleItem(
            "phoenix_feather", new Item.Properties());

    // ----- ІСТОТА: Багатько (вогняна кулька Жарокраю) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Bahatko>> BAHATKO =
            SpiritEntities.ENTITY_TYPES.register("bahatko", () -> EntityType.Builder
                    .of(Bahatko::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("bahatko"));

    public static final DeferredItem<Item> BAHATKO_EGG = SpiritItems.ITEMS.registerItem(
            "bahatko_spawn_egg",
            properties -> new DeferredSpawnEggItem(BAHATKO, 0xF28C1C, 0xFFE066, properties),
            new Item.Properties());

    /** Жаринка: пальне, що горить утричі довше за вугілля (data_maps/item/furnace_fuels.json). */
    public static final DeferredItem<Item> BAHATKO_EMBER = SpiritItems.ITEMS.registerSimpleItem(
            "bahatko_ember", new Item.Properties());

    // ----- ІСТОТА: Злидень (виглядає як Хуха, але кусає й тікає) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Zlydeny>> ZLYDENY =
            SpiritEntities.ENTITY_TYPES.register("zlydeny", () -> EntityType.Builder
                    .of(Zlydeny::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.2F)
                    .eyeHeight(1.0F)
                    .clientTrackingRange(8)
                    .build("zlydeny"));

    public static final DeferredItem<Item> ZLYDENY_EGG = SpiritItems.ITEMS.registerItem(
            "zlydeny_spawn_egg",
            properties -> new DeferredSpawnEggItem(ZLYDENY, 0x6E7B52, 0xB03030, properties),
            new Item.Properties());

    // ----- ІСТОТА: Водяник (господар річки) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Vodianyk>> VODIANYK =
            SpiritEntities.ENTITY_TYPES.register("vodianyk", () -> EntityType.Builder
                    .of(Vodianyk::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.3F)
                    .eyeHeight(2.0F)
                    .clientTrackingRange(10)
                    .build("vodianyk"));

    public static final DeferredItem<Item> VODIANYK_EGG = SpiritItems.ITEMS.registerItem(
            "vodianyk_spawn_egg",
            properties -> new DeferredSpawnEggItem(VODIANYK, 0x4F7A5C, 0xC9B458, properties),
            new Item.Properties());

    // ----- ІСТОТА: Водяниця (русалка, що заманює до води) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Vodianytsia>> VODIANYTSIA =
            SpiritEntities.ENTITY_TYPES.register("vodianytsia", () -> EntityType.Builder
                    .of(Vodianytsia::new, MobCategory.WATER_AMBIENT)
                    .sized(0.6F, 1.1F)
                    .eyeHeight(0.9F)
                    .clientTrackingRange(8)
                    .build("vodianytsia"));

    public static final DeferredItem<Item> VODIANYTSIA_EGG = SpiritItems.ITEMS.registerItem(
            "vodianytsia_spawn_egg",
            properties -> new DeferredSpawnEggItem(VODIANYTSIA, 0x3FA6A0, 0xD8E6B8, properties),
            new Item.Properties());

    // ----- ІСТОТА: Повітруля (літаюча змія лісу ночі) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Povitrulia>> POVITRULIA =
            SpiritEntities.ENTITY_TYPES.register("povitrulia", () -> EntityType.Builder
                    .of(Povitrulia::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .build("povitrulia"));

    public static final DeferredItem<Item> POVITRULIA_EGG = SpiritItems.ITEMS.registerItem(
            "povitrulia_spawn_egg",
            properties -> new DeferredSpawnEggItem(POVITRULIA, 0x2E2A3A, 0xF08A24, properties),
            new Item.Properties());

    // ----- ІСТОТА: Перелісник (тінь лісу ночі) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Perelisnyk>> PERELISNYK =
            SpiritEntities.ENTITY_TYPES.register("perelisnyk", () -> EntityType.Builder
                    .of(Perelisnyk::new, MobCategory.MONSTER)
                    .sized(0.7F, 3.0F)
            .eyeHeight(2.7F)
                    .clientTrackingRange(10)
                    .build("perelisnyk"));

    public static final DeferredItem<Item> PERELISNYK_EGG = SpiritItems.ITEMS.registerItem(
            "perelisnyk_spawn_egg",
            properties -> new DeferredSpawnEggItem(PERELISNYK, 0x1C1A22, 0xD9D4C0, properties),
            new Item.Properties());

    // ----- ІСТОТА: Нічниця (темна сестра Мавки) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Nichnytsia>> NICHNYTSIA =
            SpiritEntities.ENTITY_TYPES.register("nichnytsia", () -> EntityType.Builder
                    .of(Nichnytsia::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F)
                    .clientTrackingRange(10)
                    .build("nichnytsia"));

    public static final DeferredItem<Item> NICHNYTSIA_EGG = SpiritItems.ITEMS.registerItem(
            "nichnytsia_spawn_egg",
            properties -> new DeferredSpawnEggItem(NICHNYTSIA, 0x2A2450, 0x9A7BE0, properties),
            new Item.Properties());

    // ----- ІСТОТА: Потерча (бліда дівчинка туманного лісу) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Potercha>> POTERCHA =
            SpiritEntities.ENTITY_TYPES.register("potercha", () -> EntityType.Builder
                    .of(Potercha::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.6F)
                    .clientTrackingRange(10)
                    .build("potercha"));

    public static final DeferredItem<Item> POTERCHA_EGG = SpiritItems.ITEMS.registerItem(
            "potercha_spawn_egg",
            properties -> new DeferredSpawnEggItem(POTERCHA, 0xE6E8F0, 0x3A4A6A, properties),
            new Item.Properties());

    // ----- ІСТОТА: Польовик (господар Рівнин) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Polovyk>> POLOVYK =
            SpiritEntities.ENTITY_TYPES.register("polovyk", () -> EntityType.Builder
                    .of(Polovyk::new, MobCategory.CREATURE)
                    .sized(0.7F, 1.9F)
            .eyeHeight(1.7F)
                    .clientTrackingRange(10)
                    .build("polovyk"));

    public static final DeferredItem<Item> POLOVYK_EGG = SpiritItems.ITEMS.registerItem(
            "polovyk_spawn_egg",
            properties -> new DeferredSpawnEggItem(POLOVYK, 0xF2EBC8, 0xE8B830, properties),
            new Item.Properties());

    // ----- ІСТОТА: Вовкулака (чорний вовк завбільшки з коня) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Vovkulaka>> VOVKULAKA =
            SpiritEntities.ENTITY_TYPES.register("vovkulaka", () -> EntityType.Builder
                    .of(Vovkulaka::new, MobCategory.CREATURE)
                    .sized(1.3F, 1.6F)
            .eyeHeight(1.3F)
                    .clientTrackingRange(10)
                    .build("vovkulaka"));

    public static final DeferredItem<Item> VOVKULAKA_EGG = SpiritItems.ITEMS.registerItem(
            "vovkulaka_spawn_egg",
            properties -> new DeferredSpawnEggItem(VOVKULAKA, 0x15151C, 0xC8A030, properties),
            new Item.Properties());

    // ----- ІСТОТА: Чорт (дівчина в білому, що блукає полем) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Chort>> CHORT =
            SpiritEntities.ENTITY_TYPES.register("chort", () -> EntityType.Builder
                    .of(Chort::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
            .eyeHeight(1.6F)
                    .clientTrackingRange(10)
                    .build("chort"));

    public static final DeferredItem<Item> CHORT_EGG = SpiritItems.ITEMS.registerItem(
            "chort_spawn_egg",
            properties -> new DeferredSpawnEggItem(CHORT, 0xF4F4F8, 0x2A2A38, properties),
            new Item.Properties());

    // ----- ІСТОТА: Лісовик (велетень у моху на кордоні Лісу) -----
    public static final DeferredHolder<EntityType<?>, EntityType<Lisovyk>> LISOVYK =
            SpiritEntities.ENTITY_TYPES.register("lisovyk", () -> EntityType.Builder
                    .of(Lisovyk::new, MobCategory.CREATURE)
                    .sized(1.6F, 4.0F)
            .eyeHeight(3.5F)
                    .clientTrackingRange(16)
                    .build("lisovyk"));

    public static final DeferredItem<Item> LISOVYK_EGG = SpiritItems.ITEMS.registerItem(
            "lisovyk_spawn_egg",
            properties -> new DeferredSpawnEggItem(LISOVYK, 0x4E6B34, 0x3A2A1C, properties),
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
        event.put(MAVKA.get(), Mavka.createAttributes().build());
        event.put(CHUHAISTER.get(), Chuhaister.createAttributes().build());
        event.put(HUKHA.get(), Hukha.createAttributes().build());
        event.put(BISYK.get(), Bisyk.createAttributes().build());
        event.put(SKARBNYK.get(), Skarbnyk.createAttributes().build());
        event.put(KLUNNYK.get(), Klunnyk.createAttributes().build());
        event.put(BAHATKO.get(), Bahatko.createAttributes().build());
        event.put(PHOENIX.get(), Phoenix.createAttributes().build());
        event.put(ZLYDENY.get(), Zlydeny.createAttributes().build());
        event.put(VODIANYK.get(), Vodianyk.createAttributes().build());
        event.put(VODIANYTSIA.get(), Vodianytsia.createAttributes().build());
        event.put(POVITRULIA.get(), Povitrulia.createAttributes().build());
        event.put(PERELISNYK.get(), Perelisnyk.createAttributes().build());
        event.put(NICHNYTSIA.get(), Nichnytsia.createAttributes().build());
        event.put(POTERCHA.get(), Potercha.createAttributes().build());
        event.put(POLOVYK.get(), Polovyk.createAttributes().build());
        event.put(VOVKULAKA.get(), Vovkulaka.createAttributes().build());
        event.put(CHORT.get(), Chort.createAttributes().build());
        event.put(LISOVYK.get(), Lisovyk.createAttributes().build());
    }

    // ===== 3. МІСЦЕ ПОЯВИ (де саме істота може стати: на землі, у темряві тощо) =====

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TEMPLATE_CREATURE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        // Мавка з'являється лише разом із Деревом Мавки, тож природного місця появи не має
        event.register(CHUHAISTER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(HUKHA.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        // Скарбник з'являється лише поруч із Бісиками (див. ForestEvents), Злидень як монстр, але вдень теж
        event.register(SKARBNYK.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        // Клунник з'являється лише в Жарокраї поблизу лави
        event.register(KLUNNYK.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Klunnyk::checkSpawn,
                RegisterSpawnPlacementsEvent.Operation.OR);
        // Багатько літає над Жарокраєм
        event.register(BAHATKO.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Bahatko::checkSpawn,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(ZLYDENY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        // Водяник з'являється лише через RiverEvents; Водяниці живуть у воді й з'являються природно
        event.register(VODIANYTSIA.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.OCEAN_FLOOR,
                (type, level, reason, pos, random) -> level.getFluidState(pos).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.OR);
        // Істоти лісу ночі й туманного лісу: світло не рахуємо (вони з'являються й удень)
        event.register(PERELISNYK.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(NICHNYTSIA.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(POTERCHA.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        // Рівнини: Польовик з'являється лише разом із центром біому; Вовкулака й Чорт блукають полем
        event.register(VOVKULAKA.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(CHORT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        // Лісовик з'являється лише в лісі, поруч із його кордоном
        event.register(LISOVYK.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Mob.checkMobSpawnRules(type, level, reason, pos, random)
                        && Lisovyk.atForestBorder(level, pos),
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(POVITRULIA.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos).isAir(),
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(BISYK.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos).isAir(),
                RegisterSpawnPlacementsEvent.Operation.OR);
    }
}
