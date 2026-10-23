package com.spiritform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Дарування Життя і Терези: дальні здібності із зарядом. Поки утримується клавіша, заряд росте,
 * а за кожну одиницю заряду гравець віддає одне сердечко (доки в нього не лишиться одне).
 * Після відпускання вилітає кулька диму: біла (Дарування Життя) лікує моба на стільки ж здоров'я,
 * скільки було в заряді, чорна (Терези) забирає стільки ж.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritCharge {

    private static final class Charge {
        final ServerPlayer player;
        final SpiritAbility ability;
        int hearts;
        int ticks;

        Charge(ServerPlayer player, SpiritAbility ability) {
            this.player = player;
            this.ability = ability;
        }
    }

    private static final class Ball {
        final ServerPlayer owner;
        final ServerLevel level;
        final SpiritAbility ability;
        final Vec3 direction;
        final float amount;
        Vec3 position;
        int life;

        Ball(ServerPlayer owner, ServerLevel level, SpiritAbility ability, Vec3 position, Vec3 direction, float amount) {
            this.owner = owner;
            this.level = level;
            this.ability = ability;
            this.position = position;
            this.direction = direction;
            this.amount = amount;
        }
    }

    /** Одне сердечко = 2 одиниці здоров'я. */
    private static final float HEART = 2.0F;

    /** Здоров'я, нижче якого заряджання зупиняється: одне сердечко. */
    private static final float MIN_HEALTH = 2.0F;

    private static final double SPEED = 1.0;

    private static final Map<UUID, Charge> CHARGES = new ConcurrentHashMap<>();
    private static final List<Ball> BALLS = new ArrayList<>();

    private SpiritCharge() {
    }

    public static boolean isCharging(ServerPlayer player) {
        return CHARGES.containsKey(player.getUUID());
    }

    /** pressed = true: почати заряджання; false: відпустити кульку. */
    public static void set(ServerPlayer player, SpiritAbility ability, boolean pressed) {
        if (pressed) {
            start(player, ability);
        } else {
            release(player);
        }
    }

    private static void start(ServerPlayer player, SpiritAbility ability) {
        if (!SpiritFormState.get(player).hasAbility(ability)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.active.locked", ability.displayName()), true);
            return;
        }
        if (CHARGES.containsKey(player.getUUID())) {
            return;
        }
        if (player.getHealth() <= MIN_HEALTH) {
            player.displayClientMessage(Component.translatable("message.spirit_form.charge.no_health"), true);
            return;
        }
        CHARGES.put(player.getUUID(), new Charge(player, ability));
    }

    private static void release(ServerPlayer player) {
        Charge charge = CHARGES.remove(player.getUUID());
        if (charge == null || charge.hearts <= 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 direction = player.getLookAngle();
        Vec3 start = player.getEyePosition().add(direction);
        BALLS.add(new Ball(player, level, charge.ability, start, direction, charge.hearts * HEART));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.9F,
                charge.ability == SpiritAbility.LIFE_GIFT ? 1.4F : 0.6F);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!CHARGES.isEmpty()) {
            for (Charge charge : new ArrayList<>(CHARGES.values())) {
                tickCharge(charge);
            }
        }
        if (!BALLS.isEmpty()) {
            tickBalls();
        }
    }

    private static void tickCharge(Charge charge) {
        ServerPlayer player = charge.player;
        if (player.hasDisconnected() || !player.isAlive()
                || !SpiritFormState.get(player).hasAbility(charge.ability)) {
            CHARGES.remove(player.getUUID());
            return;
        }

        charge.ticks++;
        if (charge.ticks < Config.CHARGE_TICKS.get() || charge.hearts >= Config.CHARGE_MAX.get()) {
            return;
        }
        if (player.getHealth() - HEART < MIN_HEALTH) {
            return; // лишилося одне сердечко: заряд більше не росте
        }

        charge.ticks = 0;
        charge.hearts++;
        player.setHealth(player.getHealth() - HEART);
        SpiritServerEvents.markLowHealth(player);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F,
                0.6F + 0.08F * charge.hearts);
        player.displayClientMessage(
                Component.translatable("message.spirit_form.charge", charge.hearts), true);
    }

    private static void tickBalls() {
        double maxLife = Config.CHARGE_RANGE.get() / SPEED;

        Iterator<Ball> iterator = BALLS.iterator();
        while (iterator.hasNext()) {
            Ball ball = iterator.next();
            if (ball.owner.hasDisconnected() || ++ball.life > maxLife || !advance(ball)) {
                iterator.remove();
            }
        }
    }

    /** Рухає кульку на один тік. Повертає false, якщо вона влучила або зупинилася. */
    private static boolean advance(Ball ball) {
        ServerLevel level = ball.level;
        int substeps = 3;

        for (int i = 0; i < substeps; i++) {
            ball.position = ball.position.add(ball.direction.scale(SPEED / substeps));

            BlockPos pos = BlockPos.containing(ball.position);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                puff(ball);
                return false;
            }

            List<Mob> hit = level.getEntitiesOfClass(
                    Mob.class, new AABB(ball.position, ball.position).inflate(0.4), Mob::isAlive);
            if (!hit.isEmpty()) {
                strike(ball, hit.get(0));
                return false;
            }
        }

        trail(ball);
        return true;
    }

    private static boolean white(Ball ball) {
        return ball.ability == SpiritAbility.LIFE_GIFT;
    }

    /** Кулька з білих (Дарування Життя) або чорних (Терези) частинок диму. */
    private static void trail(Ball ball) {
        Vec3 p = ball.position;
        if (white(ball)) {
            ball.level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 8, 0.12, 0.12, 0.12, 0.0);
        } else {
            ball.level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 5, 0.12, 0.12, 0.12, 0.0);
            ball.level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 5, 0.15, 0.15, 0.15, 0.0);
        }
    }

    private static void puff(Ball ball) {
        Vec3 p = ball.position;
        ball.level.sendParticles(white(ball) ? ParticleTypes.CLOUD : ParticleTypes.SMOKE,
                p.x, p.y, p.z, 12, 0.25, 0.25, 0.25, 0.02);
    }

    private static void strike(Ball ball, Mob mob) {
        ServerLevel level = ball.level;
        double x = mob.getX();
        double y = mob.getY() + mob.getBbHeight() * 0.5;
        double z = mob.getZ();

        if (white(ball)) {
            mob.heal(ball.amount);
            level.sendParticles(ParticleTypes.HEART, x, y, z, 6, 0.4, 0.4, 0.4, 0.02);
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 14, 0.3, 0.4, 0.3, 0.03);
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        } else {
            mob.setLastHurtByPlayer(ball.owner);
            mob.hurt(level.damageSources().indirectMagic(ball.owner, ball.owner), ball.amount);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 14, 0.3, 0.4, 0.3, 0.03);
            level.playSound(null, x, y, z, SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHARGES.remove(event.getEntity().getUUID());
    }
}
