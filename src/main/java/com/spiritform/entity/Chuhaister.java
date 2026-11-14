package com.spiritform.entity;

import com.spiritform.SpiritFormMod;
import com.spiritform.worldgen.MavkaTrees;
import com.spiritform.worldgen.TitanTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Чугайстер: велетень, що блукає лісом. Кожен удар завдає великої шкоди й накладає сповільнення.
 * Мисливець на Мавок: гравця ігнорує, доки той не завдасть йому шкоди.
 */
public class Chuhaister extends Monster {

    public Chuhaister(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.STEP_HEIGHT, 1.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Чугайстер мисливець на Мавок: чує їх здалеку й нападає, навіть якщо за деревами не видно.
        // Гравця ігнорує, поки той не завдасть йому шкоди (це робить HurtByTargetGoal).
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, Mavka.class, 48.0, false, e -> true));
    }

    private static final ResourceLocation WEAK_ID = ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "tree_weakness");
    /** Радіуси, у яких Чугайстер слабшає: біля Дерева Мавки й біля Титанічного дерева. */
    private static final int WEAK_RADIUS_MAVKA = 45;
    private static final int WEAK_RADIUS_TITAN = 90;
    private boolean weakened;

    /** Біля дерев Мавок Чугайстер слабшає: б'є вп'ятеро слабше, повільніший і без броні. */
    private void updateWeakness() {
        boolean weak = false;
        if (this.level() instanceof ServerLevel server) {
            long seed = server.getSeed();
            weak = MavkaTrees.isNear(seed, getBlockX(), getBlockZ(), WEAK_RADIUS_MAVKA)
                    || TitanTrees.isNearSite(seed, getBlockX(), getBlockZ(), WEAK_RADIUS_TITAN);
        }
        if (weak == weakened) {
            return;
        }
        weakened = weak;
        modifier(Attributes.ATTACK_DAMAGE, -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, weak);
        modifier(Attributes.MOVEMENT_SPEED, -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, weak);
        modifier(Attributes.ARMOR, -6.0, AttributeModifier.Operation.ADD_VALUE, weak);
    }

    private void modifier(Holder<Attribute> attribute, double amount, AttributeModifier.Operation op, boolean on) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (on) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(WEAK_ID, amount, op));
        } else {
            instance.removeModifier(WEAK_ID);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            updateWeakness();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2), this);
        }
        return hit;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.9F, 0.6F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }
}
