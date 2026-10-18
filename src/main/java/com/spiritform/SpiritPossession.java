package com.spiritform;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Вселення привида в моба (клавіша O у формі Привида).
 *
 * Камера гравця переноситься на моба (як у режимі спостерігача), а керування мобом
 * передається через пакети: клієнт щотіку надсилає напрямок руху, стрибок і погляд,
 * сервер застосовує їх до моба. Саме рух моба підміняє LivingEntityMixin.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritPossession {

    /** Максимальна відстань до моба, в якого можна вселитися. */
    private static final double RANGE = 16.0;

    /** Керування мобом, який зараз під контролем привида. */
    public static final class Control {
        final ServerPlayer player;
        final Mob mob;
        volatile float forward;
        volatile float strafe;
        volatile boolean jump;
        volatile float yaw;
        volatile float pitch;

        Control(ServerPlayer player, Mob mob) {
            this.player = player;
            this.mob = mob;
            this.yaw = player.getYRot();
            this.pitch = player.getXRot();
        }

        public float forward() {
            return forward;
        }

        public float strafe() {
            return strafe;
        }

        public float yaw() {
            return yaw;
        }
    }

    private static final Map<UUID, Control> BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Control> BY_MOB = new ConcurrentHashMap<>();

    private SpiritPossession() {
    }

    /** Для Mixin: чи керує цим мобом привид (тільки на сервері). */
    public static Control controlFor(LivingEntity entity) {
        if (BY_MOB.isEmpty() || entity.level().isClientSide) {
            return null;
        }
        return BY_MOB.get(entity.getUUID());
    }

    public static boolean isPossessing(ServerPlayer player) {
        return BY_PLAYER.containsKey(player.getUUID());
    }

    public static void toggle(ServerPlayer player) {
        if (isPossessing(player)) {
            release(player);
            player.displayClientMessage(Component.translatable("message.spirit_form.possess.off"), true);
            return;
        }

        if (!SpiritFormState.isActive(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.possess.locked"), true);
            return;
        }

        Mob target = findTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.possess.failed"), true);
            return;
        }

        Control control = new Control(player, target);
        BY_PLAYER.put(player.getUUID(), control);
        BY_MOB.put(target.getUUID(), control);

        target.setTarget(null);
        target.getNavigation().stop();
        player.setCamera(target);

        player.displayClientMessage(
                Component.translatable("message.spirit_form.possess.on", target.getDisplayName()),
                true
        );
    }

    /** Виходить з моба. Безпечно викликати, навіть якщо гравець нікого не контролює. */
    public static void release(ServerPlayer player) {
        Control control = BY_PLAYER.remove(player.getUUID());
        if (control == null) {
            return;
        }
        BY_MOB.remove(control.mob.getUUID());

        Mob mob = control.mob;
        mob.setXxa(0.0F);
        mob.setZza(0.0F);
        mob.setJumping(false);

        if (!player.hasDisconnected() && player.getCamera() == mob) {
            player.setCamera(player);
        }
    }

    public static void setInput(ServerPlayer player, PossessInputPayload input) {
        Control control = BY_PLAYER.get(player.getUUID());
        if (control == null) {
            return;
        }
        control.forward = sanitize(input.forward(), 1.0F);
        control.strafe = sanitize(input.strafe(), 1.0F);
        control.jump = input.jump();
        control.yaw = Float.isFinite(input.yaw()) ? input.yaw() : control.yaw;
        control.pitch = Float.isFinite(input.pitch())
                ? Math.max(-90.0F, Math.min(90.0F, input.pitch()))
                : control.pitch;
    }

    private static float sanitize(float value, float limit) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(-limit, Math.min(limit, value));
    }

    private static Mob findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                box,
                entity -> entity instanceof Mob && entity.isAlive() && !entity.isSpectator(),
                RANGE * RANGE
        );
        if (hit == null) {
            return null;
        }

        Entity entity = hit.getEntity();
        if (!(entity instanceof Mob mob) || !isSupported(mob) || BY_MOB.containsKey(mob.getUUID())) {
            return null;
        }
        return mob;
    }

    /** Літаючих і особливих мобів керувати таким способом не вдається. */
    private static boolean isSupported(Mob mob) {
        return !(mob instanceof FlyingMob
                || mob instanceof FlyingAnimal
                || mob instanceof Bat
                || mob instanceof Vex
                || mob instanceof EnderDragon
                || mob instanceof WitherBoss)
                && !mob.isNoGravity();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (BY_PLAYER.isEmpty()) {
            return;
        }

        for (Control control : new ArrayList<>(BY_PLAYER.values())) {
            ServerPlayer player = control.player;
            Mob mob = control.mob;

            boolean invalid = player.hasDisconnected()
                    || !player.isAlive()
                    || mob.isRemoved()
                    || !mob.isAlive()
                    || mob.level() != player.level()
                    || !SpiritFormState.isActive(player)
                    || player.getCamera() != mob; // наприклад, гравець натиснув Shift

            if (invalid) {
                boolean mobGone = mob.isRemoved() || !mob.isAlive();
                release(player);
                if (!player.hasDisconnected() && mobGone) {
                    player.displayClientMessage(Component.translatable("message.spirit_form.possess.off"), true);
                }
                continue;
            }

            mob.setTarget(null);
            mob.getNavigation().stop();
            if (control.jump) {
                mob.getJumpControl().jump();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            release(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (Control control : new ArrayList<>(BY_PLAYER.values())) {
            release(control.player);
        }
    }

    /** Швидкість ходьби моба. */
    public static float speedOf(Mob mob) {
        return (float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }
}
