package com.spiritform;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Скарбник: сірий гоблін-злодій. Підбирає речі, що випали з гравця, і тікає з ними. Якщо його вбити, річ випаде.
 * Тікає від гравців і без здобичі.
 */
public class Skarbnyk extends PathfinderMob {

    public Skarbnyk(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public boolean isCarrying() {
        return !getMainHandItem().isEmpty();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeWithLootGoal());
        this.goalSelector.addGoal(2, new StealItemGoal());
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.2, 1.5));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !isCarrying() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    /** Річ, що випала з гравця: кинута ним або лежить поруч із ним. */
    private boolean isPlayerDrop(ItemEntity item) {
        if (!item.isAlive() || item.getItem().isEmpty()) {
            return false;
        }
        if (item.getOwner() instanceof Player) {
            return true;
        }
        return item.getOwner() == null && level().getNearestPlayer(item, 5.0) != null;
    }

    private class StealItemGoal extends Goal {
        private ItemEntity target;

        StealItemGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isCarrying() || tickCount % 10 != 0) {
                return false;
            }
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class,
                    getBoundingBox().inflate(16.0, 6.0, 16.0), Skarbnyk.this::isPlayerDrop);
            ItemEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (ItemEntity i : items) {
                double d = distanceToSqr(i);
                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
            target = best;
            return best != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !isCarrying();
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(target);
            getNavigation().moveTo(target, 1.3);
            if (distanceToSqr(target) < 2.2) {
                ItemStack stack = target.getItem().copy();
                target.discard();
                setItemSlot(EquipmentSlot.MAINHAND, stack);
                setDropChance(EquipmentSlot.MAINHAND, 2.0F);
                setPersistenceRequired();
                playSound(SoundEvents.PILLAGER_CELEBRATE, 1.0F, 1.5F);
                target = null;
            }
        }

        @Override
        public void stop() {
            target = null;
        }
    }

    private class FleeWithLootGoal extends Goal {
        FleeWithLootGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isCarrying();
        }

        @Override
        public boolean canContinueToUse() {
            return isCarrying();
        }

        @Override
        public void tick() {
            if (!getNavigation().isDone() && tickCount % 20 != 0) {
                return;
            }
            Player nearest = level().getNearestPlayer(Skarbnyk.this, 40.0);
            Vec3 away = nearest != null
                    ? LandRandomPos.getPosAway(Skarbnyk.this, 20, 8, nearest.position())
                    : LandRandomPos.getPos(Skarbnyk.this, 12, 5);
            if (away != null) {
                getNavigation().moveTo(away.x, away.y, away.z, 1.5);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }
}
