package com.spiritform.form;

import com.spiritform.Config;
import com.spiritform.ability.SpiritPossession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Правила режимів «Форма Існування» (Overworld, Nether, Beyond). */
public final class SpiritExistence {

    /** Позначка в даних гравця: він зараз Привид «Незеру» і мусить вийти з одержимого моба. */
    private static final String NETHER_KEY = "spirit_form_nether_ghost";

    /** Тривалість форми поза Потойбіччям у режимі Beyond: одна хвилина. */
    private static final long BEYOND_LIMIT_TICKS = 1200L;
    private static final long BEYOND_WARNING_TICKS = 200L;

    /** Тік, коли гравець увійшов у форму поза Потойбіччям. */
    private static final Map<UUID, Long> BEYOND_START = new HashMap<>();

    private SpiritExistence() {
    }

    public static ExistenceMode mode() {
        return Config.EXISTENCE_FORM.get();
    }

    public static boolean isNether() {
        return mode() == ExistenceMode.NETHER;
    }

    public static boolean isBeyond() {
        return mode() == ExistenceMode.BEYOND;
    }

    private static boolean marked(ServerPlayer player) {
        return player.getPersistentData().getBoolean(NETHER_KEY);
    }

    private static void mark(ServerPlayer player, boolean value) {
        if (value) {
            player.getPersistentData().putBoolean(NETHER_KEY, true);
        } else {
            player.getPersistentData().remove(NETHER_KEY);
        }
    }

    /** Смерть у режимі Nether: гравець відроджується Привидом. */
    public static void onRespawn(ServerPlayer player) {
        if (!isNether()) {
            mark(player, false);
            return;
        }
        mark(player, true);
        SpiritForm.activateForced(player);
        player.displayClientMessage(Component.translatable("message.spirit_form.nether.reborn"), false);
    }

    /** Вхід у світ: Привид «Незеру» лишається Привидом, а в інших режимах позначка зникає. */
    public static void onLogin(ServerPlayer player) {
        if (!isNether()) {
            mark(player, false);
            return;
        }
        if (marked(player)) {
            SpiritForm.activateForced(player);
        }
    }

    /**
     * Одержимість завершилася будь-яким способом (вихід, смерть моба, Марення, поглинання здоров'я).
     * У режимі Nether моб гине, а гравець знову стає людиною до наступної смерті.
     */
    public static void onPossessionEnded(ServerPlayer player, Mob mob) {
        if (!isNether() || !marked(player) || !player.isAlive() || player.hasDisconnected()) {
            return;
        }
        mark(player, false);
        if (mob != null && mob.isAlive()) {
            mob.kill();
        }
        if (SpiritFormState.isActive(player)) {
            SpiritForm.deactivate(player, "message.spirit_form.nether.returned");
        } else {
            player.displayClientMessage(Component.translatable("message.spirit_form.nether.returned"), true);
        }
    }

    /** Щотіку: у режимі Nether повертає втрачену форму, у режимі Beyond веде відлік хвилини. */
    public static void tick(ServerPlayer player) {
        ExistenceMode mode = mode();
        if (mode == ExistenceMode.NETHER) {
            BEYOND_START.remove(player.getUUID());
            if (marked(player) && !SpiritFormState.isActive(player) && player.isAlive()
                    && player.tickCount % 20 == 0) {
                SpiritForm.activateForced(player);
            }
            return;
        }

        if (mode != ExistenceMode.BEYOND || !SpiritFormState.isActive(player)
                || player.level().dimension().equals(SpiritBeyond.KEY)) {
            BEYOND_START.remove(player.getUUID());
            return;
        }

        long now = player.level().getGameTime();
        long start = BEYOND_START.computeIfAbsent(player.getUUID(), id -> now);
        long left = BEYOND_LIMIT_TICKS - (now - start);
        if (left <= 0L) {
            BEYOND_START.remove(player.getUUID());
            if (SpiritPossession.isPossessing(player)) {
                SpiritPossession.drainAndExit(player);
            } else {
                SpiritForm.deactivate(player, "message.spirit_form.beyond.timeout");
                SpiritForm.startCooldown(player);
            }
        } else if (left == BEYOND_WARNING_TICKS) {
            player.displayClientMessage(Component.translatable("message.spirit_form.beyond.warning"), true);
        }
    }

    public static void clear(ServerPlayer player) {
        BEYOND_START.remove(player.getUUID());
    }
}
