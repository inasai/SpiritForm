package com.spiritform;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Доказ сили (серця Голоду, Холоду й Бурі разом). Сам нічого не робить: ним викликають Вісника Смерті (SpiritHeralds). */
public class ProofOfStrengthItem extends Item {

    public ProofOfStrengthItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.spirit_form.proof_of_strength.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
