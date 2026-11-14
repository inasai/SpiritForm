package com.spiritform.recipe;

import com.spiritform.registry.SpiritItems;
import com.spiritform.registry.SpiritRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Кристалізована Душа + будь-який пошкоджений предмет із міцністю: предмет відновлює міцність повністю,
 * кристал витрачається. Усе інше на столі (чи другий предмет) рецепт відкидає.
 */
public class CrystalRepairRecipe extends CustomRecipe {

    public CrystalRepairRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack target = find(input);
        if (target == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = target.copy();
        result.setCount(1);
        result.setDamageValue(0);
        return result;
    }

    /** Повертає предмет, який лагодять, якщо на столі рівно кристал і один пошкоджений предмет; інакше null. */
    private static ItemStack find(CraftingInput input) {
        int crystals = 0;
        ItemStack target = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(SpiritItems.CRYSTAL_SOUL.get())) {
                crystals++;
            } else if (target == null && stack.isDamageableItem() && stack.isDamaged()) {
                target = stack;
            } else {
                return null;
            }
        }
        return crystals == 1 ? target : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SpiritRecipes.CRYSTAL_REPAIR.get();
    }
}
