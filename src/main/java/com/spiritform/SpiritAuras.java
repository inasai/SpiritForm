package com.spiritform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Пасивні здібності, що працюють, поки їх увімкнено: Крижаний Доторк і Присутність.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritAuras {

    /** Лава, яку Крижаний Доторк перетворив на магматичні блоки, щоб повернути її назад. */
    private static final class Cooled {
        final ResourceKey<Level> dimension;
        final Set<BlockPos> blocks = new HashSet<>();

        Cooled(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }

    private static final Map<UUID, Cooled> COOLED = new HashMap<>();

    private SpiritAuras() {
    }

    public static void tick(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        boolean icy = data.icy() && data.hasAbility(SpiritAbility.ICY_TOUCH);
        boolean presence = data.presence() && data.hasAbility(SpiritAbility.PRESENCE);
        boolean gaze = data.gaze() && data.hasAbility(SpiritAbility.GAZE);
        if (!icy) {
            restoreAll(player.getUUID(), player.server);
        }
        if (!icy && !presence && !gaze) {
            return;
        }

        ServerLevel level = player.serverLevel();
        int t = player.tickCount;

        if (t % 2 == 0) {
            double x = player.getX();
            double y = player.getY() + 0.9;
            double z = player.getZ();
            if (icy) {
                // Частинки багаття душ
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.35, 0.7, 0.35, 0.01);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, 0.3, 0.6, 0.3, 0.02);
            }
            if (gaze) {
                // Частинки Погляду: світлий дим і іскри
                level.sendParticles(ParticleTypes.END_ROD, x, y + 0.3, z, 1, 0.3, 0.5, 0.3, 0.01);
                level.sendParticles(ParticleTypes.SOUL, x, y, z, 1, 0.35, 0.6, 0.35, 0.01);
            }
            if (presence) {
                // Частинки звичайного багаття
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.35, 0.7, 0.35, 0.01);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2, z, 1, 0.3, 0.6, 0.3, 0.02);
            }
        }

        if (icy) {
            if (t % 4 == 0) {
                freezeWater(player, level);
                coolLava(player, level);
            }
            if (t % 5 == 0 && data.active()) {
                for (LivingEntity entity : level.getEntitiesOfClass(
                        LivingEntity.class, player.getBoundingBox().inflate(0.15),
                        e -> e != player && e instanceof Mob && !SpiritDelirium.isDecoy(e))) {
                    freeze(entity);
                }
            }
        }

        if (gaze && t % 5 == 0) {
            gaze(player, level);
        } else if (presence && !gaze && t % 10 == 0) {
            scare(player, level);
        }
    }

    /** Вода навколо гравця перетворюється на крижаний шар, що згодом тане (як при Підводній ходьбі). */
    private static void freezeWater(ServerPlayer player, ServerLevel level) {
        int radius = Config.ICY_RADIUS.get();
        BlockPos center = player.blockPosition();
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -4, -radius), center.offset(radius, 0, radius))) {
            if (!pos.closerToCenterThan(player.position(), radius + 0.5)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) {
                continue;
            }
            if (!level.getBlockState(pos.above()).isAir()) {
                continue;
            }

            BlockPos fixed = pos.immutable();
            level.setBlockAndUpdate(fixed, ice);
            level.scheduleTick(fixed, Blocks.FROSTED_ICE, Mth.nextInt(player.getRandom(), 60, 120));
        }
    }

    /** Лава в радіусі дії стає магматичним блоком, а за межами радіуса знову лавою. */
    private static void coolLava(ServerPlayer player, ServerLevel level) {
        UUID id = player.getUUID();
        Cooled cooled = COOLED.get(id);
        if (cooled != null && cooled.dimension != level.dimension()) {
            restoreAll(id, player.server);
            cooled = null;
        }

        int radius = Config.ICY_RADIUS.get();
        double reach = radius + 0.5;

        // Спершу повертаємо лаву, що опинилася поза зоною дії
        if (cooled != null) {
            Iterator<BlockPos> iterator = cooled.blocks.iterator();
            while (iterator.hasNext()) {
                BlockPos pos = iterator.next();
                if (pos.closerToCenterThan(player.position(), reach)) {
                    continue;
                }
                if (!level.hasChunkAt(pos)) {
                    continue; // чанк вивантажено: повернемо, коли він завантажиться
                }
                if (level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) {
                    level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                }
                iterator.remove();
            }
        }

        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -4, -radius), center.offset(radius, 1, radius))) {
            if (!pos.closerToCenterThan(player.position(), reach)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.LAVA) || !state.getFluidState().isSource()) {
                continue;
            }

            BlockPos fixed = pos.immutable();
            level.setBlockAndUpdate(fixed, Blocks.MAGMA_BLOCK.defaultBlockState());
            if (cooled == null) {
                cooled = new Cooled(level.dimension());
                COOLED.put(id, cooled);
            }
            cooled.blocks.add(fixed);
        }
    }

    /** Повертає всю лаву цього гравця (здібність вимкнено, гравець вийшов або сервер зупиняється). */
    private static void restoreAll(UUID id, MinecraftServer server) {
        Cooled cooled = COOLED.remove(id);
        if (cooled == null) {
            return;
        }
        ServerLevel level = server.getLevel(cooled.dimension);
        if (level == null) {
            return;
        }
        for (BlockPos pos : cooled.blocks) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) {
                level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreAll(player.getUUID(), player.server);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreAll(player.getUUID(), player.server);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (UUID id : new java.util.ArrayList<>(COOLED.keySet())) {
            restoreAll(id, event.getServer());
        }
    }

    /** Магматичний блок, який зробив Крижаний Доторк, не обпікає гравця з увімкненою здібністю. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypes.HOT_FLOOR)) {
            return;
        }
        SpiritData data = SpiritFormState.get(player);
        if (data.icy() && data.hasAbility(SpiritAbility.ICY_TOUCH)) {
            event.setCanceled(true);
        }
    }

    private static void freeze(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze() + 60));
    }

    public static boolean isGazing(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        return data.gaze() && data.hasAbility(SpiritAbility.GAZE);
    }

    /** Погляд не діє на босів і на тих, ким керує гравець. */
    public static boolean affectedByGaze(LivingEntity entity) {
        return entity instanceof Mob mob
                && !(mob instanceof EnderDragon)
                && !(mob instanceof WitherBoss)
                && !SpiritPossession.isPossessed(mob)
                && !SpiritTouch.isGrabbed(mob)
                && !SpiritDelirium.isDecoy(mob);
    }

    private static boolean isFlying(Mob mob) {
        return mob instanceof FlyingMob || mob instanceof Vex || mob instanceof Bee || mob instanceof Bat
                || mob.getMoveControl() instanceof FlyingMoveControl || mob.isNoGravity();
    }

    /** Погляд Привида: агресивні моби тікають, нейтральні й мирні тягнуться до гравця. */
    private static void gaze(ServerPlayer player, ServerLevel level) {
        double radius = Config.PRESENCE_RADIUS.get();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius));

        for (Mob mob : mobs) {
            if (!affectedByGaze(mob)) {
                continue;
            }

            boolean neutral = mob instanceof NeutralMob;
            boolean aggressive = mob instanceof Enemy && !neutral;

            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
            if (neutral) {
                ((NeutralMob) mob).stopBeingAngry();
            }

            double distance = mob.distanceToSqr(player);
            if (aggressive) {
                if (distance < 400.0) {
                    steer(mob, player, true);
                }
            } else if (distance > 9.0) {
                steer(mob, player, false);
            }
        }
    }

    /** Гонить моба від гравця або до нього: стежками, польотом або стрибками (слизь). */
    private static void steer(Mob mob, ServerPlayer player, boolean away) {
        Vec3 horizontal = mob.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        }
        Vec3 dir = horizontal.normalize();
        if (!away) {
            dir = dir.scale(-1.0);
        }

        if (isFlying(mob)) {
            double rise = away ? 1.5 : (player.getEyeY() - mob.getY()) * 0.5;
            Vec3 goal = mob.position().add(dir.scale(8.0)).add(0.0, rise, 0.0);
            mob.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, 1.2);
            return;
        }

        if (mob instanceof PathfinderMob pathfinder) {
            Vec3 goal = away
                    ? DefaultRandomPos.getPosAway(pathfinder, 16, 7, player.position())
                    : player.position();
            if (goal != null) {
                pathfinder.getNavigation().moveTo(goal.x, goal.y, goal.z, away ? 1.4 : 1.1);
            }
            return;
        }

        // Усе інше (наприклад, слизь): повертаємо до потрібного боку й підштовхуємо стрибком
        mob.setYRot((float) (Mth.atan2(dir.z, dir.x) * 57.29577951308232) - 90.0F);
        if (mob.onGround()) {
            mob.setDeltaMovement(dir.x * 0.35, 0.42, dir.z * 0.35);
            mob.hasImpulse = true;
        }
    }

    /** Присутність: агресивні моби нападають, мирні й нейтральні тікають. */
    private static void scare(ServerPlayer player, ServerLevel level) {
        double radius = Config.PRESENCE_RADIUS.get();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius));

        for (Mob mob : mobs) {
            if (SpiritPossession.isPossessed(mob) || SpiritTouch.isGrabbed(mob) || SpiritDelirium.isDecoy(mob)) {
                continue;
            }

            boolean neutral = mob instanceof NeutralMob;
            if (mob instanceof Enemy && !neutral) {
                if (mob.getTarget() != player) {
                    mob.setTarget(player);
                }
                continue;
            }

            if (neutral) {
                NeutralMob angry = (NeutralMob) mob;
                angry.stopBeingAngry();
                mob.setTarget(null);
            }
            if (mob instanceof PathfinderMob pathfinder && pathfinder.distanceToSqr(player) < 400.0) {
                Vec3 away = DefaultRandomPos.getPosAway(pathfinder, 16, 7, player.position());
                if (away != null) {
                    pathfinder.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
                }
            }
        }
    }

    /** Крижаний Доторк: хто отримав удар від гравця (або від моба, у якого він сидить), той замерзає. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        ServerPlayer owner = null;
        if (attacker instanceof ServerPlayer player) {
            owner = player;
        } else if (attacker instanceof Mob mob) {
            SpiritPossession.Control control = SpiritPossession.controlFor(mob);
            if (control != null) {
                owner = control.player;
            }
        }
        if (owner == null || event.getEntity() == owner) {
            return;
        }

        SpiritData data = SpiritFormState.get(owner);
        if (data.icy() && data.hasAbility(SpiritAbility.ICY_TOUCH)) {
            freeze(event.getEntity());
        }
    }
}
