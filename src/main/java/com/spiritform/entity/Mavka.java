package com.spiritform.entity;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Мавка: лісова дівчина-охоронниця Дерева. Не відходить від дерева далі {@link #leash} блоків і нападає на кожного,
 * хто наближається, накладаючи отруєння. Чугайстер її ворог.
 * Прихильність: якщо гравець убив Чугайстра в неї на очах, вона вважає його другом і не нападає. Другу можна
 * подарувати Троянду візера (ПКМ по Мавці або кинути поруч): вона її піднімає й дарує регенерацію на 5 хвилин.
 */
public class Mavka extends Monster {

    /** Як далеко від дерева вона блукає за замовчуванням. */
    public static final int LEASH = 12;
    /** Запас до межі, за якою вона припиняє переслідування. */
    private static final int CHASE_MARGIN = 14;
    /** Нагорода за Троянду візера. */
    private static final int REGEN_TICKS = 6000;
    private static final int REGEN_AMPLIFIER = 1;

    private BlockPos treePos;
    private int leash = LEASH;
    private final Set<UUID> friends = new HashSet<>();
    private int roseTicks;

    public Mavka(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ARMOR, 4.0);
    }

    /** Прив'язує Мавку до дерева. */
    public void setTreePos(BlockPos pos) {
        setTreePos(pos, LEASH);
    }

    public void setTreePos(BlockPos pos, int leashRadius) {
        this.treePos = pos;
        this.leash = leashRadius;
        this.restrictTo(pos, leashRadius);
    }

    public BlockPos getTreePos() {
        return treePos;
    }

    private double chaseLimit() {
        return leash + CHASE_MARGIN;
    }

    /** Свої: Мавка не чіпає лісових істот і мирних мобів. Злидня й інших ворожих мобів чіпає. */
    static boolean isForestKin(LivingEntity e) {
        return e instanceof Mavka || e instanceof Lisovyk || e instanceof Hukha || e instanceof Bisyk || e instanceof Skarbnyk
                || e.getType().getCategory().isFriendly(); // мирні тварини, кажани, водні мирні
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isForestKin(target) && super.canAttack(target);
    }

    public boolean isFriend(LivingEntity e) {
        return e instanceof Player && friends.contains(e.getUUID());
    }

    /** Мавка вважає гравця другом. */
    public void befriend(Player player) {
        if (friends.add(player.getUUID()) && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.9, getZ(), 6, 0.3, 0.3, 0.3, 0.02);
            if (getTarget() == player) {
                setTarget(null);
            }
            player.displayClientMessage(Component.translatable("message.spirit_form.mavka.friend"), true);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(2, new PickUpRoseGoal());
        this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Кожен, хто наближається до дерева (окрім лісових істот і друзів)
        this.targetSelector.addGoal(2, new ForestTargetGoal<>(this, LivingEntity.class, 16.0, true, e -> {
            if (isForestKin(e) || isFriend(e)) {
                return false;
            }
            double limit = chaseLimit();
            return treePos == null || e.blockPosition().distSqr(treePos) <= limit * limit;
        }));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Друг, що вдарив Мавку, перестає бути другом
        if (source.getEntity() instanceof Player player && friends.remove(player.getUUID())) {
            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 1.9, getZ(), 4, 0.3, 0.3, 0.3, 0.0);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1), this);
        }
        return hit;
    }

    // ===== Троянда візера =====

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.WITHER_ROSE) && isFriend(player)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                acceptRose(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** Мавка тримає троянду в руці кілька секунд і дарує другу регенерацію. */
    private void acceptRose(Player player) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WITHER_ROSE));
        this.roseTicks = 80;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, REGEN_AMPLIFIER));
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.9, getZ(), 10, 0.4, 0.4, 0.4, 0.03);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.2F);
        }
        player.displayClientMessage(Component.translatable("message.spirit_form.mavka.blessing"), true);
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    /** Мавка підходить до троянди, яку кинув її друг, піднімає її й дякує. */
    private class PickUpRoseGoal extends Goal {
        private ItemEntity rose;

        PickUpRoseGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || tickCount % 10 != 0) {
                return false;
            }
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(8.0, 3.0, 8.0),
                    i -> i.isAlive() && i.getItem().is(Items.WITHER_ROSE)
                            && i.getOwner() instanceof Player p && isFriend(p));
            if (items.isEmpty()) {
                return false;
            }
            rose = items.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return rose != null && rose.isAlive() && getTarget() == null;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(rose);
            getNavigation().moveTo(rose, 1.1);
            if (distanceToSqr(rose) < 2.5) {
                if (rose.getOwner() instanceof Player player) {
                    ItemStack stack = rose.getItem();
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        rose.discard();
                    }
                    acceptRose(player);
                }
                rose = null;
            }
        }

        @Override
        public void stop() {
            rose = null;
        }
    }

    // ===== Загальне =====

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value() == MobEffects.POISON.value()) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        if (target != null) {
            double limit = chaseLimit();
            if (isFriend(target) || (treePos != null && target.blockPosition().distSqr(treePos) > limit * limit)) {
                setTarget(null);
            }
        }
        if (roseTicks > 0 && --roseTicks == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (this.tickCount % 12 == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.2, getZ(), 2, 0.4, 0.7, 0.4, 0.0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (treePos != null) {
            tag.putInt("TreeX", treePos.getX());
            tag.putInt("TreeY", treePos.getY());
            tag.putInt("TreeZ", treePos.getZ());
            tag.putInt("Leash", leash);
        }
        ListTag list = new ListTag();
        for (UUID id : friends) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put("Friends", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TreeX")) {
            setTreePos(new BlockPos(tag.getInt("TreeX"), tag.getInt("TreeY"), tag.getInt("TreeZ")),
                    tag.contains("Leash") ? tag.getInt("Leash") : LEASH);
        }
        friends.clear();
        for (Tag t : tag.getList("Friends", Tag.TAG_INT_ARRAY)) {
            friends.add(NbtUtils.loadUUID(t));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
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
