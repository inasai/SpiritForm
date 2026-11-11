package com.spiritform;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Польовик: господар Рівнин Потойбіччя. Парить над землею біля центру біому й першим не нападає, а якщо його вдарити,
 * стає дуже швидким і сильним (шкодить навіть Привиду). Будь-яка подарована квітка його вгамовує, а Полудниця
 * приносить нагороду, що з кожною наступною квіткою мізерніє: зачароване золоте яблуко, 2 золотих, 2 звичайних, далі нічого.
 */
public class Polovyk extends PathfinderMob {


    private final Map<UUID, Integer> gifts = new HashMap<>();
    /** Гравці, яких він заспокоєно не чіпає, доки ті не вдарять його знову. */
    private final java.util.Set<UUID> calmed = new java.util.HashSet<>();
    private BlockPos home;

    public Polovyk(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FLYING_SPEED, 1.2)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public void setHome(BlockPos pos) {
        this.home = pos;
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
        this.goalSelector.addGoal(2, new HoverNearHomeGoal());
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        // Нападає лише на того, хто вдарив (інакше мирний)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Заспокоєного гравця він не обирає ціллю. */
    @Override
    public void setTarget(@javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target) {
        if (target instanceof Player player && this.calmed.contains(player.getUUID())) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return !(target instanceof Player player && this.calmed.contains(player.getUUID())) && super.canAttack(target);
    }

    /** Удар гравця знову розлючує Польовика. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof Player player && amount > 0.0F) {
            this.calmed.remove(player.getUUID());
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ItemTags.FLOWERS)) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.CONSUME;
        }
        boolean sun = stack.is(SpiritItems.POLUDNYTSIA.get());
        // Розлюченому Польовику Полудницю не віддати: спершу його треба вгамувати іншою квіткою
        if (sun && (this.getTarget() == player || this.isAggressive() && !this.calmed.contains(player.getUUID()))) {
            return InteractionResult.PASS;
        }
        int given = this.gifts.getOrDefault(player.getUUID(), 0);
        // Після п'ятої Полудниці він більше нічого не дає й квітку не бере
        if (sun && given >= 5) {
            return InteractionResult.PASS;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        calm(player);
        if (sun) {
            this.gifts.put(player.getUUID(), given + 1);
            ItemStack reward = switch (given) {
                case 0 -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
                case 1, 2 -> new ItemStack(Items.GOLDEN_APPLE);
                default -> new ItemStack(Items.APPLE);
            };
            toss(player, reward);
        }
        return InteractionResult.SUCCESS;
    }

    /** Квітка вгамовує Польовика: він перестає нападати на гравця. */
    private void calm(Player player) {
        this.calmed.add(player.getUUID());
        if (this.getTarget() == player) {
            this.setTarget(null);
        }
        this.setAggressive(false);
        this.setLastHurtByMob(null);
        this.getNavigation().stop();
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(0.7), this.getZ(), 8, 0.4, 0.5, 0.4, 0.0);
        }
        this.playSound(SoundEvents.ALLAY_ITEM_GIVEN, 1.0F, 1.0F);
    }

    private void toss(Player player, ItemStack reward) {
        ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY(0.6), this.getZ(), reward);
        Vec3 dir = player.position().add(0.0, 0.5, 0.0).subtract(item.position()).normalize().scale(0.25);
        item.setDeltaMovement(dir.x, dir.y + 0.1, dir.z);
        item.setPickUpDelay(10);
        this.level().addFreshEntity(item);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> e : this.gifts.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Player", e.getKey());
            t.putInt("Count", e.getValue());
            list.add(t);
        }
        tag.put("Gifts", list);
        ListTag calm = new ListTag();
        for (UUID id : this.calmed) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Player", id);
            calm.add(t);
        }
        tag.put("Calmed", calm);
        if (this.home != null) {
            tag.putInt("HomeX", this.home.getX());
            tag.putInt("HomeY", this.home.getY());
            tag.putInt("HomeZ", this.home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.gifts.clear();
        for (Tag t : tag.getList("Gifts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            this.gifts.put(c.getUUID("Player"), c.getInt("Count"));
        }
        this.calmed.clear();
        for (Tag t : tag.getList("Calmed", Tag.TAG_COMPOUND)) {
            this.calmed.add(((CompoundTag) t).getUUID("Player"));
        }
        if (tag.contains("HomeX")) {
            this.home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
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

    /** У спокої повільно ширяє біля свого місця, на висоті 1,5-3 блоки над землею. */
    private class HoverNearHomeGoal extends Goal {

        private int wait;

        HoverNearHomeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Polovyk.this.getTarget() == null;
        }

        @Override
        public void tick() {
            if (--this.wait > 0) {
                return;
            }
            this.wait = 40 + Polovyk.this.random.nextInt(60);
            BlockPos base = Polovyk.this.home != null ? Polovyk.this.home : Polovyk.this.blockPosition();
            double x = base.getX() + 0.5 + (Polovyk.this.random.nextDouble() - 0.5) * 8.0;
            double z = base.getZ() + 0.5 + (Polovyk.this.random.nextDouble() - 0.5) * 8.0;
            int ground = Polovyk.this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            double y = ground + 1.5 + Polovyk.this.random.nextDouble() * 1.5;
            Polovyk.this.getMoveControl().setWantedPosition(x, y, z, 0.3);
        }
    }

}
