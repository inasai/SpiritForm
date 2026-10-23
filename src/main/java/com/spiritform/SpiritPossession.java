package com.spiritform;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
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

    /** Керування мобом, який зараз під контролем привида. */
    public static final class Control {
        final ServerPlayer player;
        final Mob mob;
        volatile float forward;
        volatile float strafe;
        volatile boolean jump;
        volatile float yaw;
        volatile float pitch;
        long lastAttack;
        long lastUse;

        /** Літаючий моб: рухається вільно в усіх напрямках, а не по землі. */
        final boolean flying;
        final boolean wasNoAi;
        final boolean wasNoGravity;
        Vec3 velocity = Vec3.ZERO;

        Control(ServerPlayer player, Mob mob) {
            this.player = player;
            this.mob = mob;
            this.flying = isFlier(mob);
            this.wasNoAi = mob.isNoAi();
            this.wasNoGravity = mob.isNoGravity();
            this.yaw = player.getYRot();
            this.pitch = player.getXRot();
        }

        public boolean flying() {
            return flying;
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

    public static final int ACTION_ATTACK = 0;
    public static final int ACTION_USE = 1;

    private static final long ATTACK_COOLDOWN_TICKS = 10;
    private static final long USE_COOLDOWN_TICKS = 4;
    private static final double USE_RANGE = 4.5;

    private static final Logger LOGGER = LogUtils.getLogger();

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

    /** Чи керує цим мобом якийсь привид зараз. */
    public static boolean isPossessed(Entity entity) {
        return !BY_MOB.isEmpty() && BY_MOB.containsKey(entity.getUUID());
    }

    public static boolean isPossessing(ServerPlayer player) {
        return BY_PLAYER.containsKey(player.getUUID());
    }

    /** Звук заряду Вітрюка при вселенні (вище) і виході (нижче). */
    private static void playSound(ServerPlayer player, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_CHARGE, SoundSource.PLAYERS, 0.8F, pitch);
    }

    public static void toggle(ServerPlayer player) {
        if (isPossessing(player)) {
            release(player);
            player.displayClientMessage(Component.translatable("message.spirit_form.possess.off"), true);
            playSound(player, 0.8F);
            return;
        }

        if (!SpiritFormState.isActive(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.possess.locked"), true);
            return;
        }

        if (!SpiritFormState.get(player).hasAbility(SpiritAbility.POSSESSION)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.possess.not_learned"), true);
            return;
        }

        Mob target = findTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.possess.failed"), true);
            return;
        }

        int cost = FentonBracelet.cost(player, Config.ABILITY_HUNGER_COST.get());
        int food = player.getFoodData().getFoodLevel();
        if (food < cost) {
            player.displayClientMessage(Component.translatable("message.spirit_form.ability.hungry"), true);
            return;
        }
        player.getFoodData().setFoodLevel(food - cost);

        Control control = new Control(player, target);
        BY_PLAYER.put(player.getUUID(), control);
        BY_MOB.put(target.getUUID(), control);

        target.setTarget(null);
        target.getNavigation().stop();
        if (control.flying) {
            // Власний ШІ літуна вимикаємо, його рух повністю веде гравець
            target.setNoAi(true);
            target.setNoGravity(true);
            if (target instanceof Bat bat) {
                bat.setResting(false);
            }
        }
        player.setCamera(target);
        player.setInvisible(true);

        // Моби, що вже полювали на гравця, забувають про нього: вони бачать лише моба
        for (Mob hunter : player.level().getEntitiesOfClass(
                Mob.class, player.getBoundingBox().inflate(48.0), mob -> mob.getTarget() == player)) {
            hunter.setTarget(null);
        }

        // Назви клавіш підставляє клієнт за поточним керуванням гравця
        player.displayClientMessage(
                Component.translatable(
                        "message.spirit_form.possess.on",
                        target.getDisplayName(),
                        Component.keybind("key.spirit_form.toggle_spirit")
                ),
                true
        );
        playSound(player, 1.0F);
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
        if (control.flying) {
            mob.setNoAi(control.wasNoAi);
            mob.setNoGravity(control.wasNoGravity);
            mob.setDeltaMovement(Vec3.ZERO);
        }

        player.setInvisible(player.hasEffect(MobEffects.INVISIBILITY));

        if (!player.hasDisconnected() && player.getCamera() == mob) {
            player.setCamera(player);
        }
    }

    /**
     * Вихід з форми Привида під час вселення: гравець забирає в моба здоров'я, скільки йому
     * не вистачає до повного. Якщо в моба менше, гравець отримує лише те, що забрав, а моб помирає.
     * Після цього гравець стає людиною, а форма Привида перезаряджається.
     */
    public static void drainAndExit(ServerPlayer player) {
        Control control = BY_PLAYER.get(player.getUUID());
        if (control == null) {
            SpiritForm.deactivate(player);
            return;
        }

        Mob mob = control.mob;
        float need = Math.max(0.0F, player.getMaxHealth() - player.getHealth());
        float available = Math.max(0.0F, mob.getHealth());
        float taken = Math.min(need, available);

        // Якщо в моба ще лишилося здоров'я, воно переходить у голод
        double ratio = Config.DRAIN_HP_PER_HUNGER.get();
        int food = player.getFoodData().getFoodLevel();
        int foodGain = 0;
        float leftover = available - taken;
        if (leftover > 0.0F && food < 20) {
            foodGain = (int) Math.min(20 - food, Math.floor(leftover / ratio));
        }
        float spentOnFood = (float) (foodGain * ratio);
        float total = taken + spentOnFood;

        release(player);

        if (total > 0.0F) {
            if (taken > 0.0F) {
                player.heal(taken);
            }
            if (foodGain > 0) {
                player.getFoodData().setFoodLevel(food + foodGain);
            }

            float remaining = available - total;
            mob.setLastHurtByPlayer(player);
            if (remaining <= 0.0F) {
                mob.setHealth(0.0F);
                mob.die(player.damageSources().magic());
            } else {
                mob.setHealth(remaining);
                mob.level().broadcastEntityEvent(mob, (byte) 2); // анімація й звук ушкодження
            }
        }

        SpiritForm.deactivate(player);
        SpiritForm.startCooldown(player);

        // З Інстинктом Привида після поглинання одразу вмикається Зникнення
        SpiritData after = SpiritFormState.get(player);
        if (after.instinct() && after.hasAbility(SpiritAbility.VANISH) && !SpiritInvisibility.isActive(player)) {
            SpiritInvisibility.activate(player);
        }

        if (taken > 0.0F && foodGain > 0) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.spirit_form.possess.drain_both",
                            String.format(Locale.ROOT, "%.1f", taken),
                            foodGain
                    ),
                    true
            );
        } else if (taken > 0.0F) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.spirit_form.possess.drain",
                            String.format(Locale.ROOT, "%.1f", taken)
                    ),
                    true
            );
        } else if (foodGain > 0) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.possess.drain_food", foodGain),
                    true
            );
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

    /** Клік миші під час вселення: моб атакує або гравець використовує предмет/блок його очима. */
    public static void act(ServerPlayer player, int action) {
        Control control = BY_PLAYER.get(player.getUUID());
        if (control == null) {
            return;
        }

        long now = player.level().getGameTime();
        try {
            if (action == ACTION_ATTACK) {
                if (now - control.lastAttack >= attackCooldown(control.mob)) {
                    control.lastAttack = now;
                    attack(player, control);
                }
            } else if (action == ACTION_USE) {
                if (now - control.lastUse >= USE_COOLDOWN_TICKS) {
                    control.lastUse = now;
                    use(player, control.mob);
                }
            }
        } catch (RuntimeException e) {
            // Незвичайний моб не повинен ламати сервер
            LOGGER.warn("Spirit Form: possessed mob action failed", e);
        }
    }

    /** Скільки тіків моб відновлюється між атаками. */
    private static long attackCooldown(Mob mob) {
        if (mob instanceof Ghast) {
            return 40;
        }
        if (mob instanceof RangedAttackMob) {
            return 20;
        }
        return ATTACK_COOLDOWN_TICKS;
    }

    /** Напрямок погляду гравця очима моба (на сервері моб сам не завжди тримає нахил голови). */
    private static Vec3 lookOf(Mob mob) {
        Control control = BY_MOB.get(mob.getUUID());
        return control == null
                ? mob.getLookAngle()
                : Vec3.directionFromRotation(control.pitch, control.yaw);
    }

    /**
     * Особливі атаки мобів: вибух кріпера, вогняні кулі гаста й блейза, постріли лучників
     * і відьом тощо. Повертає true, якщо атаку оброблено.
     */
    private static boolean specialAttack(ServerPlayer player, Control control) {
        Mob mob = control.mob;
        ServerLevel level = player.serverLevel();
        Vec3 dir = lookOf(mob);

        if (mob instanceof Creeper creeper) {
            creeper.ignite(); // далі кріпер сам роздувається й вибухає
            return true;
        }

        if (mob instanceof Ghast ghast) {
            mob.swing(InteractionHand.MAIN_HAND);
            level.levelEvent(null, 1016, mob.blockPosition(), 0);
            LargeFireball fireball = new LargeFireball(level, mob, dir, ghast.getExplosionPower());
            fireball.setPos(mob.getX() + dir.x * 4.0, mob.getY(0.5) + 0.5, mob.getZ() + dir.z * 4.0);
            level.addFreshEntity(fireball);
            return true;
        }

        if (mob instanceof Blaze) {
            level.levelEvent(null, 1018, mob.blockPosition(), 0);
            SmallFireball fireball = new SmallFireball(level, mob, dir);
            fireball.setPos(mob.getX() + dir.x * 1.2, mob.getY(0.5) + 0.5, mob.getZ() + dir.z * 1.2);
            level.addFreshEntity(fireball);
            return true;
        }

        if (mob instanceof RangedAttackMob ranged && !(mob instanceof CrossbowAttackMob)) {
            Entity aimed = pickEntity(player, mob, 40.0);
            if (!(aimed instanceof LivingEntity victim)) {
                player.displayClientMessage(Component.translatable("message.spirit_form.possess.no_target"), true);
                return true;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            mob.setTarget(victim);
            try {
                ranged.performRangedAttack(victim, 1.0F);
            } finally {
                mob.setTarget(null);
            }
            return true;
        }

        return false;
    }

    private static void attack(ServerPlayer player, Control control) {
        Mob mob = control.mob;
        if (specialAttack(player, control)) {
            return;
        }
        if (!mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.possess.cannot_attack"), true);
            return;
        }

        mob.swing(InteractionHand.MAIN_HAND);

        Entity target = pickEntity(player, mob, 3.0 + mob.getBbWidth());
        if (target != null) {
            mob.doHurtTarget(target);
        }
    }

    private static void use(ServerPlayer player, Mob mob) {
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ServerLevel level = player.serverLevel();

        Entity entity = pickEntity(player, mob, USE_RANGE);
        if (entity != null && player.interactOn(entity, hand).consumesAction()) {
            return;
        }

        BlockHitResult block = clipBlocks(mob, USE_RANGE);
        if (block.getType() == HitResult.Type.BLOCK) {
            InteractionResult result = player.gameMode.useItemOn(
                    player, level, player.getItemInHand(hand), hand, block);
            if (result.consumesAction()) {
                return;
            }
        }

        player.gameMode.useItem(player, level, player.getItemInHand(hand), hand);
    }

    private static BlockHitResult clipBlocks(Mob mob, double range) {
        Vec3 eye = mob.getEyePosition();
        Vec3 end = eye.add(lookOf(mob).scale(range));
        return mob.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
    }

    /** Сутність, на яку дивиться моб (не далі за блок, що закриває огляд). */
    private static Entity pickEntity(ServerPlayer player, Mob mob, double range) {
        Vec3 eye = mob.getEyePosition();
        Vec3 look = lookOf(mob);
        Vec3 end = eye.add(look.scale(range));
        double maxDistanceSqr = range * range;

        BlockHitResult block = clipBlocks(mob, range);
        if (block.getType() == HitResult.Type.BLOCK) {
            end = block.getLocation();
            maxDistanceSqr = eye.distanceToSqr(end);
        }

        AABB box = mob.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mob,
                eye,
                end,
                box,
                entity -> entity != player && entity != mob
                        && entity.isAlive() && entity.isPickable() && !entity.isSpectator(),
                maxDistanceSqr
        );
        return hit == null ? null : hit.getEntity();
    }

    private static float sanitize(float value, float limit) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(-limit, Math.min(limit, value));
    }

    private static Mob findTarget(ServerPlayer player) {
        double range = Config.POSSESS_RANGE.get();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                box,
                entity -> entity instanceof Mob && entity.isAlive() && !entity.isSpectator(),
                range * range
        );
        if (hit == null) {
            return null;
        }

        Entity entity = hit.getEntity();
        if (!(entity instanceof Mob mob) || !isSupported(mob) || BY_MOB.containsKey(mob.getUUID())
                || SpiritTouch.isGrabbed(mob)) {
            return null;
        }
        return mob;
    }

    /** Літаючих і особливих мобів керувати таким способом не вдається. */
    private static boolean isSupported(Mob mob) {
        if (mob instanceof EnderDragon || mob instanceof WitherBoss) {
            return false;
        }
        return isFlier(mob) || !mob.isNoGravity();
    }

    /** Чи літає цей моб (фантом, вітрюк-бджола, кажан, блейз, алей, гаст, векс, папуга тощо). */
    private static boolean isFlier(Mob mob) {
        return mob instanceof FlyingMob
                || mob instanceof FlyingAnimal
                || mob instanceof Bat
                || mob instanceof Vex
                || mob instanceof Blaze
                || mob.getMoveControl() instanceof FlyingMoveControl;
    }

    /** Вільний політ одержимого літуна: напрямок погляду, бокові кроки й стрибок угору. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Control control = controlFor(mob);
        if (control == null || !control.flying) {
            return;
        }

        float yaw = control.yaw;
        float pitch = control.pitch;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setXRot(pitch);

        double yawRad = Math.toRadians(yaw);
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        Vec3 left = new Vec3(Math.cos(yawRad), 0.0, Math.sin(yawRad));
        Vec3 wish = look.scale(control.forward)
                .add(left.scale(control.strafe))
                .add(0.0, control.jump ? 1.0 : 0.0, 0.0);
        if (wish.lengthSqr() > 1.0) {
            wish = wish.normalize();
        }

        double base = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (mob.getAttributes().hasAttribute(Attributes.FLYING_SPEED)) {
            base = Math.max(base, mob.getAttributeValue(Attributes.FLYING_SPEED));
        }
        double speed = Mth.clamp(base, 0.3, 1.0) * 0.4 * speedMultiplier();

        Vec3 target = wish.scale(speed);
        Vec3 velocity = control.velocity.lerp(target, 0.25);

        mob.setDeltaMovement(velocity);
        mob.move(MoverType.SELF, velocity);
        control.velocity = mob.getDeltaMovement(); // стіни гасять швидкість
        mob.setDeltaMovement(Vec3.ZERO);
        mob.resetFallDistance();
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
                    || !SpiritFormState.isActive(player);

            if (invalid) {
                boolean mobGone = mob.isRemoved() || !mob.isAlive();
                release(player);
                if (!player.hasDisconnected() && mobGone) {
                    player.displayClientMessage(Component.translatable("message.spirit_form.possess.off"), true);
                }
                continue;
            }

            // Shift не виходить із моба: камеру повертаємо назад (вихід лише здібністю або клавішею форми)
            if (player.getCamera() != mob) {
                player.setCamera(mob);
            }

            // Дим навколо одержимого моба: стільки ж, скільки після Кроку Привида
            if (mob.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SMOKE,
                        mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                        6, mob.getBbWidth() * 0.5, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.5, 0.02);
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

    /** Швидкість моба за його власною характеристикою. */
    public static float speedOf(Mob mob) {
        return (float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /** Множник із конфігу для швидкості одержимого моба. */
    public static float speedMultiplier() {
        return Config.POSSESSED_SPEED_MULTIPLIER.get().floatValue();
    }
}
