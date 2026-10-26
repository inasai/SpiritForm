package com.spiritform;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Доторк Привида. Коротке натискання: моб, на якого дивиться гравець, на якого дивиться гравець, висить у повітрі
 * перед ним (навколо нього дим). Коли клавішу відпущено, моба відкидає вперед, і якщо він
 * вдариться об блок, то отримає шкоду залежно від швидкості удару.
 * Довге натискання: так само керуємо блоком (див. SpiritTouchBlock).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritTouch {

    private static final class Grab {
        final ServerPlayer player;
        final Mob mob;
        double distance;
        final boolean wasNoGravity;

        Grab(ServerPlayer player, Mob mob, double distance, boolean wasNoGravity) {
            this.player = player;
            this.mob = mob;
            this.distance = distance;
            this.wasNoGravity = wasNoGravity;
        }
    }

    private static final class Thrown {
        final UUID owner;
        final long expires;
        double speed;

        Thrown(UUID owner, long expires) {
            this.owner = owner;
            this.expires = expires;
        }
    }

    /** Нижня межа швидкості, з якої удар об блок завдає шкоди. */
    private static final double MIN_IMPACT_SPEED = 0.25;

    private static final Map<UUID, Grab> BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Grab> BY_MOB = new ConcurrentHashMap<>();
    private static final Map<UUID, Thrown> THROWN = new ConcurrentHashMap<>();

    private SpiritTouch() {
    }

    public static boolean isGrabbed(Entity entity) {
        return !BY_MOB.isEmpty() && BY_MOB.containsKey(entity.getUUID());
    }

    /** Скільки тіків треба тримати клавішу, щоб це було "довге натискання" (блок), а не "коротке" (моб). */
    private static final long HOLD_TICKS = 6;

    /** Гравці, які натиснули клавішу й ще не вирішено: коротке чи довге натискання (тік початку). */
    private static final Map<UUID, Long> PRESSED = new ConcurrentHashMap<>();

    /** Довге натискання вже підняло блок: відпускання клавіші нічого не означає. */
    private static final java.util.Set<UUID> LONG_PRESS = ConcurrentHashMap.newKeySet();

    /** Натискання кинуло ціль: наступне відпускання клавіші нічого не означає. */
    private static final java.util.Set<UUID> IGNORE_RELEASE = ConcurrentHashMap.newKeySet();

    /**
     * Клавіша Доторку. Тримати її постійно не треба.
     * Коротке натискання піднімає моба, на якого дивишся; довге (довше за третину секунди) піднімає блок.
     * Поки ціль висить, колесо миші наближає й віддаляє її, а наступне натискання кидає її вперед.
     */
    public static void set(ServerPlayer player, boolean pressed) {
        UUID id = player.getUUID();
        if (pressed) {
            if (!SpiritFormState.get(player).hasAbility(SpiritAbility.TOUCH)) {
                player.displayClientMessage(Component.translatable("message.spirit_form.touch.locked"), true);
                return;
            }
            if (SpiritPossession.isPossessing(player)) {
                return;
            }
            if (BY_PLAYER.containsKey(id)) {
                release(player, true);
                IGNORE_RELEASE.add(id);
                return;
            }
            if (SpiritTouchBlock.isHolding(player)) {
                SpiritTouchBlock.throwBlock(player);
                IGNORE_RELEASE.add(id);
                return;
            }
            PRESSED.put(id, player.serverLevel().getServer().getTickCount() + 0L);
            return;
        }

        if (IGNORE_RELEASE.remove(id) || LONG_PRESS.remove(id)) {
            return;
        }
        if (PRESSED.remove(id) != null) {
            start(player);
            return;
        }
        // Відпускання без натискання (наприклад, вимкнули режим здібностей): просто опускаємо ціль
        if (BY_PLAYER.containsKey(id)) {
            release(player, false);
        } else if (SpiritTouchBlock.isHolding(player)) {
            SpiritTouchBlock.drop(player);
        }
    }

    private static void start(ServerPlayer player) {
        if (!SpiritFormState.get(player).hasAbility(SpiritAbility.TOUCH)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.touch.locked"), true);
            return;
        }
        if (BY_PLAYER.containsKey(player.getUUID()) || SpiritPossession.isPossessing(player)) {
            return;
        }

        double range = Config.TOUCH_RANGE.get();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eye, end, box,
                entity -> entity instanceof Mob && entity.isAlive() && !entity.isSpectator(),
                range * range
        );

        if (hit == null || !(hit.getEntity() instanceof Mob mob)
                || mob instanceof EnderDragon || mob instanceof WitherBoss
                || SpiritPossession.isPossessed(mob) || isGrabbed(mob)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.touch.failed"), true);
            return;
        }

        double distance = Mth.clamp(eye.distanceTo(hit.getLocation()), 2.5, range);
        Grab grab = new Grab(player, mob, distance, mob.isNoGravity());

        THROWN.remove(mob.getUUID());
        BY_PLAYER.put(player.getUUID(), grab);
        BY_MOB.put(mob.getUUID(), grab);
        SpiritTouchBlock.setFlag(player, true);

        mob.setNoGravity(true);
        mob.getNavigation().stop();

        player.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    /** Колесико миші: відсуває (steps > 0) або наближає (steps < 0) схопленого моба чи блок. */
    public static void adjustDistance(ServerPlayer player, int steps) {
        Grab grab = BY_PLAYER.get(player.getUUID());
        if (grab == null) {
            SpiritTouchBlock.adjustDistance(player, steps);
            return;
        }
        int clamped = Mth.clamp(steps, -3, 3);
        grab.distance = Mth.clamp(grab.distance + clamped, 2.0, Config.TOUCH_RANGE.get());
    }

    /** Відпускає моба. throwIt = true відкидає його вперед. */
    public static void release(ServerPlayer player, boolean throwIt) {
        Grab grab = BY_PLAYER.remove(player.getUUID());
        if (grab == null) {
            return;
        }
        BY_MOB.remove(grab.mob.getUUID());
        SpiritTouchBlock.setFlag(player, false);

        Mob mob = grab.mob;
        mob.setNoGravity(grab.wasNoGravity);

        if (!throwIt || mob.isRemoved() || !mob.isAlive()) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 velocity = new Vec3(look.x, look.y + 0.1, look.z).normalize().scale(Config.TOUCH_THROW_POWER.get());
        mob.setDeltaMovement(velocity);
        mob.hurtMarked = true;
        mob.hasImpulse = true;

        long expires = player.serverLevel().getServer().getTickCount() + 80;
        THROWN.put(mob.getUUID(), new Thrown(player.getUUID(), expires));

        player.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!PRESSED.isEmpty()) {
            long tick = event.getServer().getTickCount();
            for (Map.Entry<UUID, Long> entry : new ArrayList<>(PRESSED.entrySet())) {
                if (tick - entry.getValue() < HOLD_TICKS) {
                    continue;
                }
                PRESSED.remove(entry.getKey());
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    LONG_PRESS.add(entry.getKey());
                    SpiritTouchBlock.grab(player);
                }
            }
        }

        if (!THROWN.isEmpty()) {
            long now = event.getServer().getTickCount();
            THROWN.values().removeIf(thrown -> thrown.expires <= now);
        }

        if (BY_PLAYER.isEmpty()) {
            return;
        }

        for (Grab grab : new ArrayList<>(BY_PLAYER.values())) {
            ServerPlayer player = grab.player;
            Mob mob = grab.mob;
            boolean invalid = player.hasDisconnected()
                    || !player.isAlive()
                    || mob.isRemoved()
                    || !mob.isAlive()
                    || mob.level() != player.level()
                    || SpiritPossession.isPossessing(player);
            if (invalid) {
                release(player, false);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (THROWN.isEmpty() || event.getEntity().level().isClientSide) {
            return;
        }
        Thrown thrown = THROWN.get(event.getEntity().getUUID());
        if (thrown != null) {
            thrown.speed = event.getEntity().getDeltaMovement().length();
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (BY_MOB.isEmpty() && THROWN.isEmpty()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }

        Grab grab = BY_MOB.get(mob.getUUID());
        if (grab != null) {
            hold(grab);
            return;
        }

        Thrown thrown = THROWN.get(mob.getUUID());
        if (thrown != null) {
            checkImpact(mob, thrown);
        }
    }

    /** Тримає моба на відстані перед очима гравця. */
    private static void hold(Grab grab) {
        Mob mob = grab.mob;
        ServerPlayer player = grab.player;
        if (mob.level() != player.level()) {
            return;
        }

        Vec3 want = player.getEyePosition().add(player.getLookAngle().scale(grab.distance));
        Vec3 center = mob.position().add(0.0, mob.getBbHeight() * 0.5, 0.0);
        Vec3 step = want.subtract(center).scale(0.4);
        if (step.lengthSqr() > 2.25) {
            step = step.normalize().scale(1.5);
        }

        mob.move(MoverType.SELF, step);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.resetFallDistance();
        mob.getNavigation().stop();

        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                    4, mob.getBbWidth() * 0.5, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.5, 0.02);
        }
    }

    /** Удар об блок: шкода пропорційна швидкості, з якою моб влетів у перешкоду. */
    private static void checkImpact(Mob mob, Thrown thrown) {
        boolean collided = mob.horizontalCollision || (mob.verticalCollision && !mob.onGround());
        if (!collided) {
            return;
        }

        THROWN.remove(mob.getUUID());
        if (thrown.speed < MIN_IMPACT_SPEED || !(mob.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(thrown.owner);
        if (owner != null) {
            mob.setLastHurtByPlayer(owner);
        }

        float damage = (float) (thrown.speed * Config.TOUCH_IMPACT_DAMAGE.get());
        mob.hurt(level.damageSources().flyIntoWall(), damage);

        level.sendParticles(ParticleTypes.SMOKE,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                20, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.PLAYER_BIG_FALL, SoundSource.HOSTILE, 1.0F, 0.9F);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PRESSED.remove(player.getUUID());
            LONG_PRESS.remove(player.getUUID());
            IGNORE_RELEASE.remove(player.getUUID());
            release(player, false);
        }
    }
}
