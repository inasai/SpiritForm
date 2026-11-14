package com.spiritform.item;

import com.spiritform.form.SpiritFormState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Яблуко Духів. Ефекти залежать від форми гравця в момент, коли він доїв яблуко (а не коли почав):
 * людина отримує Висушування й Голод на 5 хвилин та Нудоту на 10 секунд, Привид отримує Поглинання й Силу на 3 хвилини.
 */
public class SpiritAppleItem extends Item {

    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(4).saturationModifier(0.3F).alwaysEdible().build();

    public SpiritAppleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (SpiritFormState.isActive(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 3600, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0));
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 6000, 0));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 6000, 0));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            }
        }
        return result;
    }
}
