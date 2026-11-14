package com.spiritform.registry;

import com.spiritform.SpiritFormMod;
import com.spiritform.herald.ColdHerald;
import com.spiritform.herald.DeathHerald;
import com.spiritform.herald.HungerHerald;
import com.spiritform.herald.StormHerald;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Реєстрація чотирьох Вісників Потойбіччя (сутності та їхні характеристики). */
@EventBusSubscriber(modid = SpiritFormMod.MODID, bus = Bus.MOD)
public final class SpiritHeraldEntities {

    public static final DeferredHolder<EntityType<?>, EntityType<StormHerald>> STORM =
            SpiritEntities.ENTITY_TYPES.register("storm_herald", () -> EntityType.Builder
                    .of(StormHerald::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.8F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("storm_herald"));

    public static final DeferredHolder<EntityType<?>, EntityType<HungerHerald>> HUNGER =
            SpiritEntities.ENTITY_TYPES.register("hunger_herald", () -> EntityType.Builder
                    .of(HungerHerald::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.9F)
                    .clientTrackingRange(16)
                    .build("hunger_herald"));

    public static final DeferredHolder<EntityType<?>, EntityType<ColdHerald>> COLD =
            SpiritEntities.ENTITY_TYPES.register("cold_herald", () -> EntityType.Builder
                    .of(ColdHerald::new, MobCategory.MONSTER)
                    .sized(1.3F, 2.6F)
                    .immuneTo(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
                    .clientTrackingRange(16)
                    .build("cold_herald"));

    public static final DeferredHolder<EntityType<?>, EntityType<DeathHerald>> DEATH =
            SpiritEntities.ENTITY_TYPES.register("death_herald", () -> EntityType.Builder
                    .of(DeathHerald::new, MobCategory.MONSTER)
                    .sized(1.4F, 3.4F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("death_herald"));

    private SpiritHeraldEntities() {
    }

    /** Викликається з конструктора мода, щоб реєстрація відбулася вчасно. */
    public static void init() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(STORM.get(), StormHerald.attributes().build());
        event.put(HUNGER.get(), HungerHerald.attributes().build());
        event.put(COLD.get(), ColdHerald.attributes().build());
        event.put(DEATH.get(), DeathHerald.attributes().build());
    }
}
