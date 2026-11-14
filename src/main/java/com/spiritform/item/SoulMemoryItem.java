package com.spiritform.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Памʼять Душі, артефакт Потойбіччя. Уся логіка в SoulMemory: предмет лише носій опису й міцності. */
public class SoulMemoryItem extends Item {

    public SoulMemoryItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spirit_form.artifact.beyond").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.spirit_form.soul_memory.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spirit_form.soul_memory.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
