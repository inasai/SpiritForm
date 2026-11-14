package com.spiritform.registry;

import com.spiritform.SpiritFormMod;
import com.spiritform.herald.HeraldType;
import com.spiritform.item.CrystalSoulItem;
import com.spiritform.item.DeathHeartItem;
import com.spiritform.item.FentonBraceletItem;
import com.spiritform.item.FernPearlItem;
import com.spiritform.item.GlovesOfTheLivingItem;
import com.spiritform.item.HeraldHeartItem;
import com.spiritform.item.NecklaceItem;
import com.spiritform.item.ProofOfStrengthItem;
import com.spiritform.item.ScytheItem;
import com.spiritform.item.ShacklesItem;
import com.spiritform.item.SoulCatcherItem;
import com.spiritform.item.SoulMemoryItem;
import com.spiritform.item.SoulPlunderItem;
import com.spiritform.item.SoulPullItem;
import com.spiritform.item.SpiritAppleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
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

    public static final DeferredItem<Item> GLOVES_OF_THE_LIVING = ITEMS.registerItem(
            "gloves_of_the_living", GlovesOfTheLivingItem::new, new Item.Properties().stacksTo(1));

    /** Коса: шкода 3 + матеріал, швидкість атаки -2.4 (змініть числа нижче на свої). */
    public static final DeferredItem<Item> SCYTHE = ITEMS.registerItem(
            "scythe", ScytheItem::new,
            new Item.Properties().stacksTo(1)
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F)));

    /** Кристалізована Душа: матеріал для рецептів. Робиться з аметисту й Ловця Душ із душею (CrystalSoulRecipe). */
    public static final DeferredItem<Item> CRYSTAL_SOUL = ITEMS.registerItem(
            "crystal_soul", CrystalSoulItem::new, new Item.Properties());

    /** Серця Вісників: звичайні предмети. ПКМ у повітрі: шкала переслідування мінус 100. */
    public static final DeferredItem<Item> STORM_HEART = ITEMS.registerItem(
            "storm_heart", properties -> new HeraldHeartItem(HeraldType.STORM, properties),
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> HUNGER_HEART = ITEMS.registerItem(
            "hunger_heart", properties -> new HeraldHeartItem(HeraldType.HUNGER, properties),
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> COLD_HEART = ITEMS.registerItem(
            "cold_heart", properties -> new HeraldHeartItem(HeraldType.COLD, properties),
            new Item.Properties().stacksTo(16));

    /** Серце Смерті: з трьох різних сердець; викликає Вісника Смерті на блоці Відлуння Потойбіччя. */
    public static final DeferredItem<Item> DEATH_HEART = ITEMS.registerItem(
            "death_heart", DeathHeartItem::new,
            new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC));

    /** Доказ сили: серця Голоду, Холоду й Бурі разом. Викликає Вісника Смерті на блоці Відлуння Потойбіччя. */
    public static final DeferredItem<Item> PROOF_OF_STRENGTH = ITEMS.registerItem(
            "proof_of_strength", ProofOfStrengthItem::new,
            new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC));

    /** Кайдани Межі: артефакт Шляху Темряви, радіус дії здібностей х2. Чоботи або сховище. */
    public static final DeferredItem<Item> SHACKLES = ITEMS.registerItem(
            "shackles_of_the_edge", ShacklesItem::new,
            new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));

    /** Дзеркало Душ (id soul_plunder): артефакт Шляху Темряви, досвід х5. Штани або сховище. */
    public static final DeferredItem<Item> SOUL_PLUNDER = ITEMS.registerItem(
            "soul_plunder", SoulPlunderItem::new,
            new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));

    /** Тяжіння Душі: артефакт Потойбіччя, тягне предмети й досвід у радіусі 16. Чоботи або сховище. */
    public static final DeferredItem<Item> SOUL_PULL = ITEMS.registerItem(
            "soul_pull", SoulPullItem::new,
            new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));

    /** Памʼять Душі: артефакт Потойбіччя, лагодить броню й предмети в руках; має міцність. Рука або сховище. */
    public static final DeferredItem<Item> SOUL_MEMORY = ITEMS.registerItem(
            "soul_memory", SoulMemoryItem::new,
            new Item.Properties().durability(1500).rarity(net.minecraft.world.item.Rarity.RARE));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> MAVKA_FLOWER =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.MAVKA_FLOWER, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> POLUDNYTSIA =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.POLUDNYTSIA, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> RICHNYTSIA =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.RICHNYTSIA, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SPIRIT_LEAVES =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.SPIRIT_LEAVES, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GLOW_FERN =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.GLOW_FERN, new Item.Properties());

    public static final DeferredItem<net.minecraft.world.item.BlockItem> PHOENIX_ASH_ITEM =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.PHOENIX_ASH, new Item.Properties());
    public static final DeferredItem<Item> SPIRIT_APPLE = ITEMS.registerItem(
            "spirit_apple", SpiritAppleItem::new, new Item.Properties().food(SpiritAppleItem.FOOD));
    public static final DeferredItem<Item> FERN_PEARL = ITEMS.registerItem(
            "fern_pearl", FernPearlItem::new, new Item.Properties().food(FernPearlItem.FOOD));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> STORM_HEAD =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.STORM_HEAD, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> HUNGER_HEAD =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.HUNGER_HEAD, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> COLD_HEAD =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.COLD_HEAD, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> DEATH_HEAD =
            ITEMS.registerSimpleBlockItem(SpiritBlocks.DEATH_HEAD, new Item.Properties());

    public static final DeferredItem<Item> SPIRIT_HELMET = armor("spirit_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<Item> SPIRIT_CHESTPLATE = armor("spirit_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<Item> SPIRIT_LEGGINGS = armor("spirit_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<Item> SPIRIT_BOOTS = armor("spirit_boots", ArmorItem.Type.BOOTS);

    private static DeferredItem<Item> armor(String name, ArmorItem.Type type) {
        return ITEMS.registerItem(name,
                properties -> new ArmorItem(SpiritArmorMaterials.SPIRIT, type, properties),
                new Item.Properties().durability(type.getDurability(SpiritArmorMaterials.DURABILITY_MULTIPLIER)));
    }

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
                        output.accept(GLOVES_OF_THE_LIVING.get());
                        output.accept(SCYTHE.get());
                        output.accept(CRYSTAL_SOUL.get());
                        output.accept(STORM_HEART.get());
                        output.accept(HUNGER_HEART.get());
                        output.accept(COLD_HEART.get());
                        output.accept(DEATH_HEART.get());
                        output.accept(PROOF_OF_STRENGTH.get());
                        output.accept(SOUL_PLUNDER.get());
                        output.accept(SHACKLES.get());
                        output.accept(SOUL_PULL.get());
                        output.accept(SOUL_MEMORY.get());
                        output.accept(MAVKA_FLOWER.get());
                        output.accept(POLUDNYTSIA.get());
                        output.accept(RICHNYTSIA.get());
                        output.accept(SPIRIT_LEAVES.get());
                        output.accept(SPIRIT_APPLE.get());
                        output.accept(GLOW_FERN.get());
                        output.accept(FERN_PEARL.get());
                        output.accept(STORM_HEAD.get());
                        output.accept(HUNGER_HEAD.get());
                        output.accept(COLD_HEAD.get());
                        output.accept(DEATH_HEAD.get());
                        output.accept(SPIRIT_HELMET.get());
                        output.accept(SPIRIT_CHESTPLATE.get());
                        output.accept(SPIRIT_LEGGINGS.get());
                        output.accept(SPIRIT_BOOTS.get());
                        output.accept(SpiritCreatures.TEMPLATE_CREATURE_EGG.get());
                        output.accept(SpiritCreatures.MAVKA_EGG.get());
                        output.accept(SpiritCreatures.CHUHAISTER_EGG.get());
                        output.accept(SpiritCreatures.HUKHA_EGG.get());
                        output.accept(SpiritCreatures.BISYK_EGG.get());
                        output.accept(SpiritCreatures.SKARBNYK_EGG.get());
                        output.accept(SpiritCreatures.KLUNNYK_EGG.get());
                        output.accept(SpiritCreatures.BAHATKO_EGG.get());
                        output.accept(SpiritCreatures.BAHATKO_EMBER.get());
                        output.accept(SpiritCreatures.PHOENIX_EGG.get());
                        output.accept(SpiritCreatures.PHOENIX_FEATHER.get());
                        output.accept(PHOENIX_ASH_ITEM.get());
                        output.accept(SpiritCreatures.ZLYDENY_EGG.get());
                        output.accept(SpiritCreatures.VODIANYK_EGG.get());
                        output.accept(SpiritCreatures.VODIANYTSIA_EGG.get());
                        output.accept(SpiritCreatures.POVITRULIA_EGG.get());
                        output.accept(SpiritCreatures.PERELISNYK_EGG.get());
                        output.accept(SpiritCreatures.NICHNYTSIA_EGG.get());
                        output.accept(SpiritCreatures.POTERCHA_EGG.get());
                        output.accept(SpiritCreatures.POLOVYK_EGG.get());
                        output.accept(SpiritCreatures.VOVKULAKA_EGG.get());
                        output.accept(SpiritCreatures.CHORT_EGG.get());
                        output.accept(SpiritCreatures.LISOVYK_EGG.get());
                    })
                    .build());

    private SpiritItems() {
    }
}
