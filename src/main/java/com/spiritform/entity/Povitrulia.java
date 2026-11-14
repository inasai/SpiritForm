package com.spiritform.entity;

import java.util.EnumSet;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Повітруля: літаюча змія лісу ночі. Кружляє навколо цілі на відстані й обстрілює її вогняними кулями.
 */
public class Povitrulia extends Monster {

    public Povitrulia(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitAndFireGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, Player.class, 32.0, true, e -> true));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    /** Кружляє на відстані 11 блоків над ціллю й раз на 2-3 секунди кидає вогняну кулю. */
    private static class OrbitAndFireGoal extends Goal {

        private final Povitrulia mob;
        private double angle;
        private int cooldown;

        OrbitAndFireGoal(Povitrulia mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.angle = this.mob.getRandom().nextDouble() * Math.PI * 2.0;
            this.cooldown = 40;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            this.angle += 0.04;
            double px = target.getX() + Math.cos(this.angle) * 11.0;
            double pz = target.getZ() + Math.sin(this.angle) * 11.0;
            double py = target.getY() + 4.0 + Math.sin(this.angle * 2.0);
            this.mob.getMoveControl().setWantedPosition(px, py, pz, 1.0);
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.cooldown <= 0 && this.mob.hasLineOfSight(target) && this.mob.distanceToSqr(target) < 28.0 * 28.0) {
                shoot(target);
                this.cooldown = 50 + this.mob.getRandom().nextInt(30);
            }
        }

        private void shoot(LivingEntity target) {
            Vec3 dir = new Vec3(target.getX() - this.mob.getX(), target.getY(0.5) - this.mob.getEyeY(),
                    target.getZ() - this.mob.getZ()).normalize();
            SmallFireball fireball = new SmallFireball(this.mob.level(), this.mob, dir);
            fireball.setPos(this.mob.getX() + dir.x * 1.5, this.mob.getEyeY() + dir.y * 1.5 - 0.2,
                    this.mob.getZ() + dir.z * 1.5);
            this.mob.level().addFreshEntity(fireball);
            this.mob.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.2F);
        }
    }
}
