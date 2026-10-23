
package com.spiritform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class SpiritInvisibility {

    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    private SpiritInvisibility() {
    }

    /** Дихання Блейза. Увімкнення звучить вище, вимкнення нижче. */
    private static void playSound(ServerPlayer player, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.8F, pitch);
    }

    public static void activate(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        UUID id = player.getUUID();
        long now = player.level().getGameTime();

        if (!data.hasAbility(SpiritAbility.VANISH)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.invisibility.locked"),
                    true);
            return;
        }

        if (ACTIVE_UNTIL.getOrDefault(id, 0L) > now) {
            ACTIVE_UNTIL.remove(id);
            SpiritFormState.set(player, data.withInvisible(false));
            player.displayClientMessage(Component.translatable("message.spirit_form.invisibility.deactivated"), true);
            playSound(player, 0.7F);
            return;
        }

        if (COOLDOWN_UNTIL.getOrDefault(id, 0L) > now) {
            SpiritCooldowns.notifyTired(player, COOLDOWN_UNTIL.getOrDefault(id, 0L));
            return;
        }

        int cost = FentonBracelet.cost(player, Config.ABILITY_HUNGER_COST.get());
        int food = player.getFoodData().getFoodLevel();

        if (food < cost) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.ability.hungry"),
                    true);
            return;
        }

        player.getFoodData().setFoodLevel(food - cost);

        long duration = Config.INVISIBILITY_DURATION_SECONDS.get() * 20L;
        long cooldown = FentonBracelet.cooldown(player, Config.ABILITY_COOLDOWN_SECONDS.get() * 20L);

        SpiritFormState.set(player, data.withInvisible(true));

        ACTIVE_UNTIL.put(id, now + duration);
        COOLDOWN_UNTIL.put(id, now + cooldown);

        player.displayClientMessage(
                Component.translatable("message.spirit_form.invisibility.activated"),
                true);
        playSound(player, 1.0F);
    }

    /** Вмикає Зникнення без витрат голоду й перезарядки (для Марення). Повторний виклик лише продовжує дію. */
    public static void forceOn(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        boolean already = ACTIVE_UNTIL.getOrDefault(id, 0L) > now;

        ACTIVE_UNTIL.put(id, now + Config.INVISIBILITY_DURATION_SECONDS.get() * 20L);
        if (!already) {
            SpiritData data = SpiritFormState.get(player);
            SpiritFormState.set(player, data.withInvisible(true));
            playSound(player, 1.0F);
        }
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
            playSound(player, 0.7F);

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
