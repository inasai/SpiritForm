package com.spiritform.vault;

import com.spiritform.SpiritFormMod;
import com.spiritform.item.SoulCatcherItem;
import com.spiritform.item.SpiritReaper;
import com.spiritform.registry.SpiritItems;
import com.spiritform.registry.SpiritParticles;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Предмети в комірках рук сховища, що працюють порожньою рукою.
 *
 * Коса Потойбіччя (комірка основної руки): заряд від вбивств і Жниво див. SpiritReaper; тут лише активація:
 * затисни ПКМ порожньою рукою на 3 секунди (без Shift).
 *
 * Ловець Душ (комірка допоміжної руки): Shift + ПКМ по мобу ловить його, Shift + затиснутий ПКМ (5 секунд, як
 * зі звичайним Ловцем) поглинає душу. Випустити душу зі сховища не можна.
 *
 * Обидва предмети мусять бути в комірці з увімкненим "живленням", а справжня основна рука мусить бути порожньою.
 * Клієнт (див. SpiritVaultClient) лише повідомляє, що кнопка затиснута; відлік і перевірки ведуться тут.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritVaultHands {

    public static final int KIND_SCYTHE = 0;
    public static final int KIND_CATCHER = 1;

    /** Скільки тіків треба тримати кнопку, щоб активувати Косу (3 секунди). */
    public static final int SCYTHE_HOLD_TICKS = 60;

    /** Якщо кнопка не підтверджується довше за цей час (у тіках), вважаємо, що її відпущено. */
    private static final int KEEPALIVE_GAP = 8;

    private static final class Hold {
        final int kind;
        final long start;
        long last;
        boolean done;

        Hold(int kind, long start) {
            this.kind = kind;
            this.start = start;
            this.last = start;
        }
    }

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    private SpiritVaultHands() {
    }

    // ---------- допоміжне ----------

    private static ItemStack scythe(Player player) {
        return player.getMainHandItem().isEmpty() && !player.isShiftKeyDown()
                ? SpiritReaper.vaultScythe(player) : ItemStack.EMPTY;
    }

    private static ItemStack catcher(Player player) {
        return player.getMainHandItem().isEmpty() && player.isShiftKeyDown()
                ? SpiritVault.handStack(player, SpiritVault.CELL_OFF_HAND, SpiritItems.SOUL_CATCHER.get())
                : ItemStack.EMPTY;
    }

    private static int totalTicks(int kind) {
        return kind == KIND_SCYTHE ? SCYTHE_HOLD_TICKS : SoulCatcherItem.HOLD_TICKS;
    }

    /** Чи відповідають умови затискання: предмет на місці, рука порожня, потрібний стан Shift. */
    private static boolean valid(ServerPlayer player, int kind) {
        if (kind == KIND_SCYTHE) {
            ItemStack stack = scythe(player);
            return !stack.isEmpty() && !SpiritReaper.active(stack) && SpiritReaper.charge(stack) > 0;
        }
        ItemStack stack = catcher(player);
        return !stack.isEmpty() && SoulCatcherItem.isFull(stack);
    }

    // ---------- затискання ----------

    /** Клієнт повідомляє, що кнопка затиснута (надходить кілька разів на секунду). */
    public static void hold(ServerPlayer player, int kind) {
        if (kind != KIND_SCYTHE && kind != KIND_CATCHER) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Hold hold = HOLDS.get(id);
        if (hold == null || hold.kind != kind || now - hold.last > KEEPALIVE_GAP) {
            hold = new Hold(kind, now);
            HOLDS.put(id, hold);
            if (!valid(player, kind)) {
                // Нема чого активувати: чекаємо відпускання кнопки, щоб не повторювати повідомлення
                hold.done = true;
                if (kind == KIND_SCYTHE) {
                    ItemStack stack = scythe(player);
                    if (!stack.isEmpty() && !SpiritReaper.active(stack) && SpiritReaper.charge(stack) <= 0) {
                        player.displayClientMessage(Component.translatable("message.spirit_form.scythe.empty"), true);
                    }
                }
            }
        }
        hold.last = now;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Hold hold = HOLDS.get(player.getUUID());
        if (hold == null) {
            return;
        }
        long now = level.getGameTime();
        if (now - hold.last > KEEPALIVE_GAP) {
            HOLDS.remove(player.getUUID());
            return;
        }
        if (hold.done) {
            return;
        }
        if (!valid(player, hold.kind)) {
            // Умови зникли (Shift відпущено, предмет вимкнено чи забрано): чекаємо повторного затискання
            hold.done = true;
            return;
        }

        int used = (int) (now - hold.start);
        int total = totalTicks(hold.kind);
        if (used > 6 && used % 4 == 0) {
            if (hold.kind == KIND_SCYTHE) {
                level.sendParticles(SpiritParticles.MINT, player.getX(), player.getY() + 1.0, player.getZ(),
                        3, 0.4, 0.6, 0.4, 0.02);
            } else {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, player.getX(), player.getY() + 1.0,
                        player.getZ(), 3, 0.4, 0.6, 0.4, 0.02);
            }
        }
        if (used > 6 && used % 10 == 0 && used < total) {
            String key = hold.kind == KIND_SCYTHE
                    ? "message.spirit_form.vault.scythe_hold" : "message.spirit_form.catcher.absorbing";
            player.displayClientMessage(Component.translatable(key, String.valueOf(used * 100 / total)), true);
        }
        if (used < total) {
            return;
        }

        hold.done = true;
        if (hold.kind == KIND_SCYTHE) {
            ItemStack stack = scythe(player);
            if (!stack.isEmpty()) {
                SpiritReaper.onUse(player, stack);
                SpiritVault.touch(player);
            }
        } else {
            ItemStack stack = catcher(player);
            if (!stack.isEmpty() && stack.getItem() instanceof SoulCatcherItem item) {
                item.finishUsingItem(stack, level, player);
                SpiritVault.touch(player);
            }
        }
    }

    // ---------- ловля мобів ----------

    /**
     * Shift + ПКМ по сутності порожньою рукою з Ловцем у сховищі: ловить моба. Навіть коли Ловець повний, взаємодія
     * скасовується (щоб затискання не відкривало торгівлю чи сідло), але моба він не випускає.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stored = catcher(player);
        if (stored.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.PASS);
        if (event.getHand() != InteractionHand.MAIN_HAND || SoulCatcherItem.isFull(stored)
                || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!event.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            SoulCatcherItem.catchMob(stored, serverPlayer, target);
            SpiritVault.touch(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HOLDS.remove(event.getEntity().getUUID());
    }
}
