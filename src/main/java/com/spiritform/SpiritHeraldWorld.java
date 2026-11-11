package com.spiritform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Усе, що Вісники роблять зі світом: погода, ураган, відбиття снарядів, холод і штучна зима (з обліком змінених
 * блоків), вікно без вітру після Кроку Привида. Викликається з тіку Вісників і з SpiritHeralds.
 */
public final class SpiritHeraldWorld {

    // ---------- погода ----------

    /** Тримає дощ (і грозу для Бурі) над світом, а початкову погоду один раз запам'ятовує. */
    public static void keepWeather(ServerLevel level, boolean thunder) {
        MinecraftServer server = level.getServer();
        HeraldStore store = HeraldStore.get(server);
        ServerLevel overworld = server.overworld();
        if (!store.weatherSaved) {
            store.weatherSaved = true;
            store.weatherRain = overworld.isRaining();
            store.weatherThunder = overworld.isThundering();
            store.setDirty();
        }
        overworld.setWeatherParameters(0, 6000, true, thunder);
    }

    /** Повертає погоду, яка була до Вісників. Викликається, коли жодного Вісника з погодними ефектами вже немає. */
    public static void restoreWeather(MinecraftServer server) {
        HeraldStore store = HeraldStore.get(server);
        if (!store.weatherSaved) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (store.weatherRain || store.weatherThunder) {
            overworld.setWeatherParameters(0, 6000, true, store.weatherThunder);
        } else {
            overworld.setWeatherParameters(6000, 0, false, false);
        }
        store.weatherSaved = false;
        store.setDirty();
    }

    /** Робить ніч, якщо зараз день (для Вісника Смерті). Час нічого не повертає назад. */
    public static void makeNight(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        long time = overworld.getDayTime();
        long inDay = Math.floorMod(time, 24000L);
        if (inDay < 13000L) {
            overworld.setDayTime(time - inDay + 14000L);
        }
    }

    // ---------- ураган Вісника Бурі ----------

    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    private static final Map<UUID, Long> STEP_UNTIL = new HashMap<>();

    /** Щотіку для Вісника Бурі: вітер на гравців і відхилення снарядів. */
    public static void tickStorm(StormHerald herald, ServerLevel level) {
        double radius = Config.HERALD_RADIUS.get();
        long now = level.getGameTime();
        Vec3 center = herald.position().add(0.0, herald.getBbHeight() * 0.5, 0.0);

        if (herald.tickCount % 3 == 0) {
            for (int i = 0; i < 3; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                double dist = 2.0 + level.random.nextDouble() * 14.0;
                level.sendParticles(ParticleTypes.CLOUD, herald.getX() + Math.cos(angle) * dist,
                        herald.getY() + level.random.nextDouble() * 5.0, herald.getZ() + Math.sin(angle) * dist,
                        1, 0.0, 0.0, 0.0, 0.15);
            }
        }

        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            Vec3 last = LAST_POS.put(id, player.position());
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double dx = player.getX() - herald.getX();
            double dz = player.getZ() - herald.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius || dist < 0.05) {
                continue;
            }
            if (STEP_UNTIL.getOrDefault(id, 0L) > now) {
                continue;
            }

            double strength = 1.0 - dist / radius;
            double push = 0.06 + 0.40 * strength;

