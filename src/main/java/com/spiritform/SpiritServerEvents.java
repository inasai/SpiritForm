package com.spiritform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Щотіку на сервері: витрата голоду та постійна Темрява. Також прибирає форму при вході й смерті
 * та скасовує будь-яку шкоду, поки гравець у формі.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public class SpiritServerEvents {

    private static final Map<UUID, Vec3> LAST_POSITIONS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SpiritInvisibility.isActive(player);

        SpiritData data = SpiritFormState.get(player);
        if (!data.active()) {
            LAST_POSITIONS.remove(player.getUUID());
            return;
        }

        // Голод дійшов до нуля: сили на форму немає
        if (SpiritForm.isStarving(player)) {
            SpiritForm.deactivate(player, "message.spirit_form.exhausted");
            return;
        }

        // Лава підпалює, а вода витрачає повітря, навіть якщо шкода скасована
        if (player.isOnFire()) {
            player.clearFire();
        }
        player.setAirSupply(player.getMaxAirSupply());

        // Нічне бачення: поновлюємо з запасом, щоб воно не мигало перед завершенням
        MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (nightVision == null || nightVision.getDuration() < 300) {
            player.addEffect(SpiritForm.nightVision(400));
        }

        // Голод залежить від пройденої відстані
        Vec3 now = player.position();
        Vec3 previous = LAST_POSITIONS.put(player.getUUID(), now);
        if (previous != null) {
            double distance = now.distanceTo(previous);
            if (distance > 0.0001 && distance < 10.0) {
                double rate = SpiritBalance.exhaustionPerBlock(data.hunger());
                player.causeFoodExhaustion((float) (distance * rate));
            }
        }

        // Постійна Темрява: поновлюємо щосекунди
        if (SpiritBalance.darknessConstantInForm(data.darkness()) && player.tickCount % 20 == 0) {
            player.addEffect(SpiritForm.darkness(60));
        }
    }

    /**
     * Привид не отримує жодної шкоди: від мобів, лави, води, вогню, падіння тощо.
     * Виняток лише для шкоди, що обходить будь-який захист (порожнеча світу, команда /kill),
     * щоб гравця не можна було застрягти у світі без виходу.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !SpiritFormState.isActive(player)) {
            return;
        }

        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpiritForm.reset(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpiritForm.reset(player);
            player.removeEffect(MobEffects.DARKNESS);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_POSITIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player)) {
            return;
        }

        if (SpiritInvisibility.isActive(player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
