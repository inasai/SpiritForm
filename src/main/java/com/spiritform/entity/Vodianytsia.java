package com.spiritform.entity;

import com.spiritform.registry.SpiritEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Водяниця: дівчина-русалка. Живе у воді, а гравця, що опинився поруч, заманює: тягне до води й не дає стрибати
 * (ефект Заманювання). Зазвичай тікає, коли гравець ближче ніж 3 блоки; сміливі (близько чверті) нападають,
 * але шкода дуже мала.
 */
public class Vodianytsia extends WaterAnimal {

    /** Радіус, у якому Водяниця заманює гравців. */
    private static final double LURE_RANGE = 32.0;
    /** Відстань, ближче якої гравця Водяниця тікає. */
    private static final double FLEE_RANGE = 3.0;

    private boolean brave;

    public Vodianytsia(EntityType<? extends Vodianytsia> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FleeInWaterGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0, 40));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, Player.class, 10.0, false, e -> this.brave));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnGroupData) {
        this.brave = level.getRandom().nextFloat() < 0.25F;
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Brave", this.brave);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.brave = tag.getBoolean("Brave");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.isAlive() && this.isInWater() && this.tickCount % 10 == 0) {
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(LURE_RANGE),
                    EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
                if (player.distanceToSqr(this) <= LURE_RANGE * LURE_RANGE) {
                    player.addEffect(new MobEffectInstance(SpiritEffects.LURE, 40, 0, true, false, true), this);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(SpiritEffects.LURE, 100, 0), this);
        }
        return hit;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.AXOLOTL_IDLE_WATER : SoundEvents.AXOLOTL_IDLE_AIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    /** Тікає у воді від гравця, що підійшов ближче FLEE_RANGE (крім сміливих). */
    private static class FleeInWaterGoal extends Goal {

        private final Vodianytsia mob;
        private Player threat;
        private Vec3 destination;

        FleeInWaterGoal(Vodianytsia mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.mob.brave || !this.mob.isInWater()) {
                return false;
            }
            this.threat = this.mob.level().getNearestPlayer(this.mob, FLEE_RANGE);
            if (this.threat == null || this.threat.isSpectator()) {
                return false;
            }
            this.destination = findWaterAway();
            return this.destination != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.threat != null && this.threat.isAlive() && !this.mob.getNavigation().isDone()
                    && this.mob.distanceToSqr(this.threat) < 100.0;
        }

        @Override
        public void start() {
            this.mob.getNavigation().moveTo(this.destination.x, this.destination.y, this.destination.z, 1.6);
        }

        @Override
        public void stop() {
            this.threat = null;
            this.destination = null;
        }

        /** Точка у воді на 6..9 блоків від гравця у протилежному напрямку, або null. */
        private Vec3 findWaterAway() {
            Vec3 away = this.mob.position().subtract(this.threat.position());
            double len = Math.sqrt(away.x * away.x + away.z * away.z);
            if (len < 0.01) {
                away = new Vec3(1.0, 0.0, 0.0);
                len = 1.0;
            }
            for (int i = 0; i < 12; i++) {
                double angle = (this.mob.getRandom().nextDouble() - 0.5) * Math.PI * 0.9;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double dx = (away.x * cos - away.z * sin) / len;
                double dz = (away.x * sin + away.z * cos) / len;
                double dist = 6.0 + this.mob.getRandom().nextDouble() * 3.0;
                BlockPos pos = BlockPos.containing(this.mob.getX() + dx * dist,
                        this.mob.getY() + this.mob.getRandom().nextInt(3) - 1, this.mob.getZ() + dz * dist);
                if (this.mob.level().getFluidState(pos).is(FluidTags.WATER)) {
                    return Vec3.atCenterOf(pos);
                }
            }
            return null;
        }
    }
}
