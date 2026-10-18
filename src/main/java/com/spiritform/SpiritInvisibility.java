
package com.spiritform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SpiritInvisibility {

    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    private SpiritInvisibility() {
    }

    public static void activate(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        UUID id = player.getUUID();
        long now = player.level().getGameTime();

        if (data.hunger() < SpiritBalance.MAX_LEVEL) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.invisibility.locked"),
                    true);
            return;
        }

        if (ACTIVE_UNTIL.getOrDefault(id, 0L) > now) {
            ACTIVE_UNTIL.remove(id);
            SpiritFormState.set(player, data.withInvisible(false));
            player.displayClientMessage(Component.translatable("message.spirit_form.invisibility.deactivated"), true);
            return;
        }

        if (COOLDOWN_UNTIL.getOrDefault(id, 0L) > now) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.ability.cooldown"),
                    true);
            return;
        }

        int cost = Config.ABILITY_HUNGER_COST.get();
        int food = player.getFoodData().getFoodLevel();

        if (food < cost) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.ability.hungry"),
                    true);
            return;
        }

        player.getFoodData().setFoodLevel(food - cost);

        long duration = Config.INVISIBILITY_DURATION_SECONDS.get() * 20L;
        long cooldown = Config.ABILITY_COOLDOWN_SECONDS.get() * 20L;

        SpiritFormState.set(player, data.withInvisible(true));

        ACTIVE_UNTIL.put(id, now + duration);
        COOLDOWN_UNTIL.put(id, now + cooldown);

        player.displayClientMessage(
                Component.translatable("message.spirit_form.invisibility.activated"),
                true);
    }

    public static boolean isActive(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        Long until = ACTIVE_UNTIL.get(id);

        if (until == null) {
            return false;
        }

        if (now >= until) {
            ACTIVE_UNTIL.remove(id);

            SpiritData data = SpiritFormState.get(player);
            if (data.invisible()) {
                SpiritFormState.set(player, data.withInvisible(false));
            }

            player.displayClientMessage(
                    Component.translatable("message.spirit_form.invisibility.timeout"),
                    true
            );

            return false;
        }

        return true;
    }

    public static void clear(ServerPlayer player) {
        UUID id = player.getUUID();

        ACTIVE_UNTIL.remove(id);
        COOLDOWN_UNTIL.remove(id);

        SpiritData data = SpiritFormState.get(player);
        if (data.invisible()) {
            SpiritFormState.set(player, data.withInvisible(false));
        }
    }
}
