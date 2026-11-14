package com.spiritform.item;

import com.spiritform.registry.SpiritItems;
import com.spiritform.vault.SpiritAccessories;
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
 * Кайдани Межі, артефакт Шляху Темряви. Одягаються як чоботи або кладуться в сховище.
 * Поки вони одягнені, радіус дії здібностей удвічі більший (див. SpiritRange). Ефект один.
 * На персонажі їх малює ShacklesLayer (у сховищі ховаються "оком").
 */
public class ShacklesItem extends Item implements Equipable {

    public ShacklesItem(Properties properties) {
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
        return !stack.isEmpty() && stack.is(SpiritItems.SHACKLES.get());
    }

    /** Діють у слоті чобіт або в сховищі (навіть якщо їх сховано "оком"). */
    public static boolean isWorn(LivingEntity entity) {
        return is(entity.getItemBySlot(EquipmentSlot.FEET))
                || SpiritAccessories.isEquipped(entity, SpiritItems.SHACKLES.get());
    }

    /** Чи малювати на персонажі: слот чобіт завжди, сховище лише з відкритим "оком". */
    public static boolean shows(LivingEntity entity) {
        return is(entity.getItemBySlot(EquipmentSlot.FEET))
                || SpiritAccessories.state(entity, SpiritItems.SHACKLES.get()) == SpiritAccessories.State.VISIBLE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spirit_form.artifact.darkness").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.spirit_form.shackles.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spirit_form.shackles.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
