package com.spiritform.entity;

import com.spiritform.block.GlowFernBlock;
import com.spiritform.registry.SpiritBiomes;
import com.spiritform.registry.SpiritBlocks;
import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Потерча: бліда дівчинка туманного лісу, що веде до Папороті.
 * Помітивши гравця (до 50 блоків), іде за ним і ніколи не відходить від нього далі за 10 блоків. Коли гравець підійшов
 * ближче, біжить до Папороті (радіус пошуку 50 блоків) і чекає на нього; щойно гравець опиниться за 5 блоків від Папороті,
 * зникає в білому димі. Якщо Папороті нема, просто йде за гравцем і веде його до першої, що трапиться.
 * Не залишає Туманний ліс. Якщо її вдарити, тікає й зникає назавжди.
 */
public class Potercha extends PathfinderMob {

        private static final int NOTICE = 50;
    private static final double LEASH = 10.0;
    private static final int FOLLOW = 0;
    private static final int LEAD = 1;
    private static final int FLEE = 2;

    private BlockPos home;
    private int mode = FOLLOW;
    private int fleeTicks;
    private int searchCooldown;

    public Potercha(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public void setHome(BlockPos pos) {
        this.home = pos;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GuideGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.5));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            this.mode = FLEE;
            this.fleeTicks = 100;
        }
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (mode == FLEE && --fleeTicks <= 0) {
            vanish();
            return;
        }
        // Раз на секунду: чи ще жива її Папороть
        if (this.tickCount % 20 == 0 && mode != FLEE) {
            if (home != null && this.level().isLoaded(home) && !this.level().getBlockState(home).is(SpiritBlocks.GLOW_FERN.get())) {
                home = null;
            }
            if (home != null && home.distSqr(this.blockPosition()) > (double) NOTICE * NOTICE * 1.5) {
                home = null;
            }
            if (home == null && searchCooldown-- <= 0) {
                home = GlowFernBlock.findNearest(this.level(), this.blockPosition(), NOTICE, 12);
                searchCooldown = 3;
            }
        }
    }

    /** Зникає в білому димі. */
    private void vanish() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.9, getZ(), 40, 0.4, 0.7, 0.4, 0.05);
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.9, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.ALLAY_DEATH, SoundSource.NEUTRAL, 0.8F, 1.6F);
        }
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeX")) {
            home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    private boolean inMist(BlockPos pos) {
        return this.level().getBiome(pos).is(SpiritBiomes.MIST_FOREST);
    }

    private static class GuideGoal extends Goal {

        private final Potercha mob;
        private Player player;

        GuideGoal(Potercha mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.player = this.mob.level().getNearestPlayer(this.mob, NOTICE);
            if (this.player == null || this.player.isSpectator()) {
                return false;
            }
            // Поза Туманним лісом (її чи гравця) вона не рушає з місця
            return this.mob.mode == FLEE || (this.mob.inMist(this.mob.blockPosition()) && this.mob.inMist(this.player.blockPosition()));
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            Potercha m = this.mob;
            double dist = m.distanceTo(this.player);
            m.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
            boolean recalc = m.tickCount % 10 == 0 || m.getNavigation().isDone();

            if (m.mode == FLEE) {
                if (recalc) {
                    runAway(1.6);
                }
                return;
            }
            // Поводок: далі за 10 блоків від гравця вона не відходить за жодного режиму
            if (dist > LEASH) {
                if (recalc) {
                    m.getNavigation().moveTo(this.player, dist > LEASH + 6.0 ? 1.5 : 1.2);
                }
                return;
            }
            if (m.mode == FOLLOW) {
                if (m.home != null && dist < 7.0) {
                    m.mode = LEAD; // гравець підійшов: біжить до Папороті
                } else if (m.home != null && m.tickCount % 20 == 0 && dist < 9.0) {
                    m.mode = LEAD; // Папороть знайшлася під час супроводу
                } else {
                    if (recalc) {
                        if (dist < 6.5) {
                            retreat(); // гравець наближається: відходить, лишаючись у межах 10 блоків
                        } else {
                            m.getNavigation().stop();
                        }
                    }
                    return;
                }
            }
            // LEAD
            if (m.home == null) {
                m.mode = FOLLOW; // Папороть зникла: знову просто йде за гравцем
                return;
            }
            double hx = m.home.getX() + 0.5;
            double hz = m.home.getZ() + 0.5;
            if (this.player.distanceToSqr(hx, m.home.getY(), hz) <= 25.0) {
                m.vanish();
                return;
            }
            if (recalc) {
                double toHome = m.distanceToSqr(hx, m.home.getY(), hz);
                if (toHome <= 6.0) {
                    m.getNavigation().stop();
                } else if (dist > LEASH - 1.5 && this.player.distanceToSqr(hx, m.home.getY(), hz) > toHome) {
                    // біля краю поводка: чекає, поки гравець підійде
                    m.getNavigation().stop();
                } else {
                    m.getNavigation().moveTo(hx, m.home.getY(), hz, 1.4);
                }
            }
        }

        private void retreat() {
            Vec3 pos = DefaultRandomPos.getPosAway(this.mob, 5, 3, this.player.position());
            if (pos != null && pos.distanceToSqr(this.player.position()) < LEASH * LEASH) {
                this.mob.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.3);
            }
        }

        private void runAway(double speed) {
            Vec3 pos = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.player.position());
            if (pos != null) {
                this.mob.getNavigation().moveTo(pos.x, pos.y, pos.z, speed);
            }
        }
    }
}
