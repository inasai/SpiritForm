package com.spiritform;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Бісик: крихітна літаюча істота, схожа на світлячка. Б'є гравця майже без шкоди, але може вибити з його
 * інвентаря випадковий предмет (не одягнений і не той, що в руці).
 */
public class Bisyk extends Monster {

    /** Шанс, що влучний удар вибиває предмет. */
    private static final float DROP_CHANCE = 0.5F;

    public Bisyk(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 16.0);
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
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, Player.class, 12.0, true, e -> true));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(16) == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, getRandomX(0.4), getY() + 0.2, getRandomZ(0.4),
                    0.0, -0.01, 0.0);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer player && this.random.nextFloat() < DROP_CHANCE) {
            stealItem(player);
        }
        return hit;
    }

    /** Вибиває з інвентаря один випадковий стек, окрім обраного слота руки (броня й друга рука поза списком). */
    private void stealItem(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.items.size(); i++) {
            if (i != inv.selected && !inv.items.get(i).isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        int slot = slots.get(this.random.nextInt(slots.size()));
        ItemStack stack = inv.items.get(slot);
        inv.items.set(slot, ItemStack.EMPTY);

        ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(), stack);
        drop.setPickUpDelay(50);
        drop.setThrower(player);
        drop.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.3, 0.2, (this.random.nextDouble() - 0.5) * 0.3);
        player.level().addFreshEntity(drop);
        player.containerMenu.broadcastChanges();

        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.2, player.getZ(),
                    10, 0.3, 0.4, 0.3, 0.02);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.HOSTILE, 1.0F, 1.6F);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_RESONATE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }
}
