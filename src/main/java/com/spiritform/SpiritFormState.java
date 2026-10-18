package com.spiritform;

import net.minecraft.world.entity.player.Player;

/**
 * Зручний доступ до даних форми. Змінювати дані (set/setActive) можна лише на сервері,
 * читати можна де завгодно: на клієнті дані приходять із сервера автоматично.
 */
public final class SpiritFormState {

    private SpiritFormState() {
    }

    public static SpiritData get(Player player) {
        return player.getData(SpiritAttachments.SPIRIT.get());
    }

    public static void set(Player player, SpiritData data) {
        player.setData(SpiritAttachments.SPIRIT.get(), data);
    }

    public static boolean isActive(Player player) {
        return get(player).active();
    }

    public static void setActive(Player player, boolean active) {
        set(player, get(player).withActive(active));
    }
}
