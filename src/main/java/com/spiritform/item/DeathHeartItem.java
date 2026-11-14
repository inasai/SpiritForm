package com.spiritform.item;

import com.spiritform.ability.SpiritAbility;
import com.spiritform.data.SpiritData;
import com.spiritform.event.SpiritAdvancements;
import com.spiritform.form.SpiritFormState;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Серце Смерті (падає з Вісника Смерті). Затисніть ПКМ на 5 секунд, щоб поглинути його:
 * назавжди удвічі швидша перезарядка здібностей, удвічі менша витрата голоду (і поза формою Привида),
 * удвічі більше Уламків Душі та місць в Армії. Відкриває досягнення "Володар Потойбіччя".
 * Ефект один: друге серце гравцю не потрібне, воно не витрачається.
 */
public class DeathHeartItem extends Item {

    public static final int HOLD_TICKS = 100;

    public DeathHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (SpiritFormState.get(player).lord()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.spirit_form.lord.already"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    // Залежно від версії API метод має одну з двох форм: обидві повертають однакову тривалість
    public int getUseDuration(ItemStack stack) {
        return HOLD_TICKS;
    }

    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return HOLD_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) {
            return;
        }
        int used = HOLD_TICKS - remaining;
        if (used > 6 && used % 4 == 0) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
                    4, 0.4, 0.6, 0.4, 0.02);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return stack;
        }
        SpiritData data = SpiritFormState.get(player);
        if (data.lord()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.lord.already"), true);
            return stack;
        }
        SpiritFormState.set(player, data.withLord(true).withAbility(SpiritAbility.ENERGY, true));
        stack.shrink(1);
        ServerLevel server = (ServerLevel) level;
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
                60, 0.5, 0.9, 0.5, 0.08);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        player.displayClientMessage(Component.translatable("message.spirit_form.lord.absorbed"), false);
        player.displayClientMessage(Component.translatable("message.spirit_form.energy.unlocked"), false);
        SpiritAdvancements.grant(player, "overlord");
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.spirit_form.death_heart.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.spirit_form.death_heart.tooltip2").withStyle(ChatFormatting.DARK_AQUA));
    }
}
