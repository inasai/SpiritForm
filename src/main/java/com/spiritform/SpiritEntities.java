package com.spiritform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Сутності мода. */
public final class SpiritEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SpiritFormMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SoulShardEntity>> SOUL_SHARD =
            ENTITY_TYPES.register("soul_shard", () -> EntityType.Builder
                    .<SoulShardEntity>of(SoulShardEntity::new, MobCategory.MISC)
                    .sized(0.9F, 1.6F)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("soul_shard"));

    private SpiritEntities() {
    }
}
