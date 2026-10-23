package com.spiritform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Імунітет Привида: знімає з гравця всі ефекти, і позитивні, і негативні. */
public final class SpiritImmunity {

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    private SpiritImmunity() {
    }

    public static void activate(ServerPlayer player) {
        if (!SpiritFormState.get(player).hasAbility(SpiritAbility.IMMUNITY)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.active.locked", SpiritAbility.IMMUNITY.displayName()),
                    true);
            return;
        }

        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        long until = COOLDOWN_UNTIL.getOrDefault(id, 0L);
        if (until > now) {
            SpiritCooldowns.notifyTired(player, until);
            return;
        }

        int cost = FentonBracelet.cost(player, Config.ABILITY_HUNGER_COST.get());
        int food = player.getFoodData().getFoodLevel();
        if (food < cost) {
            player.displayClientMessage(Component.translatable("message.spirit_form.ability.hungry"), true);
            return;
        }
        player.getFoodData().setFoodLevel(food - cost);
        COOLDOWN_UNTIL.put(id, now + FentonBracelet.cooldown(player, Config.ABILITY_COOLDOWN_SECONDS.get() * 20L));

        player.removeAllEffects();

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                    24, 0.4, 0.8, 0.4, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        player.displayClientMessage(Component.translatable("message.spirit_form.immunity.activated"), true);
    }

    public static void clear(ServerPlayer player) {
        COOLDOWN_UNTIL.remove(player.getUUID());
    }
}
