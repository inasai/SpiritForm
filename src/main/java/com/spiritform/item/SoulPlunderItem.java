package com.spiritform.item;

import com.spiritform.SpiritFormMod;
import com.spiritform.registry.SpiritItems;
import com.spiritform.vault.SpiritAccessories;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/**
 * Дзеркало Душ (id soul_plunder), артефакт Шляху Темряви. Одягається як штани або кладеться в сховище.
 * Діє також у руці. Увесь отриманий досвід збільшується у п'ять разів; ефект один, скільки б предметів гравець не мав.
 * На персонажі його малює SoulMirrorLayer (у сховищі ховається "оком").
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public class SoulPlunderItem extends Item implements Equipable {

    public static final int MULTIPLIER = 5;

    public SoulPlunderItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.LEGS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    private static boolean is(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SpiritItems.SOUL_PLUNDER.get());
    }

    /** Діє в руці, у слоті штанів або в сховищі (навіть якщо його сховано "оком"). */
    public static boolean isActive(LivingEntity entity) {
        return is(entity.getItemInHand(InteractionHand.MAIN_HAND))
                || is(entity.getItemInHand(InteractionHand.OFF_HAND))
                || is(entity.getItemBySlot(EquipmentSlot.LEGS))
                || SpiritAccessories.isEquipped(entity, SpiritItems.SOUL_PLUNDER.get());
    }

    /** Чи малювати на персонажі: слот штанів завжди, сховище лише з відкритим "оком". */
    public static boolean shows(LivingEntity entity) {
        return is(entity.getItemBySlot(EquipmentSlot.LEGS))
                || SpiritAccessories.state(entity, SpiritItems.SOUL_PLUNDER.get()) == SpiritAccessories.State.VISIBLE;
    }

    /** Усі джерела досвіду (кулі, пляшки, команда /xp add) проходять через цю подію. Множимо лише додатний досвід. */
    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0 || !isActive(player)) {
            return;
        }
        long boosted = (long) event.getAmount() * MULTIPLIER;
        event.setAmount((int) Math.min(Integer.MAX_VALUE, boosted));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spirit_form.artifact.beyond").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.spirit_form.soul_plunder.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spirit_form.soul_plunder.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
