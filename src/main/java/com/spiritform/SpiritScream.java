package com.spiritform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Крик Привида: усі моби (крім босів) у радіусі встають на бік гравця на певний час. Вони не нападають на гравця
 * й не завдають йому шкоди, а ціллю обирають того, хто завдав шкоди гравцю (або кого вдарив гравець).
 * Мирні моби без вміння нападати лише перестають бути загрозою. Союзники живуть у пам'яті сервера:
 * після перезапуску світу вони знову стають собою.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritScream {

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    /** власник -> (моб -> тік, коли закінчується) */
    private static final Map<UUID, Map<UUID, Long>> ALLIES = new HashMap<>();

    /** Скільки тіків після удару пам'ятаємо, хто завдав шкоди гравцю. */
    private static final int REVENGE_TICKS = 200;

    private SpiritScream() {
    }

    public static void activate(ServerPlayer player) {
        if (!SpiritFormState.get(player).hasAbility(SpiritAbility.SCREAM)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.active.locked", SpiritAbility.SCREAM.displayName()), true);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        UUID id = player.getUUID();
        long now = level.getGameTime();
        long until = COOLDOWN_UNTIL.getOrDefault(id, 0L);
        if (until > now) {
            SpiritCooldowns.notifyTired(player, until);
            return;
        }

        double radius = Config.SCREAM_RADIUS.get();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                mob -> mob.isAlive() && mob.distanceToSqr(player) <= radius * radius
                        && !(mob instanceof EnderDragon) && !(mob instanceof WitherBoss)
                        && !SpiritPossession.isPossessed(mob));
        if (mobs.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.scream.failed"), true);
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

        long expiry = now + Config.SCREAM_DURATION_SECONDS.get() * 20L;
        Map<UUID, Long> allies = ALLIES.computeIfAbsent(id, key -> new HashMap<>());
        for (Mob mob : mobs) {
            allies.put(mob.getUUID(), expiry);
            if (mob.getTarget() != null) {
                mob.setTarget(null);
            }
            level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                    8, 0.3, 0.4, 0.3, 0.02);
        }

        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.8F, 1.6F);
        player.displayClientMessage(Component.translatable("message.spirit_form.scream.activated", mobs.size()), true);
    }

    /** Чи є моб союзником цього гравця. */
    public static boolean isAllyOf(Entity mob, Entity owner) {
        Map<UUID, Long> allies = ALLIES.get(owner.getUUID());
        return allies != null && allies.containsKey(mob.getUUID());
    }

    private static boolean isAnyAlly(Entity entity) {
        for (Map<UUID, Long> allies : ALLIES.values()) {
            if (allies.containsKey(entity.getUUID())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) {
            return;
        }
        Map<UUID, Long> allies = ALLIES.get(player.getUUID());
        if (allies == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        long now = level.getGameTime();
        LivingEntity victim = revengeTarget(player);

        Iterator<Map.Entry<UUID, Long>> it = allies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || entry.getValue() <= now) {
                it.remove();
                continue;
            }

            LivingEntity current = mob.getTarget();
            if (current == player || (current != null && isAnyAlly(current))) {
                mob.setTarget(null);
            }
            if (victim != null && victim != mob && !isAnyAlly(victim)) {
                mob.setTarget(victim);
            }
        }
        if (allies.isEmpty()) {
            ALLIES.remove(player.getUUID());
        }
    }

    /** Той, хто нещодавно завдав шкоди гравцю, а якщо таких немає, той, кого гравець нещодавно вдарив. */
    private static LivingEntity revengeTarget(ServerPlayer player) {
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker != player
                && player.tickCount - player.getLastHurtByMobTimestamp() <= REVENGE_TICKS) {
            return attacker;
        }
        LivingEntity target = player.getLastHurtMob();
        if (target != null && target.isAlive() && target != player
                && player.tickCount - player.getLastHurtMobTimestamp() <= REVENGE_TICKS) {
            return target;
        }
        return null;
    }

    /** Союзники не обирають власника ціллю. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && isAllyOf(event.getEntity(), target)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** Союзники не завдають шкоди власнику. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && isAllyOf(attacker, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        ALLIES.remove(id);
        COOLDOWN_UNTIL.remove(id);
    }
}
