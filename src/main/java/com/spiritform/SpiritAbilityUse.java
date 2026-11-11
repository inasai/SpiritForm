package com.spiritform;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Спільні дії над здібностями за їхнім ідентифікатором: чи відкрита, чи ввімкнена та як її "використати".
 * Потрібні і колесу (клавіша T), і панелям цифр та основних клавіш: у будь-яке з цих місць тепер можна
 * покласти будь-яку здібність, і пасивна там перемикається, а активна запускається.
 */
public final class SpiritAbilityUse {

    private SpiritAbilityUse() {
    }

    /** Дія перемикання на сервері для пасивної здібності, або -1, якщо це не перемикач. */
    public static int toggleAction(SpiritAbility ability) {
        return switch (ability) {
            case ICY_TOUCH -> UpgradeSpiritPayload.TOGGLE_ICY;
            case PRESENCE -> UpgradeSpiritPayload.TOGGLE_PRESENCE;
            case GAZE -> UpgradeSpiritPayload.TOGGLE_GAZE;
            case ECHO -> UpgradeSpiritPayload.TOGGLE_ECHO;
            case ENERGY -> UpgradeSpiritPayload.TOGGLE_ENERGY;
            default -> -1;
        };
    }

    /** Чи відкрита здібність гравцеві. */
    public static boolean owned(SpiritData data, String id) {
        if (SpiritLoadout.INSTINCT.equals(id)) {
            return data.unlocked();
        }
        SpiritAbility ability = SpiritAbility.byId(id);
        return ability != null && data.hasAbility(ability);
    }

    /** Чи керує гравець іншою істотою (Одержимість). */
    private static boolean controllingOther(Minecraft minecraft) {
        Entity camera = minecraft.getCameraEntity();
        return minecraft.player != null && camera != null && camera != minecraft.player;
    }

    /** Увімкнена чи ні. Null, якщо у здібності немає стану (разова дія). */
    public static Boolean state(Minecraft minecraft, SpiritData data, String id) {
        if (SpiritLoadout.INSTINCT.equals(id)) {
            return data.instinct();
        }
        SpiritAbility ability = SpiritAbility.byId(id);
        if (ability == null) {
            return null;
        }
        return switch (ability) {
            case ICY_TOUCH -> data.icy();
            case PRESENCE -> data.presence();
            case GAZE -> data.gaze();
            case ECHO -> data.echo();
            case ENERGY -> data.energy();
            case VANISH -> data.invisible();
            case SENSE -> SpiritClientHighlight.isEnabled();
            case POSSESSION -> controllingOther(minecraft);
            default -> null;
        };
    }

    /** Використати здібність один раз: перемкнути пасивну або запустити активну (утримувані сюди не потрапляють). */
    public static void use(String id) {
        if (SpiritLoadout.INSTINCT.equals(id)) {
            PacketDistributor.sendToServer(new UpgradeSpiritPayload(UpgradeSpiritPayload.TOGGLE_INSTINCT));
            return;
        }
        SpiritAbility ability = SpiritAbility.byId(id);
        if (ability != null) {
            SpiritAbilityMode.trigger(ability);
        }
    }
}
