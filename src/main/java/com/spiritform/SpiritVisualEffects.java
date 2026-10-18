package com.spiritform;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(
        modid = SpiritFormMod.MODID,
        value = Dist.CLIENT
)
public class SpiritVisualEffects {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Лише клієнтська сторона; дим бачать усі гравці навколо привида
        if (!player.level().isClientSide()) {
            return;
        }

        if (!SpiritFormState.isActive(player)) {
            return;
        }

        if (player.tickCount % 5 != 0) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            double x = player.getX()
                    + (player.getRandom().nextDouble() - 0.5) * 0.8;
            double y = player.getY()
                    + player.getRandom().nextDouble() * 1.8;
            double z = player.getZ()
                    + (player.getRandom().nextDouble() - 0.5) * 0.8;

            player.level().addParticle(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    0.0, 0.015, 0.0
            );
        }
    }
}
