package com.spiritform;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

/**
 * Коса Потойбіччя. Шкода, швидкість атаки й міцність задаються в SpiritItems та тут (Tier).
 * Уся особлива поведінка (лікування, заряд, ПКМ, Жниво) у SpiritReaper; заряд зберігається в самому предметі.
 */
public class ScytheItem extends SwordItem {

    public ScytheItem(Item.Properties properties) {
        super(Tiers.IRON, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            SpiritReaper.onUse(serverPlayer, stack);
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            SpiritReaper.tickScythe(player, stack);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return SpiritReaper.active(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int max = Config.SCYTHE_MAX_CHARGE.get();
        int charge = Math.min(max, SpiritReaper.charge(stack));
        tooltip.add(Component.translatable("tooltip.spirit_form.scythe.charge",
                "■".repeat(charge) + "□".repeat(max - charge), charge, max));
        if (SpiritReaper.active(stack)) {
            tooltip.add(Component.translatable("tooltip.spirit_form.scythe.active"));
        } else if (context.level() != null && SpiritReaper.lockUntil(stack) > context.level().getGameTime()) {
            long seconds = (SpiritReaper.lockUntil(stack) - context.level().getGameTime() + 19) / 20;
            tooltip.add(Component.translatable("tooltip.spirit_form.scythe.locked", seconds));
        }
        tooltip.add(Component.translatable("tooltip.spirit_form.scythe.heal",
                Config.SCYTHE_HEAL_MIN_PERCENT.get(), Config.SCYTHE_HEAL_MAX_PERCENT.get()));
        tooltip.add(Component.translatable("tooltip.spirit_form.scythe.charge_how"));
        tooltip.add(Component.translatable("tooltip.spirit_form.scythe.freeze",
                Config.SCYTHE_FREEZE_RADIUS.get(), Config.SCYTHE_FREEZE_SECONDS.get(), Config.SCYTHE_POWER_MULTIPLIER.get().intValue()));
        tooltip.add(Component.translatable("tooltip.spirit_form.scythe.reaper"));
        tooltip.add(Component.translatable("tooltip.spirit_form.scythe.no_form"));
    }
}
