package com.spiritform;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Чорт. Біла форма (дівчина в білому): блукає полем; якщо гравець довго стоїть поруч, кидає йому квітку (не Полудницю).
 * Коли гравець відвертається й відходить на кілька блоків, вона миттю наздоганяє й б'є на 1 серце, а потім завмирає:
 * поки на неї дивляться, не реагує (зрідка кидає ще квітку). Гравця не переслідує. Чорна форма (після удару по ній):
 * нападає відкрито, і кожен удар накладає Висушування.
 */
public class Chort extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> DARK = SynchedEntityData.defineId(Chort.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROZEN = SynchedEntityData.defineId(Chort.class, EntityDataSerializers.BOOLEAN);

    private static final double CLOSE = 7.0;
    private static final int LINGER_TICKS = 100;
    /** На скільки блоків вона може погнатися за гравцем, що відвернувся; далі кидає переслідування. */
    private static final double CHASE_RANGE = 20.0;
    /** Скільки тіків триває найдовше переслідування. */
    private static final int CHASE_TICKS = 160;

    public Chort(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DARK, false);
        builder.define(FROZEN, false);
    }

    public boolean isDark() {
        return this.entityData.get(DARK);
    }

    /** Завмерла після удару (для моделі: нерухома поза). */
    public boolean isFrozenPose() {
        return this.entityData.get(FROZEN);
    }

    private void setFrozenPose(boolean value) {
        this.entityData.set(FROZEN, value);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dark", this.isDark());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DARK, tag.getBoolean("Dark"));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DarkMeleeGoal(this));
        this.goalSelector.addGoal(2, new HauntGoal());
        this.goalSelector.addGoal(3, new IdleStrollGoal(this));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Біла форма нікого не атакує. */
    @Override
    public boolean canAttack(LivingEntity target) {
        return this.isDark() && super.canAttack(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (!this.level().isClientSide && !this.isDark() && attacker instanceof Player player && amount > 0.0F) {
            this.entityData.set(DARK, true);
            this.setFrozenPose(false);
            this.getNavigation().stop();
            this.setTarget(player);
            this.playSound(SoundEvents.WITHER_SPAWN, 0.5F, 1.8F);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float damage = this.isDark() ? (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) : 2.0F;
        boolean hit = target.hurt(this.damageSources().mobAttack(this), damage);
        if (hit && this.isDark() && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0), this);
        }
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.hasCustomName() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    /** Чи дивиться гравець на цю істоту (у конусі приблизно 70 градусів від напрямку погляду). */
    private boolean seenBy(Player player) {
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 to = this.getEyePosition().subtract(player.getEyePosition()).normalize();
        return look.dot(to) > 0.35;
    }

    private Item randomFlower() {
        List<Item> flowers = BuiltInRegistries.ITEM.getTag(ItemTags.SMALL_FLOWERS)
                .map(set -> set.stream().map(h -> h.value()).filter(i -> i != SpiritItems.POLUDNYTSIA.get()).toList())
                .orElse(List.of());
        return flowers.isEmpty() ? net.minecraft.world.item.Items.DANDELION : flowers.get(this.random.nextInt(flowers.size()));
    }

    private void throwFlower(Player player) {
        ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getEyeY() - 0.3, this.getZ(), new ItemStack(randomFlower()));
        Vec3 dir = player.getEyePosition().subtract(item.position()).normalize().scale(0.3);
        item.setDeltaMovement(dir.x, dir.y + 0.15, dir.z);
        item.setPickUpDelay(10);
        this.level().addFreshEntity(item);
        this.playSound(SoundEvents.ALLAY_THROW, 1.0F, 1.4F);
    }

    /** Атака з чорної форми: звичайна ближня, лише коли чорна. */
    private static class DarkMeleeGoal extends MeleeAttackGoal {
        private final Chort chort;

        DarkMeleeGoal(Chort chort) {
            super(chort, 1.25, false);
            this.chort = chort;
        }

        @Override
        public boolean canUse() {
            return this.chort.isDark() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.chort.isDark() && super.canContinueToUse();
        }
    }

    /** Блукає, але зупиняється, поки поруч гравець. */
    private static class IdleStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final Chort chort;

        IdleStrollGoal(Chort chort) {
            super(chort, 0.55);
            this.chort = chort;
        }

        @Override
        public boolean canUse() {
            return !this.chort.isFrozenPose() && !this.chort.isDark() && super.canUse();
        }
    }

    /**
     * Поведінка білої форми. Лічильник "довго поряд": після 5 секунд кидає квітку й чекає, поки гравець відвернеться
     * й відійде. Тоді різко наздоганяє, б'є, завмирає на 3-6 секунд.
     */
    private class HauntGoal extends Goal {

        private Player player;
        private int lingered;
        private int cooldown;
        private boolean armed;
        private boolean thrown;
        private int dash;
        private Player chased;
        private int freeze;
        private double closest;

        HauntGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Chort.this.isDark()) {
                return false;
            }
            this.player = Chort.this.level().getNearestPlayer(Chort.this, CHASE_RANGE);
            if (this.player == null || this.player.isCreative() || this.player.isSpectator()) {
                this.player = null;
                return this.freeze > 0;
            }
            // Блукання не переривається, поки гравець далеко й вона нічого не затівала
            return this.armed || this.freeze > 0 || this.player.distanceTo(Chort.this) <= CLOSE + 4.0;
        }

        @Override
        public boolean canContinueToUse() {
            return !Chort.this.isDark()
                    && (this.dash > 0 || this.player != null && this.player.isAlive() || this.freeze > 0);
        }

        @Override
        public void start() {
            this.lingered = 0;
            this.armed = false;
            this.closest = Double.MAX_VALUE;
        }

        @Override
        public void stop() {
            Chort.this.setFrozenPose(false);
            this.dash = 0;
            this.chased = null;
            this.player = null;
        }

        @Override
        public void tick() {
            if (this.cooldown > 0) {
                this.cooldown--;
            }
            this.player = Chort.this.level().getNearestPlayer(Chort.this, CHASE_RANGE);
            if (this.player != null && (this.player.isCreative() || this.player.isSpectator())) {
                this.player = null;
            }

            // Переслідування за гравцем, що відвернувся: до 20 блоків; якщо він утік далі, вона кидає погоню
            if (this.dash > 0) {
                this.dash--;
                Player target = this.chased;
                if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator()
                        || target.distanceTo(Chort.this) > CHASE_RANGE) {
                    this.dash = 0;
                    this.chased = null;
                    Chort.this.getNavigation().stop();
                    this.freeze = 40;
                    this.armed = false;
                    this.thrown = false;
                    this.lingered = 0;
                    this.closest = Double.MAX_VALUE;
                    return;
                }
                Chort.this.getNavigation().moveTo(target, 1.25);
                Chort.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (Chort.this.distanceToSqr(target) < 2.6 * 2.6) {
                    Chort.this.doHurtTarget(target);
                    this.dash = 0;
                    this.chased = null;
                    this.freeze = 60 + Chort.this.random.nextInt(80);
                    Chort.this.getNavigation().stop();
                } else if (this.dash == 0) {
                    // Не наздогнала за відведений час
                    this.chased = null;
                    Chort.this.getNavigation().stop();
                    this.freeze = 40;
                    this.armed = false;
                    this.thrown = false;
                    this.lingered = 0;
                    this.closest = Double.MAX_VALUE;
                }
                return;
            }

            // Завмерла після удару: стоїть як вкопана
            if (this.freeze > 0) {
                this.freeze--;
                Chort.this.getNavigation().stop();
                Chort.this.setDeltaMovement(0.0, Chort.this.getDeltaMovement().y, 0.0);
                Chort.this.setFrozenPose(true);
                if (this.player != null) {
                    // Погляд на неї її не зачіпає; зрідка вона кидає ще квітку
                    if (this.cooldown <= 0 && Chort.this.random.nextInt(160) == 0) {
                        Chort.this.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
                        throwFlower(this.player);
                        this.cooldown = 120;
                    }
                }
                if (this.freeze == 0) {
                    Chort.this.setFrozenPose(false);
                    this.lingered = 0;
                    this.armed = false;
                    this.closest = Double.MAX_VALUE;
                }
                return;
            }

            if (this.player == null) {
                return;
            }
            double dist = this.player.distanceTo(Chort.this);

            boolean seen = seenBy(this.player);
            if (dist <= CLOSE) {
                if (seen) {
                    Chort.this.getLookControl().setLookAt(this.player, 20.0F, 20.0F);
                }
                this.lingered++;
                this.closest = Math.min(this.closest, dist);
                if (this.lingered >= LINGER_TICKS) {
                    this.armed = true;
                    if (this.cooldown <= 0 && !this.thrown) {
                        throwFlower(this.player);
                        this.cooldown = 200;
                        this.thrown = true;
                    }
                }
            }

            // Гравець відвернувся й відійшов щонайменше на 3 блоки від найближчої точки: б'є
            if (this.armed && !seen && dist > 2.5 && dist - this.closest >= 2.5) {
                this.dash = CHASE_TICKS;
                this.chased = this.player;
            }
            if (this.armed && dist > CHASE_RANGE) {
                // Гравець пішов, не відвернувшись: усе скасовується
                this.armed = false;
                this.thrown = false;
                this.lingered = 0;
                this.closest = Double.MAX_VALUE;
            }
        }
    }
}
