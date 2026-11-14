package com.spiritform.vault;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Єдина точка запитів про артефакти, що лежать у сховищі гравця (див. SpiritVault).
 * Усі артефакти мода питають про своє "носіння" та видимість тут. Власні слоти аксесуарів іншого мода
 * більше не використовуються.
 */
public final class SpiritAccessories {

    /** Де лежить предмет: ніде, у комірці сховища з вимкненим показом ("око" закрите) чи з видимим. */
    public enum State {
        NONE,
        HIDDEN,
        VISIBLE
    }

    private SpiritAccessories() {
    }

    /** Стан предмета в сховищі. Комірка з вимкненим "живленням" вважається порожньою. */
    public static State state(LivingEntity entity, Item item) {
        return SpiritVault.state(entity, item);
    }

    /** Перший предмет такого виду в комірках сховища (сам стек, його можна змінювати) або порожній стек. */
    public static ItemStack findStack(LivingEntity entity, Item item) {
        return SpiritVault.findStack(entity, item);
    }

    /** Предмет у сховищі й діє (навіть якщо його сховано "оком"). */
    public static boolean isEquipped(LivingEntity entity, Item item) {
        return state(entity, item) != State.NONE;
    }
}
