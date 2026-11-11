package com.spiritform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Чуття Привида: слід темного диму веде до найближчого Уламка Душі (а не до місця смерті), навіть якщо той за блоком.
 * Дим пускається з боку гравця в бік уламка і дрейфує в тому ж напрямку.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritShardTrail {

    /** Найдальша точка сліду від гравця. */
    private static final double REACH = 10.0;
    /** Ближче цієї відстані слід не потрібен: уламок поруч. */
    private static final double ARRIVED = 2.5;

    private SpiritShardTrail() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused() || !SpiritClientHighlight.isEnabled()
                || !SpiritFormState.get(player).hasAbility(SpiritAbility.SENSE)) {
            return;
        }

        String dimension = mc.level.dimension().location().toString();
        Vec3 eye = player.getEyePosition();
        Vec3 target = null;
        double best = Double.MAX_VALUE;
        for (ShardMarks.Mark mark : player.getData(SpiritAttachments.SHARD_MARKS.get()).marks()) {
            if (!mark.dimension().equals(dimension)) {
                continue;
            }
            Vec3 point = new Vec3(mark.x(), mark.y() + 0.6, mark.z());
            double distance = point.distanceToSqr(eye);
            if (distance < best) {
                best = distance;
                target = point;
            }
        }
        if (target == null) {
            return;
        }
        double distance = Math.sqrt(best);
        if (distance < ARRIVED) {
            return;
        }

        Vec3 direction = target.subtract(eye).scale(1.0 / distance);
        RandomSource random = mc.level.random;
        double reach = Math.min(REACH, distance - 1.0);
        for (int i = 0; i < 3; i++) {
            double along = 1.5 + random.nextDouble() * Math.max(0.5, reach - 1.5);
            Vec3 base = eye.add(direction.scale(along)).add(0.0, -0.4, 0.0);
            double x = base.x + (random.nextDouble() - 0.5) * 0.3;
            double y = base.y + (random.nextDouble() - 0.5) * 0.3;
            double z = base.z + (random.nextDouble() - 0.5) * 0.3;
            mc.level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    x, y, z, direction.x * 0.06, direction.y * 0.06, direction.z * 0.06);
        }
    }
}
