package com.spiritform;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

    public static void toggle(ServerPlayer player) {
        if (SpiritFormState.isActive(player)) {
            deactivate(player);
        } else {
            activate(player);
        }
    }

    public static void activate(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);

        if (!data.unlocked()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.locked"), true);
            return;
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

        player.displayClientMessage(Component.translatable("message.spirit_form.activated"), true);
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

    public static MobEffectInstance darkness(int ticks) {
        return new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false, false);
    }

    private static void leaveForm(ServerPlayer player) {
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
