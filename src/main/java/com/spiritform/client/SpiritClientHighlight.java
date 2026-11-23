package com.spiritform.client;

import com.spiritform.Config;
import com.spiritform.ability.SpiritAbility;
import com.spiritform.data.SpiritData;
import com.spiritform.form.SpiritFormState;
import com.spiritform.form.SpiritRange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Підсвічування мобів лише для самого гравця (здібність Чуття).
 * Працює і у формі Привида, і у формі людини.
 * Працює тільки на клієнті, тому інші гравці підсвічування не бачать.
 * Клавіша підсвічування вмикає й вимикає здібність; за замовчуванням підсвічування вимкнене.
 */
public final class SpiritClientHighlight {

    private static boolean enabled = false;
    private static int cachedTick = -1;
    private static boolean cachedSense;
    private static double cachedRadiusSqr;

    private SpiritClientHighlight() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** М'ятні контури предметів і приглушений екран. */
    public static boolean outlines() {
        return Config.SENSE_OUTLINES.get();
    }

    /** Рамки сховищ крізь стіни. */
    public static boolean chests() {
        return Config.SENSE_CHESTS.get();
    }

    /** Підсвічування мобів крізь стіни. */
    public static boolean mobs() {
        return Config.SENSE_MOBS.get();
    }

    /** Перемикає одне з підсвічувань (0 контури, 1 сховища, 2 моби) і зберігає вибір у клієнтському конфігу. */
    public static void toggleOption(int option) {
        net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value = switch (option) {
            case 0 -> Config.SENSE_OUTLINES;
            case 1 -> Config.SENSE_CHESTS;
            default -> Config.SENSE_MOBS;
        };
        value.set(!value.get());
        Config.CLIENT_SPEC.save();
    }

    /** Викликається клавішею перемикання підсвічування. */
    public static void toggle() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        SpiritData data = SpiritFormState.get(player);
        if (!data.hasAbility(SpiritAbility.SENSE)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.highlight.locked"), true);
            return;
        }

        enabled = !enabled;

        // Посадка Вітрюка: вище при вмиканні, нижче при вимиканні
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.BREEZE_LAND, enabled ? 1.0F : 0.8F, 0.8F));
        player.displayClientMessage(
                Component.translatable(enabled
                        ? "message.spirit_form.highlight.on"
                        : "message.spirit_form.highlight.off"),
                true
        );
    }

    public static boolean shouldHighlight(Entity entity) {
        if (!enabled || !mobs()) {
            return false;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity == player || !(entity instanceof Mob)) {
            return false;
        }

        // Це питається щокадру для кожної сутності, тож радіус рахуємо раз на тік
        if (player.tickCount != cachedTick) {
            cachedTick = player.tickCount;
            cachedSense = SpiritFormState.get(player).hasAbility(SpiritAbility.SENSE);
            double radius = Config.HIGHLIGHT_RADIUS.get() * SpiritRange.mul(player);
            cachedRadiusSqr = radius * radius;
        }
        return cachedSense && entity.distanceToSqr(player) <= cachedRadiusSqr;
    }
}
