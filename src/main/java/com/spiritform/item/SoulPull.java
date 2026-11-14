package com.spiritform.item;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Логіка Тяжіння Душі: щодругого тіку підтягує предмети й кульки досвіду до гравця. */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SoulPull {

    private static final double MAX_SPEED = 0.55;
    private static final double MIN_SPEED = 0.12;

    private SoulPull() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 2 != 0
                || player.isSpectator() || !player.isAlive() || !SoulPullItem.isWorn(player)) {
            return;
        }

        double radius = Config.SOUL_PULL_RADIUS.get();
        AABB box = player.getBoundingBox().inflate(radius);
        Vec3 target = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);

        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, box,
                item -> !item.hasPickUpDelay() && !item.getItem().isEmpty());
        for (ItemEntity item : items) {
            pull(item, target, radius);
        }
        List<ExperienceOrb> orbs = player.level().getEntitiesOfClass(ExperienceOrb.class, box);
        for (ExperienceOrb orb : orbs) {
            pull(orb, target, radius);
        }
    }

    private static void pull(Entity entity, Vec3 target, double radius) {
        Vec3 delta = target.subtract(entity.position());
        double distance = delta.length();
        if (distance < 0.6 || distance > radius + 1.0) {
            return;
        }
        // Чим ближче, тим повільніше, щоб предмет не пролітав повз гравця
        double speed = Math.min(MAX_SPEED, MIN_SPEED + distance * 0.04);
        entity.setDeltaMovement(delta.scale(speed / distance));
        entity.hasImpulse = true;
    }
}
