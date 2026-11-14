package com.spiritform.item;

import com.spiritform.herald.HeraldType;
import com.spiritform.herald.SpiritHeralds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Серце Вісника (Бурі, Голоду або Холоду). Звичайний предмет: його можна носити, кидати й губити.
 * ПКМ у повітрі з'їдає серце й зменшує шкалу переслідування рівно на 100 пунктів.
 */
public class HeraldHeartItem extends Item {

    private final HeraldType kind;

    public HeraldHeartItem(HeraldType kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        // Серце зникає завжди, навіть у творчому режимі (там consume() нічого б не забирав)
        stack.shrink(1);
        SpiritHeralds.onHeartUsed(serverPlayer, kind);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.spirit_form.herald_heart.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
