package com.spiritform;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Службові дії з Уламками Душі: оновлення міток для клієнта та повернення втраченого уламка. */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SoulShardTools {

    private SoulShardTools() {
    }

    /** Оновлює вкладення гравця з місцями його уламків (клієнт веде до найближчого слід диму Чуття). */
    public static void pushMarks(MinecraftServer server, UUID owner) {
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        SoulShardIndex index = SoulShardIndex.get(server);
        List<ShardMarks.Mark> marks = new ArrayList<>();
        for (UUID id : index.shardsOf(owner)) {
            SoulShardIndex.Position position = index.position(id);
            if (position != null) {
                marks.add(new ShardMarks.Mark(position.dimension(), position.x(), position.y(), position.z()));
            }
        }
        ShardMarks next = new ShardMarks(marks);
        if (!next.equals(player.getData(SpiritAttachments.SHARD_MARKS.get()))) {
            player.setData(SpiritAttachments.SHARD_MARKS.get(), next);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            pushMarks(player.getServer(), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            pushMarks(player.getServer(), player.getUUID());
        }
    }

    /**
     * Повертає найновіший Уламок Душі з резервної копії біля гравця. Якщо такий уламок ще існує у завантаженому світі,
     * нового не створюється (щоб не подвоїти предмети), а гравцеві кажуть, де він.
     */
    public static boolean recall(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        SoulShardIndex index = SoulShardIndex.get(server);
        List<UUID> ids = index.shardsOf(player.getUUID());
        UUID chosen = null;
        for (int i = ids.size() - 1; i >= 0; i--) {
            if (index.backupOf(ids.get(i)) != null) {
                chosen = ids.get(i);
                break;
            }
        }
        if (chosen == null) {
            player.sendSystemMessage(Component.translatable("command.spirit_form.shard.none"));
            return false;
        }

        for (ServerLevel other : server.getAllLevels()) {
            Entity existing = other.getEntity(chosen);
            if (existing != null && existing.isAlive()) {
                player.sendSystemMessage(Component.translatable("command.spirit_form.shard.exists",
                        String.valueOf(Math.round(existing.getX())), String.valueOf(Math.round(existing.getY())),
                        String.valueOf(Math.round(existing.getZ())), other.dimension().location().toString()));
                return false;
            }
        }

        List<CompoundTag> items = new ArrayList<>(index.backupOf(chosen));
        ServerLevel level = player.serverLevel();
        SoulShardEntity shard = new SoulShardEntity(SpiritEntities.SOUL_SHARD.get(), level);
        shard.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
        shard.setContents(player.getUUID(), items);
        if (!level.addFreshEntity(shard)) {
            player.sendSystemMessage(Component.translatable("command.spirit_form.shard.failed"));
            return false;
        }

        // Старий запис замінюється новим; якщо стара копія колись завантажиться, вона побачить, що вже не чинна
        index.remove(player.getUUID(), chosen);
        for (UUID evicted : index.register(player.getUUID(), shard.getUUID(), Config.SOUL_SHARD_LIMIT.get())) {
            for (ServerLevel other : server.getAllLevels()) {
                Entity old = other.getEntity(evicted);
                if (old != null) {
                    old.discard();
                }
            }
        }
        index.touch(shard.getUUID(), new SoulShardIndex.Position(level.dimension().location().toString(),
                shard.getX(), shard.getY(), shard.getZ()), items);
        pushMarks(server, player.getUUID());
        player.sendSystemMessage(Component.translatable("command.spirit_form.shard.done"));
        return true;
    }
}
