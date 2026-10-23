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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Марення: на місці гравця лишається хмара диму (невидима подоба), на яку реагують моби, а сам гравець стає невидимим для мобів і може тікати.
 */
public final class SpiritDelirium {

    private record Decoy(ArmorStand stand, long until) {
    }

    private static final double ATTRACT_RADIUS = 20.0;

    private static final Map<UUID, Decoy> DECOYS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    private SpiritDelirium() {
    }

    /** Чи є ця сутність подобою (щоб інші здібності її не чіпали). */
    public static boolean isDecoy(Entity entity) {
        if (DECOYS.isEmpty()) {
            return false;
        }
        for (Decoy decoy : DECOYS.values()) {
            if (decoy.stand() == entity) {
                return true;
            }
        }
        return false;
    }

    public static void activate(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        if (!data.hasAbility(SpiritAbility.DELIRIUM)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.delirium.locked"), true);
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

        removeDecoy(id);

        ServerLevel level = player.serverLevel();
        ArmorStand stand = new ArmorStand(level, player.getX(), player.getY(), player.getZ());
        stand.setYRot(player.getYRot());
        stand.setYHeadRot(player.getYRot());
        // Подоба невидима: її видно лише як хмару диму, але моби бачать у ній ціль
        stand.setInvisible(true);
        stand.setNoBasePlate(true);

        level.addFreshEntity(stand);
        long lifetime = Config.DELIRIUM_DECOY_SECONDS.get() * 20L;
        DECOYS.put(id, new Decoy(stand, now + lifetime));
        COOLDOWN_UNTIL.put(id, now + FentonBracelet.cooldown(player, Config.ABILITY_COOLDOWN_SECONDS.get() * 20L));

        puff(level, stand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GHAST_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.5F);

        // Моби, що полювали на гравця, перемикаються на подобу
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0),
                m -> m.getTarget() == player)) {
            mob.setTarget(stand);
        }

        SpiritInvisibility.forceOn(player);
        player.displayClientMessage(Component.translatable("message.spirit_form.delirium.activated"), true);
    }

    /** Щотіку для гравця: час життя подоби та привабливість для мобів. */
    public static void tick(ServerPlayer player) {
        Decoy decoy = DECOYS.get(player.getUUID());
        if (decoy == null) {
            return;
        }

        ArmorStand stand = decoy.stand();
        long now = player.level().getGameTime();
        if (stand.isRemoved() || !stand.isAlive() || now >= decoy.until() || stand.level() != player.level()) {
            removeDecoy(player.getUUID());
            return;
        }

        ServerLevel level = player.serverLevel();
        // Хмара диму в зріст людини, поки діє здібність
        level.sendParticles(ParticleTypes.SMOKE, stand.getX(), stand.getY() + 0.9, stand.getZ(),
                6, 0.3, 0.8, 0.3, 0.01);

        if (now % 10 == 0) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, stand.getBoundingBox().inflate(ATTRACT_RADIUS),
                    m -> m instanceof Enemy)) {
                if (mob.getTarget() == null || mob.getTarget() == player) {
                    mob.setTarget(stand);
                }
            }
        }
    }

    private static void removeDecoy(UUID id) {
        Decoy decoy = DECOYS.remove(id);
        if (decoy == null) {
            return;
        }
        ArmorStand stand = decoy.stand();
        if (!stand.isRemoved() && stand.level() instanceof ServerLevel level) {
            puff(level, stand);
            stand.discard();
        }
    }

    public static void clear(ServerPlayer player) {
        removeDecoy(player.getUUID());
        COOLDOWN_UNTIL.remove(player.getUUID());
    }

    private static void puff(ServerLevel level, ArmorStand stand) {
        level.sendParticles(ParticleTypes.SMOKE, stand.getX(), stand.getY() + 0.9, stand.getZ(),
                30, 0.4, 0.8, 0.4, 0.04);
    }
}
