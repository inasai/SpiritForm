package com.spiritform;

import java.util.List;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Правила розвитку душ Армії (спільні для сервера й клієнта).
 *
 * Ранг росте від кількості вбивств душі. Перший ранг (0) без бонусів; кожне підвищення: 1 - здоров'я вдвічі,
 * 2 - шкода вдвічі, 3 - швидкість вдвічі, 4 - здоров'я й шкода ще раз удвічі (швидкість уже ні). Окремо кожну з трьох характеристик можна
 * вдосконалювати за Кристалізовані Душі (до MAX_LEVEL рівнів).
 */
public final class ArmySoulStats {

    public static final int MAX_LEVEL = 10;
    /** Скільки вбивств потрібно для рангу з відповідним номером. */
    public static final int[] RANK_KILLS = {0, 10, 30, 70, 150};
    public static final int MAX_RANK = RANK_KILLS.length - 1;

    public static final double LEVEL_HEALTH = 0.10;
    public static final double LEVEL_DAMAGE = 0.10;
    public static final double LEVEL_SPEED = 0.03;
    /** Швидкість душі не перевищує цей множник від початкової (щоб моби не ставали некерованими). */
    public static final double SPEED_CAP = 5.0;

    public static final int STAT_HEALTH = 0;
    public static final int STAT_DAMAGE = 1;
    public static final int STAT_SPEED = 2;

    public static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ArmySoulStats() {
    }

    public static int rank(int kills) {
        int rank = 0;
        for (int i = 1; i < RANK_KILLS.length; i++) {
            if (kills >= RANK_KILLS[i]) {
                rank = i;
            }
        }
        return rank;
    }

    /** Скільки вбивств треба для наступного рангу або -1, якщо ранг найвищий. */
    public static int nextKills(int rank) {
        return rank >= MAX_RANK ? -1 : RANK_KILLS[rank + 1];
    }

    private static int doublings(int rank, int promotion) {
        // promotion: 1 - здоров'я, 2 - шкода, 3 - швидкість; четверте підвищення подвоює все ще раз
        // Швидкість на найвищому рангу вдруге не подвоюється: лише здоров'я й шкода
        return (rank >= promotion ? 1 : 0) + (rank >= 4 && promotion != 3 ? 1 : 0);
    }

    public static double healthMult(int rank, int level) {
        return (1 << doublings(rank, 1)) * (1.0 + LEVEL_HEALTH * level);
    }

    public static double damageMult(int rank, int level) {
        return (1 << doublings(rank, 2)) * (1.0 + LEVEL_DAMAGE * level);
    }

    public static double speedMult(int rank, int level) {
        // Приріст швидкості вдвічі м'якший за решту: бонус рангу й рівнів ділиться навпіл
        double full = (1 << doublings(rank, 3)) * (1.0 + LEVEL_SPEED * level);
        return Math.min(SPEED_CAP, 1.0 + (full - 1.0) / 2.0);
    }

    /** Ціна наступного рівня в Кристалізованих Душах. */
    public static int cost(int level) {
        return level + 1;
    }

    /** Слот броні (0 шолом .. 3 чоботи) для предмета або -1, якщо це не обладунок. */
    public static int armorIndex(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        net.minecraft.world.item.Equipable equipable = net.minecraft.world.item.Equipable.get(stack);
        EquipmentSlot slot = equipable == null ? null : equipable.getEquipmentSlot();
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            if (ARMOR_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    /** Сума захисту (0) і міцності (1) усіх одягнених обладунків. */
    public static double[] armorTotals(List<ItemStack> armor) {
        double[] totals = new double[2];
        for (int i = 0; i < armor.size() && i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = armor.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            stack.forEachModifier(ARMOR_SLOTS[i], (attribute, modifier) -> {
                if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE) {
                    return;
                }
                if (attribute.value() == Attributes.ARMOR.value()) {
                    totals[0] += modifier.amount();
                } else if (attribute.value() == Attributes.ARMOR_TOUGHNESS.value()) {
                    totals[1] += modifier.amount();
                }
            });
        }
        return totals;
    }
}
