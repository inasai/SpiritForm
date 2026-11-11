package com.spiritform;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Нічниця: темна сестра Мавки. Кидає в гравця шкідливі зілля, як Відьма. Якщо кинути їй Кристалізовану Душу
 * (або віддати з руки), вона піднімає її й кидає в дарувальника зілля з корисним ефектом, а потім
 * на 5 хвилин вважає його другом.
 */
public class Nichnytsia extends Monster implements RangedAttackMob {

    private static final int PEACE_TICKS = 6000;

    private UUID giftGiver;
    private int giftDelay;
    private UUID peaceWith;
    private long peaceUntil;

    public Nichnytsia(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PickUpGiftGoal());
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 45, 12.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, Player.class, 24.0, true, e -> true));
    }

    private boolean isAtPeaceWith(LivingEntity e) {
        return peaceWith != null && e.getUUID().equals(peaceWith) && this.level().getGameTime() < peaceUntil;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isAtPeaceWith(target) && super.canAttack(target);
    }

    // ===== Атака зіллям =====

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        Holder<Potion> potion = switch (this.random.nextInt(4)) {
            case 0 -> Potions.POISON;
            case 1 -> Potions.SLOWNESS;
            case 2 -> Potions.WEAKNESS;
            default -> Potions.HARMING;
        };
        throwPotion(target, PotionContents.createItemStack(Items.SPLASH_POTION, potion));
    }

    private void throwPotion(LivingEntity target, ItemStack stack) {
        Vec3 v = target.getDeltaMovement();
        double dx = target.getX() + v.x - this.getX();
        double dy = target.getEyeY() - 1.1 - this.getY();
        double dz = target.getZ() + v.z - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        ThrownPotion potion = new ThrownPotion(this.level(), this);
        potion.setItem(stack);
        potion.setXRot(potion.getXRot() + 20.0F);
        potion.shoot(dx, dy + dist * 0.2, dz, 0.75F, 8.0F);
        this.level().addFreshEntity(potion);
        this.playSound(SoundEvents.WITCH_THROW, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
    }

    /** Корисне зілля: насичення, регенерація, поглинання, стійкість, швидкість, сила чи миттєве зцілення. */
    private static ItemStack beneficialPotion(RandomSource random) {
        MobEffectInstance effect;
        int color;
        switch (random.nextInt(7)) {
            case 0 -> { effect = new MobEffectInstance(MobEffects.SATURATION, 1, 3); color = 0xF82423; }
            case 1 -> { effect = new MobEffectInstance(MobEffects.REGENERATION, 400, 1); color = 0xCD5CAB; }
            case 2 -> { effect = new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1); color = 0x2552A5; }
            case 3 -> { effect = new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1800, 0); color = 0x99453A; }
            case 4 -> { effect = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1); color = 0x7CAFC6; }
            case 5 -> { effect = new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 0); color = 0x932423; }
            default -> { effect = new MobEffectInstance(MobEffects.HEAL, 1, 1); color = 0xF82423; }
        }
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(color), List.of(effect)));
        return stack;
    }

    // ===== Дар Кристалізованої Душі =====

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(SpiritItems.CRYSTAL_SOUL.get())) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                acceptGift(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void acceptGift(Player player) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(SpiritItems.CRYSTAL_SOUL.get()));
        this.giftGiver = player.getUUID();
        this.giftDelay = 30;
        if (getTarget() == player) {
            setTarget(null);
        }
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.8, getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (giftGiver != null && --giftDelay <= 0) {
            Player giver = this.level().getPlayerByUUID(giftGiver);
            giftGiver = null;
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            if (giver != null && giver.isAlive() && giver.distanceToSqr(this) < 32.0 * 32.0) {
                throwPotion(giver, beneficialPotion(this.random));
                this.peaceWith = giver.getUUID();
                this.peaceUntil = this.level().getGameTime() + PEACE_TICKS;
                if (this.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.9, getZ(), 6, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }
        LivingEntity target = getTarget();
        if (target != null && isAtPeaceWith(target)) {
            setTarget(null);
        }
    }

    /** Підходить до Кристалізованої Душі, яку кинув гравець, піднімає її. */
    private class PickUpGiftGoal extends Goal {
        private ItemEntity item;

        PickUpGiftGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (giftGiver != null || tickCount % 10 != 0) {
                return false;
            }
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(10.0, 3.0, 10.0),
                    i -> i.isAlive() && i.getItem().is(SpiritItems.CRYSTAL_SOUL.get()) && i.getOwner() instanceof Player);
            if (items.isEmpty()) {
                return false;
            }
            item = items.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return item != null && item.isAlive() && giftGiver == null;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(item);
            getNavigation().moveTo(item, 1.2);
            if (distanceToSqr(item) < 2.5) {
                if (item.getOwner() instanceof Player player) {
                    ItemStack stack = item.getItem();
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        item.discard();
                    }
                    acceptGift(player);
                }
                item = null;
            }
        }

        @Override
        public void stop() {
            item = null;
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (peaceWith != null) {
            tag.putUUID("PeaceWith", peaceWith);
            tag.putLong("PeaceUntil", peaceUntil);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("PeaceWith")) {
            peaceWith = tag.getUUID("PeaceWith");
            peaceUntil = tag.getLong("PeaceUntil");
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }
}
