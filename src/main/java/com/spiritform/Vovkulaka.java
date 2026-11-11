package com.spiritform;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Вовкулака: величезний чорний вовк розміром із коня. Дикий першим не нападає, а лише стежить за гравцем, що підійшов
 * ближче ніж на 10 блоків, і може кинутися на нього. Приручається зачарованим золотим яблуком (напевно) або золотим
 * (випадково, як коти й собаки). Приручений допомагає у боях, а власник може їздити на ньому верхи.
 */
public class Vovkulaka extends TamableAnimal implements net.minecraft.world.entity.PlayerRideableJumping {

    /** Сила стрибка, який власник запросив пробілом (0 = стрибка нема). */
    private float jumpPending;

    public static final double WATCH_RANGE = 10.0;

    public Vovkulaka(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1, 8.0F, 3.0F));
        this.goalSelector.addGoal(4, new WatchPlayerGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTame() && !this.hasCustomName() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Vovkulaka other) {
            return !other.isTame() || other.getOwner() != owner;
        }
        return !(target instanceof Player p && p == owner);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean enchanted = stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        boolean golden = stack.is(Items.GOLDEN_APPLE);
        if (!this.isTame()) {
            if (!enchanted && !golden) {
                return InteractionResult.PASS;
            }
            if (this.level().isClientSide) {
                return InteractionResult.CONSUME;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (enchanted || this.random.nextInt(3) == 0) {
                this.tame(player);
                this.getNavigation().stop();
                this.setTarget(null);
                this.setOrderedToSit(false);
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }
        if (!this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if ((enchanted || golden || stack.is(Items.APPLE)) && this.getHealth() < this.getMaxHealth()) {
            if (this.level().isClientSide) {
                return InteractionResult.CONSUME;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            this.heal(enchanted ? 40.0F : golden ? 20.0F : 4.0F);
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty()) {
            if (this.level().isClientSide) {
                return InteractionResult.CONSUME;
            }
            if (player.isShiftKeyDown()) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.getNavigation().stop();
                this.setTarget(null);
            } else {
                this.setOrderedToSit(false);
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ===== Їзда верхи (як на коні, без сідла): рух за напрямком погляду, на блок вище вовк піднімається сам =====

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (this.isTame() && this.getFirstPassenger() instanceof Player player && this.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isAlive() && this.getControllingPassenger() instanceof Player rider) {
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;
            float strafe = rider.xxa * 0.5F;
            float forward = rider.zza;
            if (forward <= 0.0F) {
                forward *= 0.25F;
            }
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.9F);
            if (this.jumpPending > 0.0F && this.onGround()) {
                Vec3 look = Vec3.directionFromRotation(0.0F, this.getYRot());
                float power = this.jumpPending;
                this.setDeltaMovement(look.x * 0.25 * power, 0.45 + 0.35 * power, look.z * 0.25 * power);
                this.hasImpulse = true;
                this.jumpPending = 0.0F;
                this.playSound(SoundEvents.WOLF_SHAKE, 0.5F, 1.4F);
            } else if (this.onGround()) {
                this.jumpPending = 0.0F;
            }
            super.travel(new Vec3(strafe, travelVector.y, forward));
        } else {
            super.travel(travelVector);
        }
    }

    // Стрибок верхи (смуга стрибка, як у коня): методи інтерфейсу PlayerRideableJumping

    public void onPlayerJump(int jumpPower) {
        if (jumpPower < 0) {
            jumpPower = 0;
        }
        this.jumpPending = jumpPower >= 90 ? 1.0F : 0.4F + 0.6F * jumpPower / 90.0F;
    }

    public boolean canJump() {
        return this.getControllingPassenger() != null;
    }

    public void handleStartJump(int jumpPower) {
    }

    public void handleStopJump() {
    }

    public int getJumpCooldown() {
        return 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Поки на ньому сидять, власні маршрути не потрібні
        if (this.getControllingPassenger() != null) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isTame() ? SoundEvents.WOLF_AMBIENT : SoundEvents.WOLF_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    /**
     * Дикий Вовкулака стежить за гравцем у радіусі 10 блоків: стоїть і дивиться. Якщо гравець підійшов упритул,
     * кожну мить є шанс кинутися на нього.
     */
    private class WatchPlayerGoal extends Goal {

        private Player watched;

        WatchPlayerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Vovkulaka.this.isTame() || Vovkulaka.this.getTarget() != null || Vovkulaka.this.isVehicle()) {
                return false;
            }
            List<Player> players = Vovkulaka.this.level().getEntitiesOfClass(Player.class,
                    Vovkulaka.this.getBoundingBox().inflate(WATCH_RANGE),
                    p -> !p.isCreative() && !p.isSpectator() && p.isAlive()
                            && p.distanceToSqr(Vovkulaka.this) <= WATCH_RANGE * WATCH_RANGE);
            this.watched = null;
            double best = Double.MAX_VALUE;
            for (Player p : players) {
                double d = p.distanceToSqr(Vovkulaka.this);
                if (d < best) {
                    best = d;
                    this.watched = p;
                }
            }
            return this.watched != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.watched != null && this.watched.isAlive() && !Vovkulaka.this.isTame()
                    && Vovkulaka.this.getTarget() == null
                    && this.watched.distanceToSqr(Vovkulaka.this) <= (WATCH_RANGE + 2.0) * (WATCH_RANGE + 2.0);
        }

        @Override
        public void start() {
            Vovkulaka.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Vovkulaka.this.getLookControl().setLookAt(this.watched, 30.0F, 30.0F);
            Vovkulaka.this.getNavigation().stop();
            double dist = this.watched.distanceTo(Vovkulaka.this);
            // Чим ближче гравець, тим вищий шанс нападу (упритул приблизно раз на півтори секунди)
            if (dist < 4.5 && Vovkulaka.this.random.nextInt(dist < 3.0 ? 30 : 90) == 0) {
                Vovkulaka.this.setTarget(this.watched);
            }
        }

        @Override
        public void stop() {
            this.watched = null;
        }
    }
}
