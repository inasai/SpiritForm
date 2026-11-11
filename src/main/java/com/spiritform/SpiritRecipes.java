package com.spiritform;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Особливі рецепти мода (data/spirit_form/recipe/crystal_soul.json посилається на них за типом). */
public final class SpiritRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, SpiritFormMod.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CrystalSoulRecipe>> CRYSTAL_SOUL =
            SERIALIZERS.register("crystal_soul", () -> new SimpleCraftingRecipeSerializer<>(CrystalSoulRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CrystalRepairRecipe>> CRYSTAL_REPAIR =
            SERIALIZERS.register("crystal_repair", () -> new SimpleCraftingRecipeSerializer<>(CrystalRepairRecipe::new));

    private SpiritRecipes() {
    }
}
