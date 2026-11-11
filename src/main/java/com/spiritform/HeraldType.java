package com.spiritform;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/** Види Вісників Потойбіччя. Перші три йдуть по колу (CYCLE), четвертий викликається сердцем. */
public enum HeraldType {
    STORM("storm_herald", BossEvent.BossBarColor.WHITE),
    HUNGER("hunger_herald", BossEvent.BossBarColor.RED),
    COLD("cold_herald", BossEvent.BossBarColor.BLUE),
    DEATH("death_herald", BossEvent.BossBarColor.PURPLE);

    /** Послідовність появи: Буря, Голод, Холод, знову Буря. */
    public static final HeraldType[] CYCLE = {STORM, HUNGER, COLD};

    public final String entityId;
    public final BossEvent.BossBarColor color;

    HeraldType(String entityId, BossEvent.BossBarColor color) {
        this.entityId = entityId;
        this.color = color;
    }

    public static HeraldType byIndex(int index) {
        return CYCLE[Math.floorMod(index, CYCLE.length)];
    }

    public Component displayName() {
        return Component.translatable("entity.spirit_form." + entityId);
    }

    public EntityType<? extends HeraldBase> entityType() {
        return switch (this) {
            case STORM -> SpiritHeraldEntities.STORM.get();
            case HUNGER -> SpiritHeraldEntities.HUNGER.get();
            case COLD -> SpiritHeraldEntities.COLD.get();
            case DEATH -> SpiritHeraldEntities.DEATH.get();
        };
    }

    public Item heart() {
        return switch (this) {
            case STORM -> SpiritItems.STORM_HEART.get();
            case HUNGER -> SpiritItems.HUNGER_HEART.get();
            case COLD -> SpiritItems.COLD_HEART.get();
            case DEATH -> SpiritItems.DEATH_HEART.get();
        };
    }
}
