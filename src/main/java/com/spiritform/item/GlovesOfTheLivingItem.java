package com.spiritform.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Рукавички Живих. Уся логіка в GlovesOfTheLiving: предмет лише носій опису. */
public class GlovesOfTheLivingItem extends Item {

    public GlovesOfTheLivingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spirit_form.artifact.beyond").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.spirit_form.gloves.1").withStyle(ChatFormatting.GRAY));
    }
}
