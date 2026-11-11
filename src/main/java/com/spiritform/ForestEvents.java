package com.spiritform;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.network.chat.Component;

/**
 * Події лісу Потойбіччя: Мавки бачать смерть Чугайстра, Скарбники з'являються біля Бісиків, Злидні біля Хух,
 * а біля Титанічного дерева частіше з'являються Бісики й Чугайстри.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class ForestEvents {

    /** Радіус, у якому Мавка помічає смерть Чугайстра. */
    private static final double WITNESS_RANGE = 24.0;

    private ForestEvents() {
    }

    // ===== Прихильність Мавок =====

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Chuhaister chuhaister)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !(chuhaister.level() instanceof ServerLevel level)) {
            return;
        }
        List<Mavka> witnesses = level.getEntitiesOfClass(Mavka.class,
                chuhaister.getBoundingBox().inflate(WITNESS_RANGE),
                m -> m.isAlive() && m.hasLineOfSight(chuhaister));
        for (Mavka mavka : witnesses) {
            mavka.befriend(player);
        }
    }

    // ===== Скарбники біля Бісиків, Злидні біля Хух =====

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        Mob mob = event.getEntity();
        // Увага: під час генерації чанка цей код працює в потоці генерації, тож чіпати можна лише
        // event.getLevel() (область генерації), а не живий ServerLevel: інакше сервер зависає.
        ServerLevelAccessor level = event.getLevel();
        RandomSource random = mob.getRandom();
        if (mob instanceof Bisyk && random.nextFloat() < 0.5F && claimSkarbnykCell(level, mob.blockPosition())) {
            spawnCompanion(level, SpiritCreatures.SKARBNYK.get(), mob.blockPosition(), 20.0, 2);
        } else if (mob instanceof Hukha && random.nextFloat() < 0.08F) {
            spawnCompanion(level, SpiritCreatures.ZLYDENY.get(), mob.blockPosition(), 40.0, 1);
        }
    }

    /** Клітинки 64x64, у яких Скарбник уже з'явився (під час генерації світу список істот області порожній, тож лічильник ведемо самі). */
    private static final java.util.Set<Long> SKARBNYK_CELLS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** У кожній клітинці 64x64 Скарбник може з'явитися поруч із Бісиками лише один раз за сеанс. */
    private static boolean claimSkarbnykCell(ServerLevelAccessor level, BlockPos pos) {
        if (SKARBNYK_CELLS.size() > 20000) {
            SKARBNYK_CELLS.clear();
        }
        long key = ((long) (pos.getX() >> 6) << 32) ^ ((pos.getZ() >> 6) & 0xFFFFFFFFL);
        return SKARBNYK_CELLS.add(key);
    }

    /** Створює істоту поруч, якщо таких довкола ще менше за limit. Безпечно для потоку генерації світу. */
    private static <T extends Mob> void spawnCompanion(ServerLevelAccessor level, EntityType<T> type, BlockPos near,
                                                       double range, int limit) {
        if (level.getDifficulty() == Difficulty.PEACEFUL && type == SpiritCreatures.ZLYDENY.get()) {
            return;
        }
        AABB box = new AABB(near).inflate(range, range / 2.0, range);
        if (level.getEntitiesOfClass(Mob.class, box, m -> m.getType() == type).size() >= limit) {
            return;
        }
        RandomSource random = level.getRandom();
        int x = near.getX() + random.nextInt(7) - 3;
        int z = near.getZ() + random.nextInt(7) - 3;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        T mob = type.create(level.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
        level.addFreshEntity(mob);
    }

    // ===== Форма Привида не діє біля Титанічного дерева =====

    /** Якщо гравець у формі залетів у радіус дерева, форма вимикається. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0
                && SpiritFormState.isActive(player) && TitanTrees.blocksForm(player)) {
            SpiritForm.toggle(player);
            player.displayClientMessage(Component.translatable("message.spirit_form.titan.no_form"), true);
        }
    }

    // ===== Підсилений спавн біля Титанічного дерева =====

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(SpiritBeyond.KEY)
                || level.getGameTime() % 100 != 0 || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            TitanTrees.Site site = TitanTrees.nearestValid(level, player.getBlockX(), player.getBlockZ(), 100);
            if (site == null) {
                continue;
            }
            BlockPos center = site.center();
            AABB area = new AABB(center).inflate(90.0, 80.0, 90.0);
            if (random.nextFloat() < 0.5F && level.getEntitiesOfClass(Bisyk.class, area).size() < 14) {
                int n = 3 + random.nextInt(3);
                for (int i = 0; i < n; i++) {
                    BlockPos p = around(level, player, center, 20, 60, 2, 6);
                    if (p != null) {
                        spawn(level, SpiritCreatures.BISYK.get(), p);
                    }
                }
                BlockPos p = around(level, player, center, 20, 60, 0, 1);
                if (p != null && random.nextFloat() < 0.5F
                        && level.getEntitiesOfClass(Skarbnyk.class, area).size() < 3) {
                    spawn(level, SpiritCreatures.SKARBNYK.get(), p);
                }
            }
            if (random.nextFloat() < 0.15F && level.getEntitiesOfClass(Chuhaister.class, area.inflate(30.0)).size() < 3) {
                BlockPos p = around(level, player, center, 40, 75, 0, 1);
                if (p != null) {
                    spawn(level, SpiritCreatures.CHUHAISTER.get(), p);
                }
            }
        }
    }

    /** Місце на відстані min..max від центру дерева й не ближче 24 блоків до гравця, або null. */
    private static BlockPos around(ServerLevel level, ServerPlayer player, BlockPos center, int min, int max,
                                   int liftMin, int liftMax) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = min + random.nextDouble() * (max - min);
        int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
        int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);
        if (!level.hasChunkAt(new BlockPos(x, 64, z))) {
            return null;
        }
        if (player.distanceToSqr(x, player.getY(), z) < 24.0 * 24.0) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + liftMin
                + (liftMax > liftMin ? random.nextInt(liftMax - liftMin) : 0);
        return new BlockPos(x, y, z);
    }

    private static <T extends Mob> void spawn(ServerLevel level, EntityType<T> type, BlockPos pos) {
        T mob = type.create(level);
        if (mob == null) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(mob)) {
            mob.discard();
            return;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        level.addFreshEntity(mob);
    }
}
