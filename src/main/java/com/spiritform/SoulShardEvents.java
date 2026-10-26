package com.spiritform;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Смерть гравця з увімкненими Інстинктами Привида: замість того, щоб предмети розсипалися, вони
 * лягають в Уламок Душі на місці смерті. Спрацьовує, лише якщо смерть справді настала (Інстинкт її не скасував).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SoulShardEvents {

    private SoulShardEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        SpiritData data = SpiritFormState.get(player);
        if (!data.instinct() || !data.unlocked()) {
            return;
        }
        // Без купленого Уламка речі не зберігаються
        if (data.shards() <= 0) {
            return;
        }
        // Із keepInventory речі й так лишаються
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        Inventory inventory = player.getInventory();
        List<CompoundTag> items = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag tag = (CompoundTag) stack.save(level.registryAccess());
            tag.putInt("Slot", slot);
            items.add(tag);
        }
        if (items.isEmpty()) {
            return;
        }

        SoulShardEntity shard = new SoulShardEntity(SpiritEntities.SOUL_SHARD.get(), level);
        double y = Math.max(player.getY(), level.getMinBuildHeight() + 2.0);
        shard.moveTo(player.getX(), y, player.getZ(), 0.0F, 0.0F);
        shard.setContents(player.getUUID(), items);
        if (!level.addFreshEntity(shard)) {
            return; // не вийшло створити: речі випадуть як зазвичай
        }
        inventory.clearContent();

        // Лишається не більше уламків, ніж дозволяє конфіг: найстаріші зникають
        List<UUID> removed = SoulShardIndex.get(level.getServer())
                .register(player.getUUID(), shard.getUUID(), data.shards());
        for (UUID id : removed) {
            for (ServerLevel other : level.getServer().getAllLevels()) {
                Entity old = other.getEntity(id);
                if (old != null) {
                    old.discard();
                }
            }
        }

        player.sendSystemMessage(Component.translatable("message.spirit_form.shard.created",
                String.valueOf(Math.round(shard.getX())), String.valueOf(Math.round(shard.getY())),
                String.valueOf(Math.round(shard.getZ())), level.dimension().location().toString()));
    }
}
