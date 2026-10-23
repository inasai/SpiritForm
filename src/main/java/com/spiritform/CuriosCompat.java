package com.spiritform;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Curios підключається необов'язково й без залежності на етапі збірки: його API викликається через рефлексію.
 * Беремо всі слоти гравця (CuriosApi.getCuriosInventory -> getCurios -> getStacks / getRenders).
 * Якщо Curios немає або його API інакше, вважаємо, що там нічого немає.
 */
public final class CuriosCompat {

    /** Де лежить предмет: ніде, у слоті Curios зі сховним виглядом (око закрите) чи з видимим. */
    public enum State {
        NONE,
        HIDDEN,
        VISIBLE
    }

    private static boolean resolved;
    private static Method getInventory;

    private CuriosCompat() {
    }

    private static void resolve() {
        resolved = true;
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            getInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
        } catch (Throwable ignored) {
            getInventory = null;
        }
    }

    /** Викликає публічний метод без параметрів (клас може бути непублічним, тож шукаємо серед getMethods). */
    private static Object call(Object target, String name) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                    // достатньо й публічного доступу
                }
                return method.invoke(target);
            }
        }
        return null;
    }

    /** Стан предмета в слотах Curios. Видимість береться з перемикача "ока" біля слота. */
    public static State state(LivingEntity entity, Item item) {
        if (!resolved) {
            resolve();
        }
        if (getInventory == null) {
            return State.NONE;
        }
        State result = State.NONE;
        try {
            Object inventory = getInventory.invoke(null, entity);
            if (!(inventory instanceof Optional<?> optional) || optional.isEmpty()) {
                return State.NONE;
            }
            if (!(call(optional.get(), "getCurios") instanceof Map<?, ?> slots)) {
                return State.NONE;
            }
            for (Object handler : slots.values()) {
                if (!(call(handler, "getStacks") instanceof IItemHandler stacks)) {
                    continue;
                }
                List<?> renders = call(handler, "getRenders") instanceof List<?> list ? list : null;
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (!stacks.getStackInSlot(i).is(item)) {
                        continue;
                    }
                    boolean visible = renders == null || i >= renders.size()
                            || Boolean.TRUE.equals(renders.get(i));
                    if (visible) {
                        return State.VISIBLE;
                    }
                    result = State.HIDDEN;
                }
            }
        } catch (Throwable ignored) {
            // Curios відповів не так, як очікувалося
        }
        return result;
    }

    public static boolean isEquipped(LivingEntity entity, Item item) {
        return state(entity, item) != State.NONE;
    }
}
