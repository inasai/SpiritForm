package com.spiritform.herald;

import com.spiritform.form.SpiritFormState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * Вісник Холоду. Повільний великий велетень. Навколо нього локальна зима, а холод і сповільнення посилюються,
 * чим ближче гравець (SpiritHeraldWorld.tickCold). Удари накладають сповільнення й заморожують, як сипкий сніг.
 */
public class ColdHerald extends HeraldBase {

    public ColdHerald(EntityType<? extends ColdHerald> type, Level level) {
        super(type, level, HeraldType.COLD);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (target instanceof LivingEntity living
                && (hit || (living instanceof net.minecraft.world.entity.player.Player player && SpiritFormState.isActive(player)))) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
            living.setTicksFrozen(Math.max(living.getTicksFrozen(), living.getTicksRequiredToFreeze() + 60));
        }
        return hit;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected void tickHerald(ServerLevel level) {
        if (tickCount % 100 == 1) {
            SpiritHeraldWorld.keepWeather(level, false);
        }
        SpiritHeraldWorld.tickCold(this, level);
    }

    @Override
    protected SoundEvent ambient() {
        return SoundEvents.STRAY_AMBIENT;
    }

    @Override
    protected SoundEvent hurtSound() {
        return SoundEvents.STRAY_HURT;
    }

    @Override
    protected SoundEvent deathSound() {
        return SoundEvents.STRAY_DEATH;
    }
}
