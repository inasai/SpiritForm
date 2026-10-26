package com.spiritform;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * М'ятна душа: дим на місці смерті істоти. Живе обмежений час (конфіг). ПКМ власника по диму
 * переносить душу до сховища армії, якщо там є місце.
 */
public class SoulWispEntity extends Entity {

    private UUID owner;
    private CompoundTag soul = new CompoundTag();
    private int life = 400;

    public SoulWispEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    public void setSoul(UUID owner, CompoundTag soul, int lifeTicks) {
        this.owner = owner;
        this.soul = soul;
        this.life = lifeTicks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (--life <= 0) {
            SpiritArmy.puff(server, this, 8);
            discard();
            return;
        }
        server.sendParticles(SpiritParticles.MINT, getX(), getY() + 0.8, getZ(), 3, 0.25, 0.5, 0.25, 0.0);
        if (tickCount % 4 == 0) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, getX(), getY() + 0.5, getZ(),
                    1, 0.25, 0.5, 0.25, 0.01);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (owner != null && !owner.equals(player.getUUID())) {
            serverPlayer.displayClientMessage(Component.translatable("message.spirit_form.army.not_yours"), true);
            return InteractionResult.SUCCESS;
        }
        if (SpiritArmy.capture(serverPlayer, soul)) {
            if (level() instanceof ServerLevel server) {
                SpiritArmy.puff(server, this, 16);
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                        SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            discard();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        soul = tag.getCompound("Soul");
        life = tag.contains("Life") ? tag.getInt("Life") : 400;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.put("Soul", soul);
        tag.putInt("Life", life);
    }
}
