package com.spiritform;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SpiritTeleport {

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    /** Наскільки не доходимо до стіни, на яку дивиться гравець (у блоках). */
    private static final double WALL_MARGIN = 0.4;

    /** Крок відступу від стіни, якщо в точці призначення не вистачає місця. */
    private static final double RETREAT_STEP = 0.1;

    /** Наскільки далеко можна відступити від стіни в пошуках вільного місця. */
    private static final double MAX_RETREAT = 6.0;

    private SpiritTeleport() {
    }

    public static void activate(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);

        if (data.movement() < SpiritBalance.MAX_LEVEL) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.teleport.locked"),
                    true
            );
            return;
        }

        UUID id = player.getUUID();
        long now = player.level().getGameTime();

        if (COOLDOWN_UNTIL.getOrDefault(id, 0L) > now) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.ability.cooldown"),
                    true
            );
            return;
        }

        Vec3 destination = findDestination(player);

        if (destination == null) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.teleport.failed"),
                    true
            );
            return;
        }

        int cost = Config.ABILITY_HUNGER_COST.get();
        int food = player.getFoodData().getFoodLevel();

        if (food < cost) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.ability.hungry"),
                    true
            );
            return;
        }

        player.teleportTo(
                destination.x,
                destination.y,
                destination.z
        );

        player.getFoodData().setFoodLevel(food - cost);

        long cooldown = Config.ABILITY_COOLDOWN_SECONDS.get() * 20L;
        COOLDOWN_UNTIL.put(id, now + cooldown);

        player.displayClientMessage(
                Component.translatable("message.spirit_form.teleport.activated"),
                true
        );
    }

    /**
     * Куди телепортувати гравця (позиція ніг) або null, якщо місця немає.
     *
     * Якщо гравець дивиться на блок, його ставимо перед ним (за зовнішнім краєм поверхні).
     * Якщо дивиться в повітря, переносимо вздовж погляду на половину радіуса з налаштувань.
     */
    private static Vec3 findDestination(ServerPlayer player) {
        double range = Config.TELEPORT_RANGE.get();

        Vec3 eyePosition = player.getEyePosition();
        Vec3 lookDirection = player.getLookAngle();

        BlockHitResult hit = player.level().clip(new ClipContext(
                eyePosition,
                eyePosition.add(lookDirection.scale(range)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        // Підлога чи стеля: ставимо гравця прямо на поверхню, на яку він дивиться
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 location = hit.getLocation();
            Vec3 feet = null;
            if (hit.getDirection() == Direction.UP) {
                feet = new Vec3(location.x, location.y + 0.01, location.z);
            } else if (hit.getDirection() == Direction.DOWN) {
                feet = new Vec3(location.x, location.y - player.getBbHeight() - 0.01, location.z);
            }
            if (feet != null && canStandAt(player, feet)) {
                return feet;
            }
        }

        double distance;
        if (hit.getType() == HitResult.Type.BLOCK) {
            distance = eyePosition.distanceTo(hit.getLocation()) - WALL_MARGIN;
        } else {
            distance = range / 2.0;
        }

        // Рухаємось від бажаної точки назад до гравця, поки там не стане вільно
        AABB currentBox = player.getBoundingBox();
        for (double d = distance; d >= 0.0 && distance - d <= MAX_RETREAT; d -= RETREAT_STEP) {
            Vec3 shift = lookDirection.scale(d);
            AABB movedBox = currentBox.move(shift);

            if (isInsideWorldHeight(player, movedBox) && !collidesWithBlocks(player, movedBox)) {
                return player.position().add(shift);
            }
        }

        return null;
    }

    private static boolean canStandAt(ServerPlayer player, Vec3 feet) {
        AABB box = player.getBoundingBox().move(feet.subtract(player.position()));
        return isInsideWorldHeight(player, box) && !collidesWithBlocks(player, box);
    }

    private static boolean isInsideWorldHeight(ServerPlayer player, AABB box) {
        return box.minY >= player.level().getMinBuildHeight()
                && box.maxY <= player.level().getMaxBuildHeight();
    }

    private static boolean collidesWithBlocks(ServerPlayer player, AABB box) {
        return player.level().getBlockCollisions(player, box).iterator().hasNext();
    }
}
