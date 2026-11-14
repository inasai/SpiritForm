package com.spiritform.item;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.data.VaultData;
import com.spiritform.registry.SpiritItems;
import com.spiritform.vault.SpiritAccessories;
import com.spiritform.vault.SpiritVault;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Памʼять Душі: щосекунди відновлює міцність усієї броні та предметів в основній і допоміжній руці,
 * а також усіх предметів у комірках спорядження сховища.
 * Артефакт діє, лежачи в руці, у комірці сховища. Сам він має міцність: за кожні N відновлених очок
 * (soulMemoryPointsPerDurability) втрачає одне очко своєї, а коли міцність скінчиться, ламається.
 * Відновити його можна Кристалізованою Душею на верстаті (CrystalRepairRecipe).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SoulMemory {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    /** Скільки відновлених очок ще не оплачено міцністю артефакта. */
    private static final Map<UUID, Integer> PENDING = new HashMap<>();

    private SoulMemory() {
    }

    private static boolean is(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SpiritItems.SOUL_MEMORY.get());
    }

    /** Сам предмет-артефакт, що діє зараз: з руки або зі сховища. */
    private static ItemStack artifact(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (is(stack)) {
                return stack;
            }
        }
        return SpiritAccessories.findStack(player, SpiritItems.SOUL_MEMORY.get());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0
                || player.isSpectator() || !player.isAlive()) {
            return;
        }
        ItemStack artifact = artifact(player);
        if (artifact.isEmpty()) {
            return;
        }

        int perSecond = Config.SOUL_MEMORY_REPAIR_PER_SECOND.get();
        int restored = 0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || stack == artifact || is(stack) || !stack.isDamageableItem() || !stack.isDamaged()) {
                continue;
            }
            int amount = Math.min(perSecond, stack.getDamageValue());
            stack.setDamageValue(stack.getDamageValue() - amount);
            restored += amount;
        }

        // Предмети в комірках сховища (усі шість комірок спорядження) лагодяться так само
        boolean vaultChanged = false;
        VaultData vault = SpiritVault.get(player);
        for (int i = 0; i < VaultData.GEAR_SIZE; i++) {
            ItemStack stack = vault.gearStack(i);
            if (!vault.gearUnlocked(i) || stack.isEmpty() || stack == artifact || is(stack)
                    || !stack.isDamageableItem() || !stack.isDamaged()) {
                continue;
            }
            int amount = Math.min(perSecond, stack.getDamageValue());
            stack.setDamageValue(stack.getDamageValue() - amount);
            restored += amount;
            vaultChanged = true;
        }

        if (restored > 0) {
            pay(player, artifact, restored);
            if (vaultChanged || SpiritVault.get(player).artifacts().contains(artifact)) {
                SpiritVault.touch(player);
            }
        }
    }

    /** Платить міцністю артефакта за відновлені очок; на нуль він ламається. */
    private static void pay(ServerPlayer player, ItemStack artifact, int restored) {
        int ratio = Config.SOUL_MEMORY_POINTS_PER_DURABILITY.get();
        int pending = PENDING.getOrDefault(player.getUUID(), 0) + restored;
        int cost = pending / ratio;
        PENDING.put(player.getUUID(), pending % ratio);
        if (cost <= 0) {
            return;
        }

        int damage = artifact.getDamageValue() + cost;
        if (damage >= artifact.getMaxDamage()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9F, 1.0F);
            artifact.shrink(1);
            PENDING.remove(player.getUUID());
        } else {
            artifact.setDamageValue(damage);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }
}
