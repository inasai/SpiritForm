package com.spiritform.herald;

import com.spiritform.form.SpiritFormState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
 * Вісник Голоду. Швидкий і стійкий: поки він рухається, шкоди отримує лише чверть (броня з голоду).
 * Коли Коса Потойбіччя його зупиняє, він беззахисний, і саме тоді його треба бити.
 * Кожен вдалий удар забирає два значки голоду й накладає ефект голоду.
 * Над територією йде кривавий дощ (SpiritHeraldWorld.keepWeather, а колір дощу задає клієнт).
 */
public class HungerHerald extends HeraldBase {

    /** Скільки шкоди проходить, поки Вісник не зупинений (решту поглинає його голодна броня). */
    private static final float RESIST = 0.25F;

    public HungerHerald(EntityType<? extends HungerHerald> type, Level level) {
        super(type, level, HeraldType.HUNGER);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Зупинений (NoAI від Коси) Вісник приймає повну шкоду, рухомий лише чверть. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isNoAi() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.6, getZ(), 2, 0.3, 0.3, 0.3, 0.1);
            }
            amount *= RESIST;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        // Форма Привида зупиняє саму шкоду, але голод від дотику Вісника проходить і в ній
        if (target instanceof Player player && !player.isCreative() && (hit || SpiritFormState.isActive(player))) {
            int food = player.getFoodData().getFoodLevel();
            player.getFoodData().setFoodLevel(Math.max(0, food - 4));
            player.getFoodData().setSaturation(0.0F);
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 2));
        }
        return hit;
    }

    @Override
    protected void tickHerald(ServerLevel level) {
        if (tickCount % 100 == 1) {
            SpiritHeraldWorld.keepWeather(level, false);
        }
        if (tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1.4, getZ(), 3, 0.5, 0.9, 0.5, 0.02);
        }
    }

    @Override
    protected SoundEvent ambient() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent hurtSound() {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent deathSound() {
        return SoundEvents.HUSK_DEATH;
    }
}
