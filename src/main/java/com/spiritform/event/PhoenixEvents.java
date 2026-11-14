package com.spiritform.event;

import com.spiritform.SpiritFormMod;
import com.spiritform.block.PhoenixAshBlock;
import com.spiritform.data.PhoenixNests;
import com.spiritform.entity.Phoenix;
import com.spiritform.form.SpiritBeyond;
import com.spiritform.registry.SpiritCreatures;
import com.spiritform.worldgen.HeatlandSites;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Поява Жар-птиці над вулканом (коли поблизу гравець, жодної дикої немає й перезарядка минула)
 * та наслідки загибелі: перезарядка гнізда й Гнів Перуна для вбивці дикої Жар-птиці.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class PhoenixEvents {

    /** Відстань від вершини, на якій гравець «будить» Жар-птицю. */
    private static final double WAKE_DISTANCE = 160.0;
    /** 20 хвилин. */
    private static final long RESPAWN = 24000L;

    private PhoenixEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(SpiritBeyond.KEY)
                || level.getGameTime() % 100 != 0) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            HeatlandSites.Volcano v = HeatlandSites.nearest(level, player.getX(), player.getZ(), 1);
            if (v == null) {
                continue;
            }
            BlockPos summit = HeatlandSites.summit(v);
            double dx = summit.getX() - player.getX();
            double dz = summit.getZ() - player.getZ();
            if (dx * dx + dz * dz > WAKE_DISTANCE * WAKE_DISTANCE) {
                continue;
            }
            PhoenixNests nests = PhoenixNests.get(level.getServer());
            long key = PhoenixNests.key(v);
            if (now < nests.nextSpawn(key)) {
                continue;
            }
            boolean present = !level.getEntitiesOfClass(Phoenix.class, new net.minecraft.world.phys.AABB(summit).inflate(170.0),
                    p -> !p.isTame() && !p.isReborn()).isEmpty();
            if (present) {
                continue;
            }
            Phoenix phoenix = SpiritCreatures.PHOENIX.get().create(level);
            if (phoenix == null) {
                continue;
            }
            phoenix.moveTo(summit.getX() + 30.0, summit.getY() + 24.0, summit.getZ(), 90.0F, 0.0F);
            phoenix.finalizeSpawn(level, level.getCurrentDifficultyAt(phoenix.blockPosition()), MobSpawnType.EVENT, null);
            level.addFreshEntity(phoenix);
            // Щоб не з'явилася друга, поки перша ще не встигла прокинутися.
            nests.setNext(key, now + 200L);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Phoenix phoenix) || !(phoenix.level() instanceof ServerLevel level)) {
            return;
        }
        if (!phoenix.isReborn()) {
            HeatlandSites.Volcano v = HeatlandSites.nearest(level, phoenix.getX(), phoenix.getZ(), 1);
            if (v != null) {
                PhoenixNests.get(level.getServer()).setNext(PhoenixNests.key(v), level.getGameTime() + RESPAWN);
            }
        }
        ServerPlayer killer = killerOf(event.getSource());
        if (killer != null) {
            PhoenixAshBlock.placeAt(level, phoenix.position());
            SpiritAdvancements.grant(killer, "bestiary/phoenix_ash");
            if (!phoenix.isReborn()) {
                PerunWrath.mark(killer);
            }
        }
    }

    private static ServerPlayer killerOf(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer p) {
            return p;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer p) {
            return p;
        }
        return null;
    }
}
