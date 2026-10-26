package com.spiritform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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

    /**
     * Гравці, для яких Інстинкт уже спрацював або які вже на межі смерті. Поки здоров'я не
     * піднялося вище порога, Інстинкт не спрацьовує вдруге: наступна смертельна шкода вбиває.
     */
    private static final Set<UUID> LOW_HEALTH = new HashSet<>();

    /** Здоров'я, з яким гравець лишається після рятівного від смерті Інстинкту: половина сердечка. */
    private static final float SURVIVE_HEALTH = 1.0F;

    /** Здоров'я, при якому спрацьовує Інстинкт Привида: 2 одиниці, тобто одне сердечко. */
    private static final float INSTINCT_HEALTH = 2.0F;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SpiritInvisibility.isActive(player);
        SpiritCooldowns.tick(player);
        SpiritTeleport.tickSmoke(player);
        SpiritDelirium.tick(player);
        SpiritAuras.tick(player);

        // Поки гравець у моба, його тіло не показуємо (ефекти можуть скинути прапорець)
        if (SpiritPossession.isPossessing(player)) {
            player.setInvisible(true);
        }

        SpiritData data = SpiritFormState.get(player);
        checkInstinct(player, data);
        data = SpiritFormState.get(player);

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
                if (!FentonBracelet.hasNecklace(player)) {
                    player.causeFoodExhaustion((float) (distance * rate));
                }
            }
        }

        // Постійна Темрява: поновлюємо щосекунди
        if (SpiritBalance.darknessConstantInForm(data.darkness()) && player.tickCount % 20 == 0) {
            player.addEffect(SpiritForm.darkness(60));
        }
    }

    /**
     * Інстинкт Привида: коли здоров'я падає до одного сердечка, форма вмикається сама,
     * але лише якщо в гравця є голод. Вимкнений вручну Привид знову не вмикається,
     * доки здоров'я не відновиться вище цього порога.
     */
    private static void checkInstinct(ServerPlayer player, SpiritData data) {
        boolean low = player.isAlive() && player.getHealth() <= INSTINCT_HEALTH;

        if (!low) {
            LOW_HEALTH.remove(player.getUUID()); // здоров'я відновилося: Інстинкт знову готовий
            return;
        }

        if (LOW_HEALTH.contains(player.getUUID())) {
            return; // уже спрацьовував
        }

        if (canTriggerInstinct(player, data)) {
            triggerInstinct(player);
        }
    }

    /** Гравець навмисно віддав здоров'я (заряд здібності): Інстинкт через це не спрацьовує. */
    public static void markLowHealth(ServerPlayer player) {
        if (player.getHealth() <= INSTINCT_HEALTH) {
            LOW_HEALTH.add(player.getUUID());
        }
    }

    private static boolean canTriggerInstinct(ServerPlayer player, SpiritData data) {
        return data.instinct() && data.unlocked() && !data.active() && !SpiritForm.isStarving(player)
                && (player.getAbilities().instabuild
                || player.getFoodData().getFoodLevel() >= FentonBracelet.cost(player, Config.INSTINCT_HUNGER_COST.get()));
    }

    /** Спрацювання Інстинкту: гравець лишається з половиною сердечка й стає Привидом. */
    private static void triggerInstinct(ServerPlayer player) {
        LOW_HEALTH.add(player.getUUID());
        player.setHealth(Math.min(player.getHealth(), SURVIVE_HEALTH));
        SpiritForm.activate(player, true);
        SpiritAdvancements.grant(player, "instinct");

        // Інстинкт коштує голоду, як і інші здібності
        if (SpiritFormState.isActive(player) && !player.getAbilities().instabuild) {
            int food = player.getFoodData().getFoodLevel();
            player.getFoodData().setFoodLevel(Math.max(0, food - FentonBracelet.cost(player, Config.INSTINCT_HUNGER_COST.get())));
        }
    }

    /**
     * Інстинкт Привида рятує від смертельного удару: шкода, яка вбила б гравця, скасовується,
     * а форма Привида вмикається замість смерті; гравцеві лишається половина сердечка.
     * Це спрацьовує лише раз, доки здоров'я не відновиться вище двох одиниць. Працює за тих самих умов, що й при одному
     * сердечку (Інстинкт увімкнено, є голод). Порожнеча світу та /kill вбивають, як і раніше.
     */
    @SubscribeEvent
    public static void onLethalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SpiritData data = SpiritFormState.get(player);
        if (!canTriggerInstinct(player, data) || LOW_HEALTH.contains(player.getUUID())) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (event.getNewDamage() < player.getHealth() + player.getAbsorptionAmount()) {
            return;
        }

        event.setNewDamage(0.0F);
        triggerInstinct(player);
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

        // З Косою Потойбіччя в руках невразливість Привида не діє
        if (SpiritReaper.holdsScythe(player)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpiritForm.reset(player);

            // Перезахід не дає нового рятунку: хто вже на межі смерті, Інстинкт не спрацьовує
            if (player.getHealth() <= INSTINCT_HEALTH) {
                LOW_HEALTH.add(player.getUUID());
            }
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
        if (event.getEntity() instanceof ServerPlayer player) {
            SpiritCooldowns.clear(player);
            SpiritDelirium.clear(player);
            SpiritImmunity.clear(player);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player)) {
            return;
        }

        // Погляд Привида: ніхто (крім босів) не обирає гравця ціллю
        if (SpiritAuras.isGazing(player) && SpiritAuras.affectedByGaze(event.getEntity())) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }

        // Моби не реагують на гравця, який сидить у мобі: вони бачать лише самого моба
        if (SpiritInvisibility.isActive(player) || SpiritPossession.isPossessing(player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
