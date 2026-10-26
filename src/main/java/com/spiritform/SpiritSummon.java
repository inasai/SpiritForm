package com.spiritform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Виклик: телепортує до гравця істоту, на яку він дивиться (у межах видимості). Гравців викликати не можна. */
public final class SpiritSummon {

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    private SpiritSummon() {
    }

    public static void activate(ServerPlayer player) {
        if (!SpiritFormState.get(player).hasAbility(SpiritAbility.SUMMON)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.active.locked", SpiritAbility.SUMMON.displayName()), true);
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

        LivingEntity target = findTarget(player, level);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.summon.failed"), true);
            return;
        }

        Vec3 destination = findDestination(player, level, target);
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.summon.no_room"), true);
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

        puff(level, target.getX(), target.getY(), target.getZ());
        if (target.isPassenger()) {
            target.stopRiding();
        }
        target.moveTo(destination.x, destination.y, destination.z, target.getYRot(), target.getXRot());
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        puff(level, destination.x, destination.y, destination.z);

        player.displayClientMessage(Component.translatable("message.spirit_form.summon.activated"), true);
    }

    private static void puff(ServerLevel level, double x, double y, double z) {
        level.sendParticles(SpiritParticles.MINT, x, y + 0.9, z, 30, 0.3, 0.6, 0.3, 0.06);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    /**
     * Спершу істота, у яку гравець влучає поглядом. Якщо такої немає, обираємо ту, що найближча до лінії погляду
     * (допуск зростає з відстанню, щоб далеких і тонких істот, як скелети й кріпери, можна було вибрати).
     * Крізь блоки не діє.
     */
    private static LivingEntity findTarget(ServerPlayer player, ServerLevel level) {
        double range = Config.SUMMON_RANGE.get();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        double maxDistanceSqr = range * range;

        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() == HitResult.Type.BLOCK) {
            end = block.getLocation();
            maxDistanceSqr = eye.distanceToSqr(end);
        }

        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eye, end, box, SpiritSummon::canSummon, maxDistanceSqr);
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }

        LivingEntity best = null;
        double bestMiss = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(player, box.inflate(range * 0.1), SpiritSummon::canSummon)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            Vec3 center = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0);
            Vec3 toEntity = center.subtract(eye);
            double along = toEntity.dot(look);
            if (along <= 0.0 || along * along > maxDistanceSqr) {
                continue;
            }
            double miss = toEntity.subtract(look.scale(along)).length();
            double allowed = living.getBbWidth() * 0.5 + 0.5 + along * 0.05;
            if (miss > allowed || miss >= bestMiss) {
                continue;
            }
            BlockHitResult sight = level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player));
            if (sight.getType() == HitResult.Type.BLOCK) {
                continue;
            }
            best = living;
            bestMiss = miss;
        }
        return best;
    }

    private static boolean canSummon(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player)
                && entity.isAlive() && !entity.isSpectator()
                && !(entity instanceof Mob mob && SpiritPossession.isPossessed(mob));
    }

    /** Перед гравцем, а якщо там тісно, то на його місці. */
    private static Vec3 findDestination(ServerPlayer player, ServerLevel level, Entity target) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();

        for (double distance : new double[] {1.8, 1.0, 0.0}) {
            Vec3 pos = player.position().add(flat.scale(distance));
            AABB box = target.getBoundingBox().move(pos.x - target.getX(), pos.y - target.getY(), pos.z - target.getZ());
            if (level.noCollision(target, box)) {
                return pos;
            }
        }
        return null;
    }

    public static void clear(ServerPlayer player) {
        COOLDOWN_UNTIL.remove(player.getUUID());
    }
}
