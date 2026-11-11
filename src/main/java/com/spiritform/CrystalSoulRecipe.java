package com.spiritform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Кристалізована Душа: аметист + Ловець Душ із душею всередині (порожній не підходить).
 * Рецепт особливий, бо звичайний рецепт не вміє відрізнити повного ловця від порожнього.
 * Обидва предмети витрачаються, душа лишається в кристалі.
 */
public class CrystalSoulRecipe extends CustomRecipe {

    public CrystalSoulRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack catcher = find(input);
        if (catcher == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(SpiritItems.CRYSTAL_SOUL.get());
        CompoundTag soul = SoulCatcherItem.stored(catcher);
        if (soul != null && soul.contains("id")) {
            CrystalSoulItem.remember(result, soul.getString("id"));
        }
        return result;
    }

    /** Повертає повного ловця, якщо на столі рівно аметист і він; інакше null. */
    private static ItemStack find(CraftingInput input) {
        ItemStack catcher = null;
        int amethyst = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.AMETHYST_SHARD)) {
                amethyst++;
            } else if (stack.is(SpiritItems.SOUL_CATCHER.get()) && SoulCatcherItem.isFull(stack) && catcher == null) {
                catcher = stack;
            } else {
                return null;
            }
        }
        return amethyst == 1 ? catcher : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SpiritRecipes.CRYSTAL_SOUL.get();
    }
}
