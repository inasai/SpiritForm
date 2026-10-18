package com.spiritform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Підсвічування мобів лише для самого гравця (рівень 15 гілки Темряви).
 * Працює тільки на клієнті, тому інші гравці підсвічування не бачать.
 * Клавіша підсвічування вмикає й вимикає здібність; після перезапуску гри вона знову ввімкнена.
 */
public final class SpiritClientHighlight {

    private static boolean enabled = false;

    private SpiritClientHighlight() {
    }

    /**
     * Викликається клавішею перемикання підсвічування.
     */
    public static void toggle() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        SpiritData data = SpiritFormState.get(player);
        if (!SpiritBalance.highlightsMobs(data.darkness())) {
            player.displayClientMessage(Component.translatable("message.spirit_form.highlight.locked"), true);
            return;
        }

        enabled = !enabled;
        player.displayClientMessage(
                Component.translatable(enabled
                        ? "message.spirit_form.highlight.on"
                        : "message.spirit_form.highlight.off"),
                true
        );
    }

    public static boolean shouldHighlight(Entity entity) {
        if (!enabled) {
            return false;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity == player || !(entity instanceof Mob)) {
            return false;
        }

        SpiritData data = SpiritFormState.get(player);
        if (!data.active() || !SpiritBalance.highlightsMobs(data.darkness())) {
            return false;
        }

        double radius = Config.HIGHLIGHT_RADIUS.get();
        return entity.distanceToSqr(player) <= radius * radius;
    }
}
