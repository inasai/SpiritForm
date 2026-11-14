package com.spiritform.vault;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;

/**
 * Сумісність із модом «Хроніки Акаши» (akashic_records). Книга стає сторонньою станцією для Самодостатності:
 * її кладуть у комірку так само, як блок станції з іншого мода.
 *
 * Мод Akashic не підключається під час компіляції. Книгу знаходимо в реєстрі предметів, а вікно відкриваємо
 * її власним методом use(): він сам відкриває меню, синхронізує Знання й сховище. Перевірки «книга в руці»
 * в меню немає, тому книга в інвентарі не потрібна.
 */
public final class AkashicCompat {

    public static final ResourceLocation BOOK_ID =
            ResourceLocation.fromNamespaceAndPath("akashic_records", "akashic_chronicles");

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AkashicCompat.class);

    private AkashicCompat() {
    }

    /** Книга Хронік Акаши або null, якщо мод не встановлено. */
    public static Item book() {
        return BuiltInRegistries.ITEM.getOptional(BOOK_ID).orElse(null);
    }

    public static boolean isBook(Item item) {
        Item book = book();
        return book != null && item == book;
    }

    /** Предмет-станція за ідентифікатором із списку сторонніх станцій (null, якщо це не книга чи мода немає). */
    public static Item stationItem(ResourceLocation id) {
        return BOOK_ID.equals(id) ? book() : null;
    }

    /** Відкриває вікно Хронік Акаши для гравця. Повертає false, якщо не вдалося. */
    public static boolean open(ServerPlayer player) {
        Item book = book();
        if (book == null) {
            return false;
        }
        try {
            book.use(player.serverLevel(), player, InteractionHand.MAIN_HAND);
            return true;
        } catch (RuntimeException e) {
            LOGGER.warn("Не вдалося відкрити Хроніки Акаши", e);
            return false;
        }
    }
}
