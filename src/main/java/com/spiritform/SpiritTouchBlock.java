package com.spiritform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Доторк Привида, режим блока (довге натискання). Блок, на який дивишся, відривається від землі й висить
 * перед тобою. Колесо миші наближає й віддаляє його, наступне натискання клавіші кидає блок уперед.
 * Кинутий блок завдає шкоди живим істотам, у яких влучає, а потім падає й стає блоком (чи випадає предметом).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritTouchBlock {

    private static final class Grab {
        final ServerPlayer player;
        final FallingBlockEntity block;
        double distance;

        Grab(ServerPlayer player, FallingBlockEntity block, double distance) {
            this.player = player;
            this.block = block;
            this.distance = distance;
        }
    }

    private static final class Thrown {
        final UUID owner;
        final long expires;
        final Set<UUID> hit = new HashSet<>();
        double speed;

        Thrown(UUID owner, long expires) {
            this.owner = owner;
            this.expires = expires;
        }
    }

    /** Нижня межа швидкості, з якої влучання завдає шкоди. */
    private static final double MIN_IMPACT_SPEED = 0.25;

    private static final Map<UUID, Grab> BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, Grab> BY_BLOCK = new ConcurrentHashMap<>();
    private static final Map<UUID, Thrown> THROWN = new ConcurrentHashMap<>();

    private SpiritTouchBlock() {
    }

    public static boolean isHolding(ServerPlayer player) {
        return BY_PLAYER.containsKey(player.getUUID());
    }

    static void setFlag(ServerPlayer player, boolean value) {
        SpiritData data = SpiritFormState.get(player);
        if (data.blockGrab() != value) {
            SpiritFormState.set(player, data.withBlockGrab(value));
        }
    }

    /** Довге натискання: піднімає блок, на який дивиться гравець. */
    public static void grab(ServerPlayer player) {
        if (isHolding(player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        double range = Config.TOUCH_RANGE.get();
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.spirit_form.touch.block_failed"), true);
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (state.isAir() || level.getBlockEntity(pos) != null || hardness < 0.0F
                || hardness > Config.TOUCH_BLOCK_MAX_HARDNESS.get()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.touch.block_failed"), true);
            return;
        }

        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        block.setNoGravity(true);
        block.setDeltaMovement(Vec3.ZERO);
        double distance = Mth.clamp(eye.distanceTo(hit.getLocation()), 2.5, range);

        Grab grab = new Grab(player, block, distance);
        BY_PLAYER.put(player.getUUID(), grab);
        BY_BLOCK.put(block.getUUID(), grab);
        setFlag(player, true);

        level.playSound(null, pos, SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    public static void adjustDistance(ServerPlayer player, int steps) {
        Grab grab = BY_PLAYER.get(player.getUUID());
        if (grab != null) {
            grab.distance = Mth.clamp(grab.distance + Mth.clamp(steps, -3, 3), 2.0, Config.TOUCH_RANGE.get());
        }
    }

    /** Кидає блок уперед. */
    public static void throwBlock(ServerPlayer player) {
        Grab grab = detach(player);
        if (grab == null || grab.block.isRemoved()) {
            return;
        }
        FallingBlockEntity block = grab.block;
        Vec3 look = player.getLookAngle();
        block.setNoGravity(false);
        block.setDeltaMovement(new Vec3(look.x, look.y + 0.1, look.z).normalize().scale(Config.TOUCH_THROW_POWER.get()));
        block.hurtMarked = true;
        block.hasImpulse = true;

        long expires = player.serverLevel().getServer().getTickCount() + 100;
        THROWN.put(block.getUUID(), new Thrown(player.getUUID(), expires));
        player.level().playSound(null, block.getX(), block.getY(), block.getZ(),
                SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** Відпускає блок без кидка: він просто падає. */
    public static void drop(ServerPlayer player) {
        Grab grab = detach(player);
        if (grab != null && !grab.block.isRemoved()) {
            grab.block.setNoGravity(false);
        }
    }

    private static Grab detach(ServerPlayer player) {
        Grab grab = BY_PLAYER.remove(player.getUUID());
        if (grab != null) {
            BY_BLOCK.remove(grab.block.getUUID());
        }
        setFlag(player, false);
        return grab;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        if (!THROWN.isEmpty()) {
            long now = event.getServer().getTickCount();
            THROWN.values().removeIf(thrown -> thrown.expires <= now);
        }
        for (Grab grab : new ArrayList<>(BY_PLAYER.values())) {
            ServerPlayer player = grab.player;
            if (player.hasDisconnected() || !player.isAlive() || grab.block.isRemoved()
                    || grab.block.level() != player.level() || SpiritPossession.isPossessing(player)) {
                drop(player);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof FallingBlockEntity block) || block.level().isClientSide) {
            return;
        }
        Thrown thrown = THROWN.get(block.getUUID());
        if (thrown != null) {
            thrown.speed = block.getDeltaMovement().length();
        }
        if (BY_BLOCK.containsKey(block.getUUID())) {
            block.time = 1; // щоб блок не розсипався від старості, поки ми його тримаємо
            block.setDeltaMovement(Vec3.ZERO); // рухаємо його самі (див. hold), ванільний рух не потрібен
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (BY_BLOCK.isEmpty() && THROWN.isEmpty()) {
            return;
        }
        if (!(event.getEntity() instanceof FallingBlockEntity block) || block.level().isClientSide) {
            return;
        }

        Grab grab = BY_BLOCK.get(block.getUUID());
        if (grab != null) {
            hold(grab);
            return;
        }
        Thrown thrown = THROWN.get(block.getUUID());
        if (thrown != null) {
            checkHit(block, thrown);
        }
    }

    /** Тримає блок на відстані перед очима гравця. */
    private static void hold(Grab grab) {
        FallingBlockEntity block = grab.block;
        ServerPlayer player = grab.player;
        if (block.level() != player.level()) {
            return;
        }

        Vec3 want = player.getEyePosition().add(player.getLookAngle().scale(grab.distance));
        Vec3 center = block.position().add(0.0, block.getBbHeight() * 0.5, 0.0);
        Vec3 step = want.subtract(center).scale(0.4);
        if (step.lengthSqr() > 2.25) {
            step = step.normalize().scale(1.5);
        }
        Vec3 before = block.position();
        block.move(MoverType.SELF, step);
        block.resetFallDistance();

        // Клієнтам теж розсилаємо швидкість щотіку: блок рухається плавно, як під час кидка,
        // а не стрибками раз на кілька тіків (так оновлюються звичайні падаючі блоки)
        block.setDeltaMovement(block.position().subtract(before));
        block.hurtMarked = true;
        block.hasImpulse = true;

        if (block.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, block.getX(), block.getY() + 0.5, block.getZ(),
                    3, 0.4, 0.4, 0.4, 0.02);
        }
    }

    /** Влучання кинутого блока в живу істоту: шкода пропорційна швидкості. */
    private static void checkHit(FallingBlockEntity block, Thrown thrown) {
        if (thrown.speed < MIN_IMPACT_SPEED || !(block.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(thrown.owner);
        float damage = (float) (thrown.speed * Config.TOUCH_BLOCK_DAMAGE.get());

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, block.getBoundingBox().inflate(0.25),
                entity -> entity.isAlive() && !entity.isSpectator()
                        && !entity.getUUID().equals(thrown.owner) && !thrown.hit.contains(entity.getUUID()))) {
            thrown.hit.add(target.getUUID());
            if (owner != null) {
                target.setLastHurtByPlayer(owner);
            }
            target.hurt(level.damageSources().fallingBlock(block), damage);
            block.setDeltaMovement(block.getDeltaMovement().scale(0.3));
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_BIG_FALL, SoundSource.HOSTILE, 1.0F, 0.9F);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            drop(player);
        }
    }
}
