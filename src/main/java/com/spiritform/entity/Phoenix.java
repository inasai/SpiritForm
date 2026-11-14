package com.spiritform.entity;

import com.spiritform.registry.SpiritCreatures;
import com.spiritform.registry.SpiritItems;
import com.spiritform.worldgen.HeatlandSites;
import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Жар-птиця: гігантський хижий птах кольору сонця. Дика кружляє над кратером вулкана, уночі спить на гнізді,
 * а вдень обстрілює гравців вогняними кулями. Дику не приручити: лише ту, що відродилася з Попелу
 * (див. PhoenixAshBlock), Яблуком Духів із шансом 1/3. Приручена потребує «розпеченої» їжі: без неї
 * відлітає до вулкана. Допомагає в бою, несе скриню й возить власника верхи (політ палить вершника, як магмовий блок,
 * рятує Крижаний Доторк). Вбивство дикої Жар-птиці кличе Гнів Перуна (див. PerunWrath).
 */
public class Phoenix extends TamableAnimal implements FlyingAnimal {

    private static final EntityDataAccessor<Boolean> HAS_CHEST =
            SynchedEntityData.defineId(Phoenix.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RESTING =
            SynchedEntityData.defineId(Phoenix.class, EntityDataSerializers.BOOLEAN);

    private static final double RIDE_SPEED = 0.5;
    private static final int MAX_FED = 48000;
    private static final int START_FED = 24000;
    private static final int HUNGRY_WARNING = 3000;

    private BlockPos circleCenter;
    private BlockPos nestPos;
    private float circleRadius = 30.0F;
    private float circleHeight = 22.0F;
    private boolean reborn;
    private int fed = START_FED;
    private boolean warned;
    private int sleepBlocked;
    private int fireCooldown;

    private final SimpleContainer container = new SimpleContainer(27) {
        @Override
        public boolean stillValid(Player player) {
            return Phoenix.this.isAlive() && player.distanceToSqr(Phoenix.this) < 100.0;
        }
    };

    public Phoenix(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, false);
        this.setNoGravity(true);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_CHEST, false);
        builder.define(RESTING, false);
    }

    public boolean hasChest() {
        return this.entityData.get(HAS_CHEST);
    }

    public boolean isResting() {
        return this.entityData.get(RESTING);
    }

    private void setResting(boolean value) {
        this.entityData.set(RESTING, value);
    }

    public boolean isReborn() {
        return reborn;
    }

    public boolean isHungry() {
        return isTame() && fed <= 0;
    }

    /** Відроджена з Попелу: спокійна, висить над місцем відродження й чекає на приручення. */
    public void markReborn(BlockPos hover) {
        this.reborn = true;
        this.circleCenter = hover;
        this.circleRadius = 7.0F;
        this.circleHeight = 6.0F;
        this.setPersistenceRequired();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FireGoal());
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 12.0F, 4.0F) {
            @Override
            public boolean canUse() {
                return !isHungry() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new NestGoal());
        this.goalSelector.addGoal(4, new CircleGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> !this.isTame() && !this.reborn));
    }

    @Override
    public boolean isFlying() {
        return true;
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
        return !this.isTame() && !this.reborn && !this.hasCustomName();
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Phoenix other) {
            return !other.isTame() || other.getOwner() != owner;
        }
        return !(target instanceof Player p && p == owner);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Сплячу птицю не будить здалеку: лише той, хто підійшов упритул.
        if (isResting() && target != null && distanceToSqr(target) > 64.0) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isResting()) {
            setResting(false);
            sleepBlocked = 2400;
        }
        return super.hurt(source, amount);
    }

    // ----- годування -----

    private static int foodValue(ItemStack stack) {
        if (stack.is(SpiritCreatures.BAHATKO_EMBER.get())) {
            return 36000;
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return 24000;
        }
        if (stack.is(Items.MAGMA_CREAM)) {
            return 12000;
        }
        if (stack.is(Items.BLAZE_POWDER)) {
            return 6000;
        }
        return 0;
    }

    // ----- приручення, догляд, скриня, їзда -----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.isTame()) {
            if (!stack.is(SpiritItems.SPIRIT_APPLE.get())) {
                return InteractionResult.PASS;
            }
            if (this.level().isClientSide) {
                return InteractionResult.CONSUME;
            }
            if (!this.reborn) {
                player.displayClientMessage(Component.translatable("message.spirit_form.phoenix.untameable"), true);
                return InteractionResult.CONSUME;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (this.random.nextInt(3) == 0) {
                this.tame(player);
                this.fed = START_FED;
                this.getNavigation().stop();
                this.setTarget(null);
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }
        if (!this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        int food = foodValue(stack);
        if (food > 0 && (fed < MAX_FED || getHealth() < getMaxHealth())) {
            if (this.level().isClientSide) {
                return InteractionResult.CONSUME;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            fed = Math.min(MAX_FED, Math.max(fed, 0) + food);
            warned = false;
            heal(food >= 24000 ? 20.0F : 10.0F);
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.5, getZ(), 14, 0.9, 0.7, 0.9, 0.02);
            playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.7F);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.CHEST) && !hasChest()) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.entityData.set(HAS_CHEST, true);
                playSound(SoundEvents.DONKEY_CHEST, 1.0F, 0.8F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (stack.isEmpty() && player.isSecondaryUseActive() && hasChest()) {
            if (!this.level().isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> ChestMenu.threeRows(id, inventory, container), getDisplayName()));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (stack.isEmpty() && !player.isSecondaryUseActive() && !this.isVehicle()) {
            if (!this.level().isClientSide) {
                if (isHungry()) {
                    player.displayClientMessage(Component.translatable("message.spirit_form.phoenix.hungry"), true);
                    return InteractionResult.CONSUME;
                }
                setResting(false);
                player.startRiding(this);
                player.displayClientMessage(Component.translatable("message.spirit_form.phoenix.ride_hot"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p && isOwnedBy(p) ? p : null;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player p && !level().isClientSide) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, true, false, true));
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (getControllingPassenger() instanceof Player player && isVehicle()) {
            setYRot(player.getYRot());
            yRotO = getYRot();
            setXRot(player.getXRot() * 0.5F);
            setRot(getYRot(), getXRot());
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            if (isControlledByLocalInstance()) {
                Vec3 look = player.getLookAngle();
                Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
                Vec3 want = look.scale(player.zza).add(right.scale(-player.xxa));
                Vec3 motion = getDeltaMovement().scale(0.85).add(want.scale(RIDE_SPEED * 0.18));
                setDeltaMovement(motion);
                move(MoverType.SELF, getDeltaMovement());
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
            calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    // ----- тик -----

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (!isResting() && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.FLAME, getRandomX(1.6), getY() + random.nextDouble() * 1.6,
                        getRandomZ(1.6), 0.0, -0.02, 0.0);
            }
            return;
        }
        if (fireCooldown > 0) {
            fireCooldown--;
        }
        if (sleepBlocked > 0) {
            sleepBlocked--;
        }
        if (circleCenter == null && level() instanceof ServerLevel server) {
            HeatlandSites.Volcano v = HeatlandSites.nearest(server, getX(), getZ(), 1);
            if (v != null) {
                circleCenter = HeatlandSites.summit(v);
                nestPos = HeatlandSites.nestPos(v);
            }
        }
        // Палить вершника, як магмовий блок (Крижаний Доторк або вогнестійкість рятують).
        if (tickCount % 10 == 0 && getControllingPassenger() instanceof Player rider && !rider.isCreative()) {
            rider.hurt(rider.damageSources().hotFloor(), 1.0F);
        }
        if (isTame()) {
            if (fed > 0) {
                fed--;
            }
            if (fed <= HUNGRY_WARNING && !warned && getOwner() instanceof Player owner && distanceToSqr(owner) < 48.0 * 48.0) {
                warned = true;
                owner.displayClientMessage(Component.translatable("message.spirit_form.phoenix.hungry_soon"), true);
            }
            // Линяє: інколи лишає перо.
            if (tickCount % 6000 == 0 && random.nextInt(3) == 0) {
                spawnAtLocation(new ItemStack(SpiritCreatures.PHOENIX_FEATHER.get()));
            }
        }
    }

    // ----- нападники, гніздо, коло над кратером -----

    private class FireGoal extends Goal {
        FireGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && !isVehicle();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            setResting(false);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                return;
            }
            double ang = tickCount * 0.04;
            getMoveControl().setWantedPosition(t.getX() + Math.cos(ang) * 14.0, t.getY() + 9.0,
                    t.getZ() + Math.sin(ang) * 14.0, 1.0);
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            if (fireCooldown <= 0 && hasLineOfSight(t) && distanceToSqr(t) < 45.0 * 45.0) {
                Vec3 from = new Vec3(getX(), getY() + 1.5, getZ());
                Vec3 dir = t.getEyePosition().subtract(from).normalize();
                SmallFireball fireball = new SmallFireball(level(), Phoenix.this, dir);
                fireball.setPos(from.x, from.y, from.z);
                level().addFreshEntity(fireball);
                playSound(SoundEvents.BLAZE_SHOOT, 1.5F, 0.8F);
                fireCooldown = isTame() ? 25 : 35;
            }
        }
    }

    /** Уночі дика Жар-птиця летить до гнізда на краю кратера й засинає. */
    private class NestGoal extends Goal {
        NestGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !isTame() && !reborn && nestPos != null && !level().isDay() && sleepBlocked <= 0
                    && getTarget() == null && !isVehicle();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            double tx = nestPos.getX() + 0.5;
            double ty = nestPos.getY() + 1.2;
            double tz = nestPos.getZ() + 0.5;
            if (distanceToSqr(tx, ty, tz) < 2.5) {
                getNavigation().stop();
                setDeltaMovement(Vec3.ZERO);
                if (!isResting()) {
                    setResting(true);
                }
            } else {
                setResting(false);
                getMoveControl().setWantedPosition(tx, ty, tz, 1.0);
            }
        }

        @Override
        public void stop() {
            setResting(false);
        }
    }

    private class CircleGoal extends Goal {
        CircleGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return circleCenter != null && getTarget() == null && !isVehicle() && (!isTame() || isHungry());
        }

        @Override
        public void tick() {
            double dx = getX() - circleCenter.getX();
            double dz = getZ() - circleCenter.getZ();
            double next = Math.atan2(dz, dx) + 0.3;
            double y = circleCenter.getY() + circleHeight + Math.sin(tickCount * 0.02) * 3.0;
            getMoveControl().setWantedPosition(circleCenter.getX() + Math.cos(next) * circleRadius, y,
                    circleCenter.getZ() + Math.sin(next) * circleRadius, 0.9);
        }
    }

    // ----- здобич й збереження -----

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(SpiritCreatures.PHOENIX_FEATHER.get()));
        if (hasChest()) {
            spawnAtLocation(new ItemStack(Items.CHEST));
            Containers.dropContents(level, blockPosition(), container);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (circleCenter != null) {
            tag.putInt("CX", circleCenter.getX());
            tag.putInt("CY", circleCenter.getY());
            tag.putInt("CZ", circleCenter.getZ());
        }
        if (nestPos != null) {
            tag.putInt("NX", nestPos.getX());
            tag.putInt("NY", nestPos.getY());
            tag.putInt("NZ", nestPos.getZ());
        }
        tag.putFloat("CircleRadius", circleRadius);
        tag.putFloat("CircleHeight", circleHeight);
        tag.putBoolean("Reborn", reborn);
        tag.putInt("Fed", fed);
        tag.putBoolean("HasChest", hasChest());
        tag.put("Items", container.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CX")) {
            circleCenter = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        }
        if (tag.contains("NX")) {
            nestPos = new BlockPos(tag.getInt("NX"), tag.getInt("NY"), tag.getInt("NZ"));
        }
        if (tag.contains("CircleRadius")) {
            circleRadius = tag.getFloat("CircleRadius");
            circleHeight = tag.getFloat("CircleHeight");
        }
        reborn = tag.getBoolean("Reborn");
        if (reborn) {
            setPersistenceRequired();
        }
        if (tag.contains("Fed")) {
            fed = tag.getInt("Fed");
        }
        this.entityData.set(HAS_CHEST, tag.getBoolean("HasChest"));
        if (tag.contains("Items", Tag.TAG_LIST)) {
            container.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), registryAccess());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return Mth.clamp(super.getVoicePitch() * 0.5F, 0.4F, 1.0F);
    }
}
