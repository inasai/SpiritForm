package com.spiritform;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Спільна основа всіх Вісників Потойбіччя: смуга боса, власник (гравець, для якого Вісник з'явився),
 * збереження, нагорода за перемогу. Увесь життєвий цикл (поява, перемога, поразка) веде SpiritHeralds,
 * а ефекти середовища SpiritHeraldWorld.
 *
 * Вісників не можна одержати: див. SpiritPossession.isSupported і SpiritTouch.
 */
public abstract class HeraldBase extends Monster {

    private final HeraldType kind;
    private final ServerBossEvent bossEvent;
    private UUID owner;

    protected HeraldBase(EntityType<? extends HeraldBase> type, Level level, HeraldType kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = kind == HeraldType.DEATH ? 200 : 80;
        this.bossEvent = new ServerBossEvent(kind.displayName(), kind.color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setDarkenScreen(false);
        this.setPersistenceRequired();
        if (!level.isClientSide) {
            double multiplier = Config.HERALD_HEALTH_MULTIPLIER.get();
            if (multiplier != 1.0 && getAttribute(Attributes.MAX_HEALTH) != null) {
                getAttribute(Attributes.MAX_HEALTH).setBaseValue(getAttribute(Attributes.MAX_HEALTH).getBaseValue() * multiplier);
                setHealth(getMaxHealth());
            }
        }
    }

    public HeraldType kind() {
        return kind;
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /** Гравець-власник, якщо він зараз у грі. */
    public ServerPlayer ownerPlayer() {
        if (owner == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(owner);
    }

    /** Ефекти конкретного Вісника на сервері, щотіку. */
    protected abstract void tickHerald(ServerLevel level);

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel && isAlive()) {
            bossEvent.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth())));
            tickHerald(serverLevel);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(name == null ? kind.displayName() : name);
    }

    @Override
    public void remove(RemovalReason reason) {
        bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    /** Смерть Вісника: перемога гравця. Нагороду випускає dropCustomDeathLoot, облік веде SpiritHeralds. */
    @Override
    public void die(DamageSource source) {
        boolean already = isDeadOrDying();
        super.die(source);
        if (!already && level() instanceof ServerLevel serverLevel) {
            SpiritHeralds.onHeraldDied(serverLevel, this, source);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // Серце гарантоване. Вісник Смерті залишає Серце Смерті, яке можна поглинути (див. DeathHeartItem)
        spawnAtLocation(new ItemStack(kind.heart()));
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) {
            tag.putUUID("HeraldOwner", owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("HeraldOwner") ? tag.getUUID("HeraldOwner") : null;
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    // Звуки: ванільні, підібрані за характером кожного Вісника
    protected SoundEvent ambient() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    protected SoundEvent hurtSound() {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    protected SoundEvent deathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ambient();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return deathSound();
    }
}
