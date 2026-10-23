package com.spiritform;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

/**
 * Посібник "Щоденник моєї смерті" робиться за допомогою Patchouli (див. data/spirit_form/patchouli_books/diary).
 * Patchouli підключається необов'язково: предмет і компонент беруться за назвою, без залежності на етапі збірки.
 * Той самий посібник видається командою: /give @p patchouli:guide_book[patchouli:book="spirit_form:diary"]
 */
public final class SpiritGuide {

    public static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "diary");

    private SpiritGuide() {
    }

    /** Видає гравцеві посібник (якщо Patchouli встановлено). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void give(ServerPlayer player) {
        if (!Config.GIVE_GUIDE_BOOK.get() || !ModList.get().isLoaded("patchouli")) {
            return;
        }
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("patchouli:guide_book"));
            DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("patchouli:book"));
            if (item == Items.AIR || type == null) {
                return;
            }
            ItemStack stack = new ItemStack(item);
            stack.set(type, BOOK_ID);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        } catch (Throwable ignored) {
            // Patchouli іншої версії: посібник можна взяти з творчої вкладки чи командою
        }
    }
}
