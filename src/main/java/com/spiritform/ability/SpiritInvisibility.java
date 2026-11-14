
package com.spiritform.ability;

import com.spiritform.Config;
import com.spiritform.data.SpiritData;
import com.spiritform.form.SpiritFormState;
import com.spiritform.item.FentonBracelet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class SpiritInvisibility {

    /**
     * Зникнення гравець просто вмикає й вимикає (стан у SpiritData.invisible), тривалості в нього немає.
     * Часовий відлік лишається тільки для невидимості, яку вмикає Марення (forceOn).
     */
    private static final Map<UUID, Long> FORCED_UNTIL = new HashMap<>();
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

        // Уже невидимий (сам увімкнув або від Марення): повторне натискання вимикає
        if (data.invisible()) {
            FORCED_UNTIL.remove(id);
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

        long cooldown = FentonBracelet.cooldown(player, Config.ABILITY_COOLDOWN_SECONDS.get() * 20L);

        SpiritFormState.set(player, data.withInvisible(true));

        COOLDOWN_UNTIL.put(id, now + cooldown);
        SpiritCooldowns.start(player, "vanish", cooldown);

        player.displayClientMessage(
                Component.translatable("message.spirit_form.invisibility.activated"),
                true);
        playSound(player, 1.0F);
    }

    /**
     * Вмикає Зникнення без витрат голоду й перезарядки (для Марення) на обмежений час.
     * Якщо гравець уже невидимий сам, нічого не змінюється й таймер не вмикається.
     */
    public static void forceOn(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        SpiritData data = SpiritFormState.get(player);

        if (data.invisible() && !FORCED_UNTIL.containsKey(id)) {
            return;
        }
        boolean already = data.invisible();

        FORCED_UNTIL.put(id, now + Config.INVISIBILITY_DURATION_SECONDS.get() * 20L);
        if (!already) {
            SpiritFormState.set(player, data.withInvisible(true));
            playSound(player, 1.0F);
        }
    }

    /** Чи гравець зараз невидимий. Заодно вимикає невидимість від Марення, коли її час сплив. */
    public static boolean isActive(ServerPlayer player) {
        UUID id = player.getUUID();
        Long until = FORCED_UNTIL.get(id);

        if (until != null && player.level().getGameTime() >= until) {
            FORCED_UNTIL.remove(id);

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

        return SpiritFormState.get(player).invisible();
    }

    public static void clear(ServerPlayer player) {
        UUID id = player.getUUID();

        FORCED_UNTIL.remove(id);
        COOLDOWN_UNTIL.remove(id);

        SpiritData data = SpiritFormState.get(player);
        if (data.invisible()) {
            SpiritFormState.set(player, data.withInvisible(false));
        }
    }
}
