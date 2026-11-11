package com.spiritform;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;

/**
 * Водяник: господар річки, один на ділянку. Рухається і по воді, і по суші (поведінка Потопельника),
 * але без тризуба. Кожен удар накладає ефект Задуха на 5 секунд.
 */
public class Vodianyk extends Drowned {

    /** Тривалість Задухи від одного удару, у тіках (5 секунд). */
    public static final int SUFFOCATION_TICKS = 100;

    private final GroundPathNavigation groundNav;
    private final WaterBoundPathNavigation waterNav;
    private final MoveControl groundMove;
    private final SmoothSwimmingMoveControl swimMove;

    public Vodianyk(EntityType<? extends Drowned> type, Level level) {
        super(type, level);
        this.groundNav = new GroundPathNavigation(this, level);
        this.waterNav = new WaterBoundPathNavigation(this, level);
        this.groundMove = new MoveControl(this);
        this.swimMove = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.navigation = this.groundNav;
        this.moveControl = this.groundMove;
    }

    /** У воді, коли ціль теж у воді або цілі нема, він пливе, як Водяниці; щоб вийти за ціллю на берег, ступає по дну. */
    private boolean swimsNow() {
        if (!this.isInWater()) {
            return false;
        }
        LivingEntity target = this.getTarget();
        return target == null || target.isInWater();
    }

    @Override
    public void updateSwimming() {
        if (!this.level().isClientSide) {
            if (this.isEffectiveAi() && this.swimsNow()) {
                this.navigation = this.waterNav;
                this.moveControl = this.swimMove;
                this.setSwimming(true);
            } else {
                this.navigation = this.groundNav;
                this.moveControl = this.groundMove;
                this.setSwimming(false);
            }
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            if (this.swimsNow()) {
                this.moveRelative(this.getSpeed(), travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
                if (this.getTarget() == null) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
                }
            } else {
                // Ціль на березі: плавно виринає й ступає на землю, без підстрибування
                super.travel(travelVector);
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.02, 0.0));
            }
        } else {
            super.travel(travelVector);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 90.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ZombieAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0, 40) {
            @Override
            public boolean canUse() {
                return Vodianyk.this.isInWater() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !Vodianyk.this.isInWater() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Господар річки нападає на гравця будь-коли, не лише вночі, як звичайний Потопельник
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, Player.class, 24.0, false, e -> true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // Без тризубів і броні
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnGroupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, new Zombie.ZombieGroupData(false, false));
        this.setBaby(false);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setPersistenceRequired();
        return result;
    }

    /** Радіус, у якому Водяник виганяє гравця з форми Привида. */
    private static final double EXPEL_RANGE = 5.0;
    /** На скільки гравець не може знову стати Привидом (10 секунд). */
    private static final long EXPEL_COOLDOWN = 200L;

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.isAlive() && this.tickCount % 5 == 0) {
            for (net.minecraft.server.level.ServerPlayer player : ((net.minecraft.server.level.ServerLevel) this.level()).players()) {
                if (SpiritFormState.isActive(player) && player.distanceToSqr(this) <= EXPEL_RANGE * EXPEL_RANGE) {
                    SpiritForm.toggle(player);
                    SpiritForm.extendCooldown(player, EXPEL_COOLDOWN);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.spirit_form.vodianyk.expel"), true);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(SpiritEffects.SUFFOCATION, SUFFOCATION_TICKS, 0), this);
        }
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