            // Швидкість гравця сервер сам не знає (рух рахує клієнт), тому беремо її зі зміни позиції
            Vec3 observed = Vec3.ZERO;
            if (last != null) {
                Vec3 delta = player.position().subtract(last);
                if (delta.lengthSqr() < 4.0) {
                    observed = delta;
                }
            }
            double vx = observed.x + dx / dist * push;
            double vz = observed.z + dz / dist * push;
            double vy = Math.max(-1.0, Math.min(1.0, observed.y));
            if (player.onGround() && strength > 0.2) {
                vy = Math.max(vy, 0.06);
            }
            // Стеля швидкості: сильно, але гравця не викидає за межі світу
            double horizontal = Math.sqrt(vx * vx + vz * vz);
            double cap = 1.2;
            if (horizontal > cap) {
                vx = vx / horizontal * cap;
                vz = vz / horizontal * cap;
            }
            if (player.getY() > level.getMaxBuildHeight() - 8) {
                vy = Math.min(vy, 0.0);
            }
            player.setDeltaMovement(vx, vy, vz);
            player.hurtMarked = true;
            if (herald.tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                        3, 0.4, 0.5, 0.4, 0.1);
            }
        }

        AABB area = herald.getBoundingBox().inflate(radius);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area,
                p -> p.getOwner() != herald && p.isAlive())) {
            Vec3 rel = projectile.position().subtract(center);
            double dist = rel.length();
            Vec3 velocity = projectile.getDeltaMovement();
            if (dist < 0.5 || dist > radius || velocity.lengthSqr() < 0.01 || rel.dot(velocity) >= 0.0) {
                continue;
            }
            double strength = 1.0 - dist / radius;
            Vec3 away = rel.normalize();
            projectile.setDeltaMovement(velocity.add(away.scale(0.15 + 0.7 * strength)).scale(0.98));
            projectile.hasImpulse = true;
            projectile.hurtMarked = true;
        }
    }

    // ---------- Крок Привида ----------

    /**
     * Якщо гравець дивиться на Вісника Бурі, Крок Привида переносить його впритул до нього.
     * Повертає позицію ніг або null (тоді працює звичайний Крок).
     */
    public static Vec3 stepToHerald(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        double radius = Config.HERALD_RADIUS.get();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        StormHerald best = null;
        double bestDistance = Double.MAX_VALUE;
        for (StormHerald herald : level.getEntitiesOfClass(StormHerald.class, player.getBoundingBox().inflate(radius))) {
            if (!herald.isAlive()) {
                continue;
            }
            Vec3 to = herald.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            double along = to.dot(look);
            if (distance > radius || distance < 2.5 || along <= 0.0) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off > Math.max(1.5, herald.getBbWidth()) || distance >= bestDistance) {
                continue;
            }
            best = herald;
            bestDistance = distance;
        }
        if (best == null) {
            return null;
        }
        Vec3 dir = new Vec3(player.getX() - best.getX(), 0.0, player.getZ() - best.getZ());
        if (dir.lengthSqr() < 1.0E-4) {
            dir = new Vec3(-look.x, 0.0, -look.z);
        }
        if (dir.lengthSqr() < 1.0E-4) {
            dir = new Vec3(1.0, 0.0, 0.0);
        }
        dir = dir.normalize();
        double offset = best.getBbWidth() * 0.5 + 1.4;
        double[] lifts = {0.0, 1.0, -1.0, 2.0, -2.0};
        for (double lift : lifts) {
            Vec3 dest = new Vec3(best.getX() + dir.x * offset, best.getY() + lift, best.getZ() + dir.z * offset);
            AABB box = player.getDimensions(player.getPose()).makeBoundingBox(dest);
            if (level.noCollision(player, box)) {
                return dest;
            }
        }
        return null;
    }

    /** Після успішного Кроку поряд із Вісником Бурі: коротке вікно, коли вітер не діє. */
    public static void onStep(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = Config.HERALD_RADIUS.get();
        boolean near = !level.getEntitiesOfClass(StormHerald.class, player.getBoundingBox().inflate(radius),
                h -> h.isAlive()).isEmpty();
        if (near) {
            STEP_UNTIL.put(player.getUUID(), level.getGameTime() + Config.HERALD_STORM_WINDOW_SECONDS.get() * 20L);
            LAST_POS.remove(player.getUUID());
        }
    }

    // ---------- холод Вісника Холоду ----------

    private static final Map<UUID, Long> CLEANSED_UNTIL = new HashMap<>();
    private static final Set<UUID> SLOWED_FLIERS = new HashSet<>();

    /** Імунітет Привида зняв ефекти: кілька секунд аура холоду не накладає нових. */
    public static void onCleanse(ServerPlayer player) {
        CLEANSED_UNTIL.put(player.getUUID(), player.level().getGameTime() + 100L);
        player.setTicksFrozen(0);
        releaseFlier(player);
    }

    private static void releaseFlier(ServerPlayer player) {
        if (SLOWED_FLIERS.remove(player.getUUID())) {
            SpiritForm.refreshStats(player);
        }
    }

    /** Щотіку для Вісника Холоду: аура холоду за відстанню та штучна зима. */
    public static void tickCold(ColdHerald herald, ServerLevel level) {
        double radius = Config.HERALD_RADIUS.get();
        long now = level.getGameTime();
        markWinter(herald.getUUID(), herald.getX(), herald.getZ(), radius);

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                releaseFlier(player);
                continue;
            }
            double dist = Math.sqrt(player.distanceToSqr(herald));
            if (dist > radius) {
                releaseFlier(player);
                continue;
            }
            if (CLEANSED_UNTIL.getOrDefault(player.getUUID(), 0L) > now) {
                continue;
            }
            double strength = Math.pow(1.0 - dist / radius, 1.5);
            if (strength < 0.08) {
                releaseFlier(player);
                continue;
            }
            int amplifier = (int) Math.min(6.0, Math.ceil(strength * 7.0) - 1.0);
            if (herald.tickCount % 10 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, amplifier, true, false, true));
            }
            int need = player.getTicksRequiredToFreeze();
            int target = (int) (need * Math.min(1.4, strength * 1.6));
            if (player.getTicksFrozen() < target) {
                player.setTicksFrozen(Math.min(target, player.getTicksFrozen() + 4));
            }
            if (SpiritFormState.isActive(player) && herald.tickCount % 5 == 0) {
                float base = SpiritBalance.flySpeed(SpiritFormState.get(player).movement());
                player.getAbilities().setFlyingSpeed((float) (base * (1.0 - 0.95 * strength)));
                player.onUpdateAbilities();
                SLOWED_FLIERS.add(player.getUUID());
            }
        }

        if (herald.tickCount % 10 == 0) {
            placeWinter(level, herald, 28);
            level.sendParticles(ParticleTypes.SNOWFLAKE, herald.getX(), herald.getY() + 1.5, herald.getZ(),
                    10, 3.0, 1.5, 3.0, 0.02);
        }
    }

    /** Знімає уповільнення польоту з усіх, кого заморозив холод (коли Вісника Холоду вже немає). */
    public static void releaseAllFliers(MinecraftServer server) {
        for (UUID id : new ArrayList<>(SLOWED_FLIERS)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                SpiritForm.refreshStats(player);
            }
            SLOWED_FLIERS.remove(id);
        }
    }

    // ---------- штучна зима ----------

    private static void placeWinter(ServerLevel level, ColdHerald herald, int attempts) {
        HeraldStore store = HeraldStore.get(level.getServer());
        String dimension = level.dimension().location().toString();
        if (store.winterSnow.isEmpty() && store.winterIce.isEmpty()) {
            store.winterDimension = dimension;
        }
        if (!store.winterDimension.equals(dimension)) {
            return;
        }
        RandomSource random = level.random;
        double radius = Config.HERALD_RADIUS.get();
        boolean changed = false;
        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = radius * Math.sqrt(random.nextDouble());
            int x = (int) Math.floor(herald.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(herald.getZ() + Math.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos above = new BlockPos(x, top, z);
            BlockPos below = above.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(Blocks.WATER) && belowState.getFluidState().isSource()) {
                level.setBlock(below, Blocks.ICE.defaultBlockState(), 3);
                store.winterIce.add(below.asLong());
                changed = true;
            } else if (level.getBlockState(above).isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, above)) {
                level.setBlock(above, Blocks.SNOW.defaultBlockState(), 3);
                store.winterSnow.add(above.asLong());
                changed = true;
            }
        }
        if (changed) {
            store.setDirty();
        }
    }

    /**
     * Прибирає штучну зиму: лише ті блоки, що й досі такі, якими їх зробив Вісник (сніг в один шар, лід).
     * Що гравець змінив після цього, лишається. Незавантажені місця чекають наступного виклику.
     */
    public static void cleanupWinter(MinecraftServer server, int limit) {
        HeraldStore store = HeraldStore.get(server);
        if (store.winterSnow.isEmpty() && store.winterIce.isEmpty()) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(store.winterDimension);
        ServerLevel level = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) {
            store.winterSnow.clear();
            store.winterIce.clear();
            store.setDirty();
            return;
        }
        int handled = 0;
        Iterator<Long> snow = store.winterSnow.iterator();
        while (snow.hasNext() && handled < limit) {
            BlockPos pos = BlockPos.of(snow.next());
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SNOW) && state.getValue(SnowLayerBlock.LAYERS) == 1) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            snow.remove();
            handled++;
        }
        Iterator<Long> ice = store.winterIce.iterator();
        while (ice.hasNext() && handled < limit) {
            BlockPos pos = BlockPos.of(ice.next());
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            if (level.getBlockState(pos).is(Blocks.ICE)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
            ice.remove();
            handled++;
        }
        store.setDirty();
    }

    // ---------- зони зими (для вигляду опадів) ----------

    /** x, z, радіус і момент (мс), коли зона перестає діяти, якщо її не підтверджувати. */
    private static final Map<UUID, double[]> ZONES = new ConcurrentHashMap<>();

    public static void markWinter(UUID source, double x, double z, double radius) {
        ZONES.put(source, new double[] {x, z, radius, System.currentTimeMillis() + 1500L});
    }

    /** Чи йде в цьому місці штучна зима (читають міксин біома та клієнт). */
    public static boolean isWinterAt(BlockPos pos) {
        if (ZONES.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        for (double[] zone : ZONES.values()) {
            if (zone[3] < now) {
                continue;
            }
            double dx = pos.getX() + 0.5 - zone[0];
            double dz = pos.getZ() + 0.5 - zone[1];
            if (dx * dx + dz * dz <= zone[2] * zone[2]) {
                return true;
            }
        }
        return false;
    }

    public static void clearZones() {
        ZONES.clear();
    }

    // ---------- службове ----------

    /** Забуває дані про гравця, що вийшов. */
    public static void forget(UUID player) {
        LAST_POS.remove(player);
        STEP_UNTIL.remove(player);
        CLEANSED_UNTIL.remove(player);
        SLOWED_FLIERS.remove(player);
    }

    private SpiritHeraldWorld() {
    }
}
