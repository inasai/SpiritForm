package com.spiritform;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Предмети мода й вкладка "Spirit Form" у творчому режимі. */
public final class SpiritItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpiritFormMod.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpiritFormMod.MODID);

    /** Стигмат Фантома (назва файлів лишилася fentons_bracelet, щоб не ламати твої текстури). */
    public static final DeferredItem<Item> FENTONS_BRACELET = ITEMS.registerItem(
            "fentons_bracelet", FentonBraceletItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> SOUL_CATCHER = ITEMS.registerItem(
            "soul_catcher",
            properties -> new SoulCatcherItem(SpiritBlocks.SOUL_CATCHER.get(), properties),
            new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> NECKLACE = ITEMS.registerItem(
            "necklace_of_the_beyond", NecklaceItem::new, new Item.Properties().stacksTo(1));

    /** Службовий предмет: його моделі з custom_model_data показують іконки досягнень. У вкладці його немає. */
    public static final DeferredItem<Item> ADVANCEMENT_ICON = ITEMS.registerItem(
            "advancement_icon", Item::new, new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register(
            "spirit_form",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.spirit_form"))
                    .icon(() -> new ItemStack(SOUL_CATCHER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(FENTONS_BRACELET.get());
                        output.accept(SOUL_CATCHER.get());
                        output.accept(NECKLACE.get());
                    })
                    .build());

    private SpiritItems() {
    }
}
