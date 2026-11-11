package com.spiritform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Перлина папороті: повністю відновлює здоров'я й голод. */
public class FernPearlItem extends Item {

    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(1).saturationModifier(0.0F).alwaysEdible().build();

    public FernPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) {
            entity.setHealth(entity.getMaxHealth());
            if (entity instanceof ServerPlayer player) {
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0F);
            }
        }
        return result;
    }
}
