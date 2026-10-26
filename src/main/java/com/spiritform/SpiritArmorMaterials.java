package com.spiritform;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Матеріал обладунків Привида. Усі характеристики (захист, міцність, зачарування, стійкість) змінюються тут.
 * Текстури одягненого обладунка кладуться у assets/spirit_form/textures/models/armor/spirit_layer_1.png
 * (шолом, нагрудник, чоботи) та spirit_layer_2.png (штани).
 */
public final class SpiritArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, SpiritFormMod.MODID);

    /** Множник міцності: міцність деталі = базове значення деталі * цей множник (залізо 15, алмаз 33, незерит 37). */
    public static final int DURABILITY_MULTIPLIER = 15;

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SPIRIT = ARMOR_MATERIALS.register("spirit", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 2);
        defense.put(ArmorItem.Type.LEGGINGS, 5);
        defense.put(ArmorItem.Type.CHESTPLATE, 6);
        defense.put(ArmorItem.Type.HELMET, 2);
        defense.put(ArmorItem.Type.BODY, 5);
        return new ArmorMaterial(
                defense,
                9, // зачаровуваність
                SoundEvents.ARMOR_EQUIP_IRON, // звук надягання
                () -> Ingredient.EMPTY, // чим лагодиться (порожньо: ніяк)
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "spirit"))),
                0.0F, // міцність броні (toughness)
                0.0F // стійкість до відкидання
        );
    });

    private SpiritArmorMaterials() {
    }

    public static Holder<ArmorMaterial> material() {
        return SPIRIT;
    }
}
