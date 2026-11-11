package com.spiritform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Клієнтська частина вкраденої зовнішності: для кожного гравця з надягнутим виглядом тримаємо "макет" моба
 * (сутність, якої немає у світі) і малюємо його замість моделі гравця, щокадру перенісши на нього положення,
 * повороти й анімацію гравця.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritDisguiseClient {

    private static final class Dummy {
        LivingEntity entity;
        String key = "";
        long lastTick = -1L;
        boolean failed;
        Level level;
    }

    private static final Map<UUID, Dummy> DUMMIES = new HashMap<>();

    private SpiritDisguiseClient() {
    }

    /** Макет моба для гравця або null, якщо гравець у своєму вигляді (або макет створити не вдалося). */
    public static LivingEntity prepare(AbstractClientPlayer player, float partialTick) {
        DisguiseData data = player.getData(SpiritAttachments.DISGUISE);
        if (!data.active()) {
            if (!DUMMIES.isEmpty()) {
                DUMMIES.remove(player.getUUID());
            }
            return null;
        }
        // Поки гравець сидить у мобі, його тіло не малюється зовсім (це робить інша частина мода)
        if (player.isInvisible() && SpiritFormState.get(player).active()) {
            return null;
        }

        Dummy dummy = DUMMIES.computeIfAbsent(player.getUUID(), id -> new Dummy());
        String key = data.type() + data.nbt().hashCode();
        if (dummy.failed && key.equals(dummy.key)) {
            return null;
        }
        if (dummy.entity == null || !key.equals(dummy.key) || dummy.level != player.level()) {
            dummy.key = key;
            dummy.failed = false;
            dummy.level = player.level();
            dummy.entity = create(data, player.level());
            dummy.lastTick = -1L;
            if (dummy.entity == null) {
                dummy.failed = true;
                return null;
            }
        }

        LivingEntity mob = dummy.entity;
        mob.setPos(player.getX(), player.getY(), player.getZ());
        mob.xo = player.xo;
        mob.yo = player.yo;
        mob.zo = player.zo;
        mob.xOld = player.xOld;
        mob.yOld = player.yOld;
        mob.zOld = player.zOld;
        mob.setYRot(player.getYRot());
        mob.yRotO = player.yRotO;
        mob.setXRot(player.getXRot());
        mob.xRotO = player.xRotO;
        mob.yBodyRot = player.yBodyRot;
        mob.yBodyRotO = player.yBodyRotO;
        mob.yHeadRot = player.yHeadRot;
        mob.yHeadRotO = player.yHeadRotO;
        mob.setDeltaMovement(player.getDeltaMovement());
        mob.setOnGround(player.onGround());
        mob.setPose(player.isCrouching() ? Pose.CROUCHING : Pose.STANDING);
        mob.hurtTime = player.hurtTime;
        mob.hurtDuration = player.hurtDuration;
        mob.deathTime = player.deathTime;
        mob.attackAnim = player.attackAnim;
        mob.oAttackAnim = player.oAttackAnim;
        mob.swinging = player.swinging;
        mob.swingTime = player.swingTime;
        mob.swingingArm = player.swingingArm;
        mob.setInvisible(false);

        // Анімація ходьби просувається раз на тік гри, а не раз на кадр
        if (dummy.lastTick != player.tickCount) {
            dummy.lastTick = player.tickCount;
            mob.tickCount = player.tickCount;
            mob.walkAnimation.update(player.walkAnimation.speed(), 0.4F);
        }
        return mob;
    }

    private static LivingEntity create(DisguiseData data, Level level) {
        ResourceLocation id = ResourceLocation.tryParse(data.type());
        if (id == null) {
            return null;
        }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (type.isEmpty()) {
            return null;
        }
        Entity entity = null;
        try {
            entity = EntityType.create(data.nbt(), level).orElse(null);
        } catch (RuntimeException ignored) {
            // знімок міг не прочитатися: спробуємо простий моб
        }
        if (entity == null) {
            try {
                entity = type.get().create(level);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return entity instanceof LivingEntity living ? living : null;
    }

    /** Вимикає вигляд для гравця, якщо малювання моба впало (щоб гра не вилітала щокадру). */
    public static void fail(AbstractClientPlayer player) {
        Dummy dummy = DUMMIES.get(player.getUUID());
        if (dummy != null) {
            dummy.failed = true;
            dummy.entity = null;
        }
    }

    /** Чи це макет гравця у формі Привида: тоді він малюється напівпрозорим, як сам привид. */
    public static boolean ghostly(Entity entity) {
        if (DUMMIES.isEmpty()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        for (Iterator<Map.Entry<UUID, Dummy>> it = DUMMIES.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Dummy> entry = it.next();
            if (entry.getValue().entity != entity) {
                continue;
            }
            net.minecraft.world.entity.player.Player owner = minecraft.level.getPlayerByUUID(entry.getKey());
            if (owner == null) {
                it.remove();
                return false;
            }
            SpiritData data = SpiritFormState.get(owner);
            return data.active() || data.invisible();
        }
        return false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DUMMIES.clear();
    }
}
