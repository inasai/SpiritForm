package com.spiritform;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Тяжіння Душі, артефакт Потойбіччя. Одягається як чоботи або кладеться в сховище.
 * Поки воно одягнене, предмети й досвід у радіусі з конфігу (16 блоків) летять до гравця (див. SoulPull).
 */
public class SoulPullItem extends Item implements Equipable {

    public SoulPullItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.FEET;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    private static boolean is(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SpiritItems.SOUL_PULL.get());
    }

    /** Діє у слоті чобіт або в сховищі (навіть якщо його сховано "оком"). */
    public static boolean isWorn(LivingEntity entity) {
        return is(entity.getItemBySlot(EquipmentSlot.FEET))
                || SpiritAccessories.isEquipped(entity, SpiritItems.SOUL_PULL.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spirit_form.artifact.beyond").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.spirit_form.soul_pull.1", Config.SOUL_PULL_RADIUS.get())
                .withStyle(ChatFormatting.GRAY));
    }
}
