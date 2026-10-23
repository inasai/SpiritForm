package com.spiritform;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Уламок Душі: чорний дим на місці смерті гравця з увімкненими Інстинктами Привида.
 * Зберігає предмети разом із номерами слотів (інвентар, броня, друга рука). ПКМ по диму повертає
 * предмети на свої місця; якщо місце зайняте, предмет іде у вільний слот або падає під ноги.
 * Забрати може лише власник. Кількість уламків одного гравця обмежує конфіг, найстаріші зникають.
 */
public class SoulShardEntity extends Entity {

    private UUID owner;
    private final List<CompoundTag> items = new ArrayList<>();

    public SoulShardEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    public void setContents(UUID owner, List<CompoundTag> contents) {
        this.owner = owner;
        this.items.clear();
        this.items.addAll(contents);
    }

    public UUID getOwner() {
        return owner;
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

        // Чорний дим
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 2, 0.25, 0.5, 0.25, 0.01);
        if (tickCount % 2 == 0) {
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 3, 0.3, 0.6, 0.3, 0.02);
        }

        // Застарілий уламок (власник уже має новіший) зникає
        if (owner != null && tickCount % 40 == 20
                && !SoulShardIndex.get(server.getServer()).keep(owner, getUUID(), Config.SOUL_SHARD_LIMIT.get())) {
            discard();
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
        if (owner != null && !owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
            serverPlayer.displayClientMessage(Component.translatable("message.spirit_form.shard.not_yours"), true);
            return InteractionResult.SUCCESS;
        }

        restore(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    private void restore(ServerPlayer player) {
        HolderLookup.Provider access = level().registryAccess();
        Inventory inventory = player.getInventory();

        for (CompoundTag tag : items) {
            ItemStack stack = ItemStack.parseOptional(access, tag);
            if (stack.isEmpty()) {
                continue;
            }
            int slot = tag.getInt("Slot");
            if (slot >= 0 && slot < inventory.getContainerSize() && inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack);
            } else {
                inventory.add(stack);
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                }
            }
        }

        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.8, getZ(), 24, 0.3, 0.6, 0.3, 0.05);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (owner != null) {
                SoulShardIndex.get(server.getServer()).remove(owner, getUUID());
            }
        }
        player.displayClientMessage(Component.translatable("message.spirit_form.shard.restored"), true);
        SpiritAdvancements.grant(player, "shard");
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        items.clear();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            items.add(list.getCompound(i));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        ListTag list = new ListTag();
        list.addAll(items);
        tag.put("Items", list);
    }
}
