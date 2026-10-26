package com.spiritform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Abilities;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * Серверна логіка входу у форму Привида та виходу з неї.
 */
public final class SpiritForm {

    private static final ResourceLocation FLIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "spirit_flight");

    private SpiritForm() {
    }

    /** Перезарядка входу у форму (після того, як Привид забрав здоров'я одержимого моба). */
    private static final Map<UUID, Long> FORM_COOLDOWN_UNTIL = new HashMap<>();

    public static void toggle(ServerPlayer player) {
        if (SpiritFormState.isActive(player)) {
            if (SpiritPossession.isPossessing(player)) {
                SpiritPossession.drainAndExit(player);
            } else {
                deactivate(player);
            }
        } else {
            activate(player);
        }
    }

    /** Запускає перезарядку форми Привида. */
    public static void startCooldown(ServerPlayer player) {
        long ticks = FentonBracelet.cooldown(player, Config.FORM_COOLDOWN_SECONDS.get() * 20L);
        FORM_COOLDOWN_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static void activate(ServerPlayer player) {
        activate(player, false);
    }

    /** ignoreCooldown = true для рятівного Інстинкту, який не може чекати. */
    public static void activate(ServerPlayer player, boolean ignoreCooldown) {
        SpiritData data = SpiritFormState.get(player);

        // З Косою Потойбіччя в руках Привидом бути не можна
        if (SpiritReaper.holdsScythe(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.scythe.no_form"), true);
            return;
        }

        if (!data.unlocked()) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.locked", Component.keybind("key.spirit_form.open_upgrades")),
                    true
            );
            return;
        }

        if (!ignoreCooldown) {
            long until = FORM_COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L);
            if (until > player.level().getGameTime()) {
                SpiritCooldowns.notifyTired(player, until);
                return;
            }
        }

        if (isStarving(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.exhausted"), true);
            return;
        }

        SpiritFormState.setActive(player, true);

        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null && !flight.hasModifier(FLIGHT_MODIFIER_ID)) {
            flight.addTransientModifier(new AttributeModifier(
                    FLIGHT_MODIFIER_ID,
                    1.0,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }

        Abilities abilities = player.getAbilities();
        abilities.setFlyingSpeed(SpiritBalance.flySpeed(data.movement()));
        abilities.flying = true;
        player.onUpdateAbilities();

        // Темрява при вході у форму
        if (SpiritBalance.darknessConstantInForm(data.darkness())) {
            player.addEffect(darkness(60));
        } else {
            int seconds = SpiritBalance.darknessInFormSeconds(data.darkness());
            if (seconds > 0) {
                player.addEffect(darkness(seconds * 20));
            }
        }

        SpiritReaper.onFormEntered(player);
        player.displayClientMessage(Component.translatable("message.spirit_form.activated"), true);
        playSound(player, 1.0F);
    }

    /** Гудіння Вітрюка при вході у форму та виході з неї (при виході нижче). */
    private static void playSound(ServerPlayer player, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_IDLE_GROUND, SoundSource.PLAYERS, 0.8F, pitch);
    }

    /** Чи надто голодний гравець для форми (0 одиниць голоду). У творчому режимі голод не діє. */
    public static boolean isStarving(ServerPlayer player) {
        return !player.getAbilities().instabuild && player.getFoodData().getFoodLevel() <= 0;
    }

    public static void deactivate(ServerPlayer player) {
        deactivate(player, "message.spirit_form.deactivated");
    }

    public static void deactivate(ServerPlayer player, String messageKey) {
        SpiritData data = SpiritFormState.get(player);
        leaveForm(player);

        // Темрява у формі людини
        player.removeEffect(MobEffects.DARKNESS);
        int seconds = SpiritBalance.darknessAfterSeconds(data.darkness());
        if (seconds > 0) {
            player.addEffect(darkness(seconds * 20));
        }

        player.displayClientMessage(Component.translatable(messageKey), true);
        playSound(player, 0.8F);
        SpiritReaper.onFormLeft(player);
    }

    /** Подовжує перезарядку форми щонайменше на вказану кількість тіків від зараз (Накидка Потойбіччя). */
    public static void extendCooldown(ServerPlayer player, long ticks) {
        long until = player.level().getGameTime() + ticks;
        FORM_COOLDOWN_UNTIL.merge(player.getUUID(), until, Math::max);
    }

    /** Тихий вихід з форми (вхід у світ, смерть): без ефектів і повідомлень. */
    public static void reset(ServerPlayer player) {
        if (SpiritFormState.isActive(player)) {
            leaveForm(player);
        }
    }

    /** Викликається після покупки рівня, щоб нова швидкість подіяла одразу. */
    public static void refreshStats(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        if (!data.active()) {
            return;
        }
        player.getAbilities().setFlyingSpeed(SpiritBalance.flySpeed(data.movement()));
        player.onUpdateAbilities();
    }

    public static MobEffectInstance nightVision(int ticks) {
        return new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 0, false, false, false);
    }

    public static MobEffectInstance darkness(int ticks) {
        return new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false, false);
    }

    private static void leaveForm(ServerPlayer player) {
        SpiritPossession.release(player);
        player.removeEffect(MobEffects.NIGHT_VISION);

        SpiritFormState.setActive(player, false);

        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            flight.removeModifier(FLIGHT_MODIFIER_ID);
        }

        Abilities abilities = player.getAbilities();
        if (!player.isCreative() && !player.isSpectator()) {
            abilities.flying = false;
        }
        abilities.setFlyingSpeed(SpiritBalance.DEFAULT_FLY_SPEED);
        player.onUpdateAbilities();
    }
}
