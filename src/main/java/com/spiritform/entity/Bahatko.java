package com.spiritform.entity;

import com.spiritform.SpiritFormMod;
import com.spiritform.registry.SpiritBiomes;
import com.spiritform.registry.SpiritCreatures;
import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Багатько: маленька вогняна кулька, що літає над Жарокраєм і захищає його від чужинців.
 * Нападає на всіх, хто тут не живе (тег heatland_dwellers, вогнетривкі істоти, Армія Привида не рахуються).
 * Б'є й підпалює, тікає, а потім вертається; поранений кличе родичів. Вода гасить його: тоді лишається жаринка.
 * Може злетіти від вогню печі, коптильні, домни чи варильної стійки (див. BahatkoEvents).
 */
public class Bahatko extends PathfinderMob {

    public static final TagKey<EntityType<?>> DWELLERS = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "heatland_dwellers"));
    private static final String ARMY_OWNER_KEY = "spirit_form_army_owner";

    private int emerge;
    private boolean fromCraft;
    private UUID lureTarget;
    private boolean quenched;
    private int attackCooldown;
    private int retreat;

    public Bahatko(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
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
        this.goalSelector.addGoal(1, new AttackGoal());
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> isOutsider(e)));
    }

    /** Чужинець: усе, що не з Жарокраю (мешканці, вогнетривкі істоти й Армія Привида не рахуються). */
    public static boolean isOutsider(LivingEntity e) {
        if (!e.isAlive() || e.getType().is(DWELLERS) || e.getType().fireImmune()
                || e.getPersistentData().contains(ARMY_OWNER_KEY)) {
            return false;
        }
        return !(e instanceof Player p) || !p.isSpectator();
    }

    /** Запускає появу від вогню станції: кілька секунд висить непорушно, потім нападає на гравця. */
    public void beginLure(Player player) {
        this.fromCraft = true;
        this.emerge = 50;
        this.lureTarget = player.getUUID();
        this.setPersistenceRequired();
    }

    public boolean isFromCraft() {
        return fromCraft;
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (emerge > 0 && target != null) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (emerge > 0) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.FLAME, getRandomX(0.5), getY() + 0.3 + random.nextDouble() * 0.3,
                        getRandomZ(0.5), 0.0, 0.02, 0.0);
            }
            return;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (emerge > 0) {
            emerge--;
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().scale(0.5));
            if (level() instanceof ServerLevel server) {
                if (tickCount % 3 == 0) {
                    server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 1, 0.2, 0.2, 0.2, 0.0);
                    server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 2, 0.2, 0.2, 0.2, 0.02);
                }
                if (emerge == 0 && lureTarget != null) {
                    Player p = server.getPlayerByUUID(lureTarget);
                    if (p != null && p.isAlive()) {
                        setTarget(p);
                    }
                }
            }
            return;
        }
        if (isInWaterOrBubble()) {
            quenched = true;
            if (tickCount % 10 == 0) {
                hurt(damageSources().drown(), 1.0F);
            }
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    // ----- напад: влучив, відлетів, повернувся -----

    private class AttackGoal extends Goal {
        AttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return emerge <= 0 && t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                return;
            }
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            if (retreat > 0) {
                retreat--;
                double dx = getX() - t.getX();
                double dz = getZ() - t.getZ();
                double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                getMoveControl().setWantedPosition(getX() + dx / len * 3.0, t.getY() + 2.5, getZ() + dz / len * 3.0, 1.0);
                return;
            }
            getMoveControl().setWantedPosition(t.getX(), t.getEyeY() - 0.3, t.getZ(), 1.2);
            if (attackCooldown <= 0 && getBoundingBox().inflate(0.3).intersects(t.getBoundingBox())) {
                if (doHurtTarget(t)) {
                    t.igniteForSeconds(4.0F);
                    playSound(SoundEvents.BLAZE_SHOOT, 0.6F, 1.6F);
                }
                attackCooldown = 40;
                retreat = 12;
            }
        }
    }

    // ----- здобич -----

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        RandomSource r = getRandom();
        int n = (quenched || source.is(DamageTypes.DROWN)) ? 1 + r.nextInt(2) : (r.nextFloat() < 0.25F ? 1 : 0);
        if (n > 0) {
            spawnAtLocation(new ItemStack(SpiritCreatures.BAHATKO_EMBER.get(), n));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Emerge", emerge);
        tag.putBoolean("FromCraft", fromCraft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerge = tag.getInt("Emerge");
        fromCraft = tag.getBoolean("FromCraft");
        if (fromCraft) {
            setPersistenceRequired();
        }
    }

    /** Природна поява: Жарокрай, у повітрі. */
    public static boolean checkSpawn(EntityType<Bahatko> type, ServerLevelAccessor level, MobSpawnType reason,
                                     BlockPos pos, RandomSource random) {
        return level.getBiome(pos).is(SpiritBiomes.HEATLAND) && level.getBlockState(pos).isAir();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !fromCraft;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FIRE_EXTINGUISH;
    }
}
