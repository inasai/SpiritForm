package com.spiritform.entity;

import com.spiritform.registry.SpiritCreatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;

/**
 * Хуха: великий лісовий кіт. Уся поведінка котячої (приручення рибою, мурчання, втеча від гравців, полювання
 * на кролів, відлякування кріперів), лише тіло завбільшки з вівцю. Розмір задають хітбокс і рендерер.
 */
public class Hukha extends Cat {

    public Hukha(EntityType<? extends Cat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Cat.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Приручена Хуха нападає на Злидня, коли той поряд
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, Zlydeny.class, 14.0, true,
                e -> this.isTame() && !this.isOrderedToSit()));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.4, true) {
            @Override
            public boolean canUse() {
                return Hukha.this.isTame() && !Hukha.this.isOrderedToSit()
                        && Hukha.this.getTarget() instanceof Zlydeny && super.canUse();
            }
        });
    }

    /** Кошеня теж Хуха (а не звичайний кіт). */
    @Override
    public Cat getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        Hukha baby = SpiritCreatures.HUKHA.get().create(level);
        if (baby != null && this.isTame()) {
            baby.setOwnerUUID(this.getOwnerUUID());
            baby.setTame(true, true);
        }
        return baby;
    }
}
