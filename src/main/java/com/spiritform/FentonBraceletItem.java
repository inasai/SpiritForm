package com.spiritform;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Браслет Фентона. Уся логіка в FentonBracelet: предмет лише носій опису. */
public class FentonBraceletItem extends Item {

    public FentonBraceletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spirit_form.artifact.darkness").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.spirit_form.bracelet.1").withStyle(ChatFormatting.GRAY));
    }
}
