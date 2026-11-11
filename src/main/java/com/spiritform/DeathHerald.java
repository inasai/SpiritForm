package com.spiritform;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Вісник Смерті, фінальний бос. Дуже сильний у ближньому бою (орієнтир: три удари вбивають гравця без броні),
 * не зважає на невразливість форми Привида (SpiritServerEvents.onIncomingDamage) і лікується, б'ючи звичайних істот
 * (SpiritHeralds.onDamageDealt). Нежить і душі Армії Потойбіччя його не лікують: їх він бити може, але без вигоди.
 * Ціллю обирає гравців, потім душі Армії, потім інших живих істот поблизу.
 */
public class DeathHerald extends HeraldBase {

    public DeathHerald(EntityType<? extends DeathHerald> type, Level level) {
        super(type, level, HeraldType.DEATH);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Душі Армії Потойбіччя
        this.targetSelector.addGoal(3, new HeraldTargetGoal(this, 24.0, SpiritArmy::isMinion));
        // Інші живі істоти (вони його лікують), крім нежиті, Вісників і гравців
        this.targetSelector.addGoal(4, new HeraldTargetGoal(this, 16.0, e ->
                !(e instanceof HeraldBase) && !(e instanceof Player) && !SpiritArmy.isMinion(e)
                        && !e.getType().is(EntityTypeTags.UNDEAD)));
    }

    @Override
    protected void tickHerald(ServerLevel level) {
        if (tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.6, getZ(), 2, 0.6, 1.2, 0.6, 0.01);
        }
    }

    @Override
    protected SoundEvent ambient() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent hurtSound() {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent deathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
