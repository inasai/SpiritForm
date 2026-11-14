package com.spiritform.event;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.ability.SpiritAuras;
import com.spiritform.ability.SpiritCooldowns;
import com.spiritform.ability.SpiritDelirium;
import com.spiritform.ability.SpiritDisguise;
import com.spiritform.ability.SpiritImmunity;
import com.spiritform.ability.SpiritInvisibility;
import com.spiritform.ability.SpiritPossession;
import com.spiritform.ability.SpiritTeleport;
import com.spiritform.data.SpiritData;
import com.spiritform.entity.Perelisnyk;
import com.spiritform.entity.Polovyk;
import com.spiritform.form.SpiritBalance;
import com.spiritform.form.SpiritExistence;
import com.spiritform.form.SpiritForm;
import com.spiritform.form.SpiritFormState;
import com.spiritform.herald.DeathHerald;
import com.spiritform.item.FentonBracelet;
import com.spiritform.item.SpiritReaper;
import com.spiritform.registry.SpiritGameRules;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Щотіку на сервері: витрата голоду та постійна Темрява. Також прибирає форму при вході й смерті
 * та скасовує будь-яку шкоду, поки гравець у формі.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public class SpiritServerEvents {

    private static final Map<UUID, Vec3> LAST_POSITIONS = new HashMap<>();

    /** Для кожного гравця: {чи тримав Shift минулого тіку (0/1), тік останнього натискання}. */
    private static final Map<UUID, long[]> SNEAK_STATE = new HashMap<>();
    private static final long DOUBLE_TAP_TICKS = 7;

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

        syncFormMode(player);
        SpiritInvisibility.isActive(player);
        SpiritCooldowns.tick(player);
        SpiritTeleport.tickSmoke(player);
        SpiritDelirium.tick(player);
        SpiritDisguise.tick(player);
        SpiritAuras.tick(player);
        SpiritExistence.tick(player);

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

        tickPhaseToggle(player, data);
        data = SpiritFormState.get(player);

        // Голод дійшов до нуля: сили на форму немає (Привид «Незеру» лишається Привидом)
        if (SpiritForm.isStarving(player) && !SpiritExistence.isNether()) {
            SpiritForm.deactivate(player, "message.spirit_form.exhausted");
            return;
        }

        // Лава підпалює, а вода витрачає повітря, навіть якщо шкода скасована
        if (data.invulnerable() && !SpiritExistence.isBeyond()) {
            if (player.isOnFire()) {
                player.clearFire();
            }
            player.setAirSupply(player.getMaxAirSupply());
        }

        // Нічне бачення не зілля: його дає клієнтський міксин LightTextureMixin, тож іконки немає.
        // Лишок старого ефекту (з попередніх версій) прибираємо
        MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (nightVision != null && !nightVision.isVisible() && !nightVision.showIcon()) {
            player.removeEffect(MobEffects.NIGHT_VISION);
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
     * Двічі швидко натиснути присідання (Shift) у формі: вмикає або вимикає проходження крізь блоки (з 2 рівня форми).
     * Подвійне натискання Пробілу вмикає польот сама гра, бо форма дає гравцеві творчий політ.
     */
    private static void tickPhaseToggle(ServerPlayer player, SpiritData data) {
        long[] state = SNEAK_STATE.computeIfAbsent(player.getUUID(), id -> new long[] {0, -100});
        boolean sneaking = player.isShiftKeyDown();
        long now = player.level().getGameTime();

        if (sneaking && state[0] == 0 && !SpiritPossession.isPossessing(player)) {
            if (data.canPhase() && now - state[1] <= DOUBLE_TAP_TICKS) {
                boolean enable = !data.phasing();
                SpiritFormState.set(player, data.withPhasing(enable));
                player.displayClientMessage(Component.translatable(
                        enable ? "message.spirit_form.phase.on" : "message.spirit_form.phase.off"), true);
                state[1] = -100;
            } else {
                state[1] = now;
            }
        }
        state[0] = sneaking ? 1 : 0;
    }

    /**
     * Режим форми (ігрове правило spiritFormOverworldMode) переписується в дані гравця, бо клієнт не бачить правил світу.
     * Перевіряється раз на секунду; якщо режим змінився, форма одразу отримує або втрачає політ.
     */
    private static void syncFormMode(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        boolean full = player.serverLevel().getGameRules().getBoolean(SpiritGameRules.OVERWORLD_MODE);
        SpiritData data = SpiritFormState.get(player);
        if (data.fullForm() != full) {
            data = data.withFullForm(full);
            SpiritFormState.set(player, data);
            SpiritForm.refreshStats(player);
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
                && !SpiritExistence.isNether()
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
        // Режим Nether: Привид нікому не шкодить своїми руками (удари одержимого моба не рахуються)
        if (SpiritExistence.isNether() && event.getSource().getEntity() instanceof Player attacker
                && SpiritFormState.isActive(attacker)) {
            event.setCanceled(true);
            return;
        }

        if (!(event.getEntity() instanceof Player player) || !SpiritFormState.isActive(player)) {
            return;
        }

        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        // Режим Beyond: Привид отримує шкоду, як звичайний гравець
        if (SpiritExistence.isBeyond()) {
            return;
        }

        // Режим Nether: Привид не отримує жодної шкоди
        if (SpiritExistence.isNether()) {
            event.setCanceled(true);
            return;
        }

        // Поки невразливість форми не розвинена (рівень 4), Привид захищений лише від падіння
        if (!SpiritFormState.get(player).invulnerable()) {
            if (event.getSource().is(DamageTypeTags.IS_FALL)) {
                event.setCanceled(true);
            }
            return;
        }

        // Вісник Смерті ігнорує невразливість Привида, але лише своїми ударами (інші моби такого права не мають)
        if (event.getSource().getEntity() instanceof DeathHerald
                || event.getSource().getDirectEntity() instanceof DeathHerald) {
            return;
        }

        // Перелісник б'є й Привида, не вимикаючи форму
        if (event.getSource().getEntity() instanceof Perelisnyk
                || event.getSource().getDirectEntity() instanceof Perelisnyk) {
            return;
        }

        // Польовик б'є й Привида: його удари проходять крізь невразливість форми
        if (event.getSource().getEntity() instanceof Polovyk
                || event.getSource().getDirectEntity() instanceof Polovyk) {
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
            SpiritExistence.onLogin(player);

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
            if (!event.isEndConquered()) {
                SpiritExistence.onRespawn(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_POSITIONS.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            SpiritCooldowns.clear(player);
            SpiritExistence.clear(player);
            SpiritDelirium.clear(player);
            SpiritDisguise.clear(player);
            SpiritImmunity.clear(player);
            SNEAK_STATE.remove(player.getUUID());
        }
    }

    /** Режим Nether: Привид не може атакувати та добувати блоки. */
    @SubscribeEvent
    public static void onGhostAttack(AttackEntityEvent event) {
        if (SpiritExistence.isNether() && SpiritFormState.isActive(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGhostLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (SpiritExistence.isNether() && SpiritFormState.isActive(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGhostBreak(BlockEvent.BreakEvent event) {
        if (SpiritExistence.isNether() && SpiritFormState.isActive(event.getPlayer())) {
            event.setCanceled(true);
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
