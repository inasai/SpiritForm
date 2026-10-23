package com.spiritform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Повідомлення про перезарядку здібностей. Спершу "Ти втомився", а коли воно зникне,
 * щосекунди "Ти відновишся через N с" до самого відновлення.
 */
public final class SpiritCooldowns {

    /** Через скільки тіків після "Ти втомився" з'являється відлік (повідомлення живе близько 3 с). */
    private static final long COUNTDOWN_DELAY_TICKS = 60;

    private record Notice(long until, long showAt) {
    }

    private static final Map<UUID, Notice> NOTICES = new HashMap<>();

    private SpiritCooldowns() {
    }

    /** Гравець спробував скористатися здібністю під час перезарядки. */
    public static void notifyTired(ServerPlayer player, long untilTick) {
        long now = player.level().getGameTime();
        player.displayClientMessage(Component.translatable("message.spirit_form.ability.cooldown"), true);
        NOTICES.put(player.getUUID(), new Notice(untilTick, now + COUNTDOWN_DELAY_TICKS));
    }

    public static void tick(ServerPlayer player) {
        Notice notice = NOTICES.get(player.getUUID());
        if (notice == null) {
            return;
        }

        long now = player.level().getGameTime();
        if (now >= notice.until()) {
            NOTICES.remove(player.getUUID());
            return;
        }

        if (now >= notice.showAt() && (now - notice.showAt()) % 20 == 0) {
            long seconds = (notice.until() - now + 19) / 20;
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.ability.recovering", seconds),
                    true
            );
        }
    }

    public static void clear(ServerPlayer player) {
        NOTICES.remove(player.getUUID());
    }
}
