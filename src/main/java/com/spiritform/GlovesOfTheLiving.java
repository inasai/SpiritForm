package com.spiritform;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Рукавички Живих: прибирають ванільне сповільнення копання в повітрі (там швидкість ділиться на 5).
 * Діють, лише коли гравець не стоїть на землі (політ), а рукавички в руці або в сховищі.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class GlovesOfTheLiving {

    /** Те саме число, що в ванільному Player.getDestroySpeed. */
    private static final float AIR_PENALTY = 5.0F;

    private GlovesOfTheLiving() {
    }

    private static boolean isGloves(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SpiritItems.GLOVES_OF_THE_LIVING.get());
    }

    public static boolean isWearing(LivingEntity entity) {
        return isGloves(entity.getItemInHand(InteractionHand.MAIN_HAND))
                || isGloves(entity.getItemInHand(InteractionHand.OFF_HAND))
                || SpiritAccessories.isEquipped(entity, SpiritItems.GLOVES_OF_THE_LIVING.get());
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.onGround() || !isWearing(player)) {
            return;
        }
        event.setNewSpeed(event.getNewSpeed() * AIR_PENALTY);
    }
}
