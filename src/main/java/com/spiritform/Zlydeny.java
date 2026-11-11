package com.spiritform;

import java.util.EnumSet;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Злидень: виглядає точнісінько як Хуха, але стежить за гравцем, а коли той підійде ближче, завмирає на секунду й
 * нападає: вкусить, відскочить і знову вкусить. Приручити не можна. Кожен укус знімає одне сердечко (2 одиниці здоров'я) крізь броню.
 */
public class Zlydeny extends Monster {

    /** Шкода за укус: одне сердечко. */
    private static final float BITE = 2.0F;

    public Zlydeny(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, BITE)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new FleeFromHukhaGoal());
        this.goalSelector.addGoal(1, new StalkAndStrikeGoal());
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Хух і інших істот не чіпає: лише гравців, що наблизилися
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, Player.class, 20.0, true, e -> true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Магічна шкода обходить броню, тож укус завжди забирає рівно сердечко
        boolean hit = target.hurt(this.damageSources().indirectMagic(this, this), BITE);
        if (hit) {
            this.swing(InteractionHand.MAIN_HAND);
            this.playSound(SoundEvents.CAT_HISS, 1.0F, 1.2F);
        }
        return hit;
    }

    private boolean provoked;
    private int fleeTicks;
    private LivingEntity fleeFrom;

    /** Тікає від того, хто його вдарив (приручена Хуха). */
    private class FleeFromHukhaGoal extends Goal {
        FleeFromHukhaGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return fleeTicks > 0 && fleeFrom != null && fleeFrom.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (getNavigation().isDone()) {
                Vec3 away = LandRandomPos.getPosAway(Zlydeny.this, 14, 6, fleeFrom.position());
                if (away != null) {
                    getNavigation().moveTo(away.x, away.y, away.z, 1.8);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (fleeTicks > 0) {
            fleeTicks--;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof Hukha hukha) {
            fleeTicks = 120;
            fleeFrom = hukha;
            provoked = false;
            setTarget(null);
        } else if (result && source.getEntity() instanceof LivingEntity) {
            provoked = true; // якщо вдарили, одразу в атаку
        }
        return result;
    }

    /**
     * Спершу стежить: іде за гравцем і тримається на відстані, не зводячи з нього очей. Коли гравець підійшов
     * ближче ніж STALK_CLOSE, завмирає на секунду, потім нападає: кусає, відскакує й знову кусає.
     */
    private class StalkAndStrikeGoal extends Goal {
        private static final double STALK_FAR = 10.0;
        private static final double STALK_CLOSE = 5.0;
        private static final double LOSE_ATTACK = 16.0;
        private static final int FREEZE = 20;

        private int phase; // 0 стеження, 1 завмер, 2 напад
        private int freezeTicks;
        private boolean retreating;
        private int retreatTicks;
        private int cooldown;

        StalkAndStrikeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void start() {
            phase = provoked ? 2 : 0;
            retreating = false;
            cooldown = 0;
        }

        @Override
        public void stop() {
            provoked = false;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                return;
            }
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            double dist = distanceTo(t);
            if (provoked && phase != 2) {
                phase = 2;
            }
            switch (phase) {
                case 0 -> {
                    if (dist <= STALK_CLOSE) {
                        phase = 1;
                        freezeTicks = FREEZE;
                        getNavigation().stop();
                    } else if (dist > STALK_FAR) {
                        getNavigation().moveTo(t, 1.0);
                    } else {
                        getNavigation().stop();
                    }
                }
                case 1 -> {
                    getNavigation().stop();
                    if (dist > STALK_CLOSE + 3.0) {
                        phase = 0; // гравець відійшов
                    } else if (--freezeTicks <= 0) {
                        phase = 2;
                        retreating = false;
                        cooldown = 0;
                        playSound(SoundEvents.CAT_HISS, 1.0F, 0.9F);
                    }
                }
                default -> {
                    if (dist > LOSE_ATTACK && !provoked) {
                        phase = 0;
                        return;
                    }
                    attack(t);
                }
            }
        }

        private void attack(LivingEntity t) {
            if (cooldown > 0) {
                cooldown--;
            }
            if (!retreating) {
                getNavigation().moveTo(t, 1.6);
                double reach = getBbWidth() * 2.0F * getBbWidth() * 2.0F + t.getBbWidth();
                if (distanceToSqr(t) <= reach && cooldown <= 0) {
                    doHurtTarget(t);
                    retreating = true;
                    retreatTicks = 30 + random.nextInt(25);
                    cooldown = 20;
                    getNavigation().stop();
                }
            } else {
                retreatTicks--;
                if (getNavigation().isDone()) {
                    Vec3 away = LandRandomPos.getPosAway(Zlydeny.this, 10, 5, t.position());
                    if (away != null) {
                        getNavigation().moveTo(away.x, away.y, away.z, 1.7);
                    }
                }
                if (retreatTicks <= 0) {
                    retreating = false;
                }
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_STRAY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }
}
