package com.spiritform.herald;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Вісник Бурі. Летить, навколо нього постійний ураган (SpiritHeraldWorld.tickStorm): відкидає гравців,
 * відхиляє снаряди, а стріли й інші снаряди йому шкоди не завдають. Б'є блискавкою (з попередженням іскрами)
 * і в ближньому бою. До нього можна дістатися «Кроком Привида»: після нього є коротке вікно без вітру.
 */
public class StormHerald extends HeraldBase {

    /** Блискавка, що чекає: x, y, z і скільки тіків лишилося до удару. */
    private static final class Strike {
        final double x;
        final double z;
        int ticks;

        Strike(double x, double z, int ticks) {
            this.x = x;
            this.z = z;
            this.ticks = ticks;
        }
    }

    private final List<Strike> strikes = new ArrayList<>();

    public StormHerald(EntityType<? extends StormHerald> type, Level level) {
        super(type, level, HeraldType.STORM);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StormGoal());
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Снаряди відбиваються вітром, а блискавка йому не шкодить. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.5, getZ(), 8, 0.5, 0.5, 0.5, 0.05);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.LIGHTNING_BOLT) || super.isInvulnerableTo(source);
    }

    @Override
    protected void tickHerald(ServerLevel level) {
        if (tickCount % 100 == 1) {
            SpiritHeraldWorld.keepWeather(level, true);
        }
        SpiritHeraldWorld.tickStorm(this, level);

        Iterator<Strike> iterator = strikes.iterator();
        while (iterator.hasNext()) {
            Strike strike = iterator.next();
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(strike.x), (int) Math.floor(strike.z));
            strike.ticks--;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strike.x, ground + 0.3, strike.z, 6, 1.0, 0.1, 1.0, 0.05);
            if (strike.ticks <= 0) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(strike.x, ground, strike.z);
                    level.addFreshEntity(bolt);
                }
                iterator.remove();
            }
        }
    }

    private void queueStrike(Vec3 at) {
        strikes.add(new Strike(at.x, at.z, 25));
    }

    /** Кружляє навколо цілі на висоті, час від часу кличе блискавку, а впритул б'є в ближньому бою. */
    private final class StormGoal extends Goal {
        private int strikeTimer = 50;
        private int meleeTimer;

        StormGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            setAggressive(false);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            meleeTimer--;

            boolean close = getBoundingBox().inflate(1.5).intersects(target.getBoundingBox());
            if (close) {
                setAggressive(true);
                if (meleeTimer <= 0) {
                    doHurtTarget(target);
                    meleeTimer = 20;
                }
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.0, target.getZ(), 1.0);
            } else {
                setAggressive(false);
                double angle = tickCount * 0.04;
                getMoveControl().setWantedPosition(target.getX() + Math.cos(angle) * 11.0, target.getY() + 5.0,
                        target.getZ() + Math.sin(angle) * 11.0, 1.0);
            }

            if (--strikeTimer <= 0) {
                queueStrike(target.position());
                strikeTimer = 60 + getRandom().nextInt(40);
            }
        }
    }

    @Override
    protected SoundEvent ambient() {
        return SoundEvents.BREEZE_IDLE_AIR;
    }

    @Override
    protected SoundEvent hurtSound() {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent deathSound() {
        return SoundEvents.BREEZE_DEATH;
    }
}
