package com.spiritform.entity;

import com.spiritform.ability.SpiritAbility;
import com.spiritform.event.KlunnykEvents;
import com.spiritform.form.SpiritFormState;
import com.spiritform.registry.SpiritBiomes;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.RandomSource;

/**
 * Клунник: старий дід у вогняному вбранні, що живе біля лавових озер Жарокраю. Першим не нападає.
 * Поки гравець його не зачепив, береже його від вогню в радіусі 10 блоків (див. KlunnykEvents).
 * Той, хто вдарить Клунника чи заморозить йому озеро, отримує його жар. Прощає лише золоте яблуко.
 */
public class Klunnyk extends PathfinderMob {

    public static final int PROTECT_RADIUS = 10;
    private static final int FROWN_RADIUS = 12;
    private static final int FROWN_LIMIT = 60;
    private static final int GIFT_COOLDOWN = 6000;

    private final Set<UUID> angry = new HashSet<>();
    private BlockPos home;
    private int frown;
    private UUID frownAt;
    private int giftCooldown;

    public Klunnyk(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MoveTowardsRestrictionGoal(this, 0.7));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ----- гнів -----

    public boolean isAngryAt(Player player) {
        return angry.contains(player.getUUID());
    }

    public void angerAt(Player player) {
        angry.add(player.getUUID());
    }

    public void forgive(Player player) {
        angry.remove(player.getUUID());
        if (player.getUUID().equals(frownAt)) {
            frown = 0;
        }
    }

    // ----- тик -----

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || tickCount % 10 != 0) {
            return;
        }
        if (giftCooldown > 0) {
            giftCooldown -= 10;
        }
        if (home == null) {
            home = blockPosition();
            restrictTo(home, 14);
        }
        server.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 1.0, getZ(), 2, 0.35, 0.5, 0.35, 0.01);

        List<Player> players = server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(FROWN_RADIUS),
                p -> !p.isSpectator() && p.isAlive() && p.distanceToSqr(this) <= FROWN_RADIUS * FROWN_RADIUS);
        Player culprit = null;
        for (Player p : players) {
            if (p.distanceToSqr(this) <= PROTECT_RADIUS * PROTECT_RADIUS && !isAngryAt(p) && KlunnykEvents.heat(p) <= 0) {
                p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, true));
                if (p.isOnFire()) {
                    p.clearFire();
                }
            }
            if (!isAngryAt(p) && SpiritFormState.get(p).icy() && SpiritFormState.get(p).hasAbility(SpiritAbility.ICY_TOUCH)) {
                culprit = p;
            }
        }
        if (culprit != null) {
            if (frown == 0) {
                culprit.displayClientMessage(Component.translatable("message.spirit_form.klunnyk.frown"), true);
                playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.6F);
            }
            frownAt = culprit.getUUID();
            frown += 10;
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY() + 0.3, getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            if (frown >= FROWN_LIMIT) {
                frown = 0;
                angerAt(culprit);
                KlunnykEvents.punish(culprit, this);
            }
        } else if (frown > 0) {
            frown = Math.max(0, frown - 5);
        }
    }

    // ----- подарунки -----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean gold = stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        boolean fuel = stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.BLAZE_ROD);
        if (!gold && !fuel) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (isAngryAt(player)) {
            if (gold) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                forgive(player);
                KlunnykEvents.calm(player);
                player.displayClientMessage(Component.translatable("message.spirit_form.klunnyk.calm"), true);
                playSound(SoundEvents.VILLAGER_YES, 1.0F, 0.7F);
            } else {
                player.displayClientMessage(Component.translatable("message.spirit_form.klunnyk.refuse"), true);
                playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.6F);
            }
            return InteractionResult.CONSUME;
        }
        if (giftCooldown > 0) {
            player.displayClientMessage(Component.translatable("message.spirit_form.klunnyk.full"), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        giftCooldown = GIFT_COOLDOWN;
        RandomSource r = getRandom();
        ItemStack reward = switch (r.nextInt(4)) {
            case 0 -> new ItemStack(Items.BLAZE_POWDER, 2 + r.nextInt(2));
            case 1 -> new ItemStack(Items.MAGMA_CREAM, 1 + r.nextInt(2));
            case 2 -> new ItemStack(Items.GOLD_NUGGET, 3 + r.nextInt(4));
            default -> new ItemStack(Items.OBSIDIAN, 2 + r.nextInt(3));
        };
        if (gold || stack.is(Items.BLAZE_ROD)) {
            reward = new ItemStack(Items.GILDED_BLACKSTONE, 2 + r.nextInt(3));
        }
        spawnAtLocation(reward);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getEyeY(), getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }
        playSound(SoundEvents.VILLAGER_YES, 1.0F, 0.8F);
        return InteractionResult.CONSUME;
    }

    // ----- збереження -----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        for (UUID id : angry) {
            list.add(NbtUtils.createUUID(id));
        }
        tag.put("Angry", list);
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        angry.clear();
        ListTag list = tag.getList("Angry", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < list.size(); i++) {
            angry.add(NbtUtils.loadUUID(list.get(i)));
        }
        if (tag.contains("HomeX")) {
            home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            restrictTo(home, 14);
        }
    }

    // ----- місце появи -----

    /** Природна поява: Жарокрай, на землі, неподалік від лави. */
    public static boolean checkSpawn(EntityType<Klunnyk> type, ServerLevelAccessor level, MobSpawnType reason,
                                     BlockPos pos, RandomSource random) {
        return level.getBiome(pos).is(SpiritBiomes.HEATLAND)
                && net.minecraft.world.entity.Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && nearLava(level, pos);
    }

    private static boolean nearLava(LevelAccessor level, BlockPos pos) {
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                for (int dy = -3; dy <= 2; dy++) {
                    if (level.getBlockState(pos.offset(dx, dy, dz)).is(Blocks.LAVA)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
}
