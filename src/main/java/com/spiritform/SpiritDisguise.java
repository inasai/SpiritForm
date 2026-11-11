package com.spiritform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Вкрадена зовнішність: Одержимість -> (сидячи в мобі) Марення. Моб помирає, а гравець забирає його вигляд.
 * Модель гравця для всіх замінюється на цього моба, а інші моби ставляться до гравця так, як до моба цього виду
 * (див. {@link #hostileTo}). Вигляд зберігається, доки гравець не вкраде інший. Щоб надягти чи зняти його,
 * треба 5 секунд утримувати клавішу Марення; коротке натискання працює як звичайне Марення.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritDisguise {

    /** Скільки тіків утримувати клавішу: 5 секунд. */
    public static final long HOLD_TICKS = 100L;

    private static final class Press {
        final long start;
        boolean consumed;

        Press(long start) {
            this.start = start;
        }
    }

    private static final Map<UUID, Press> PRESSES = new HashMap<>();

    private static final Set<EntityType<?>> BLOCKED = Set.of(
            EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.PLAYER, EntityType.ARMOR_STAND);

    private SpiritDisguise() {
    }

    public static DisguiseData get(ServerPlayer player) {
        return player.getData(SpiritAttachments.DISGUISE);
    }

    private static void set(ServerPlayer player, DisguiseData data) {
        player.setData(SpiritAttachments.DISGUISE, data);
    }

    public static void clear(ServerPlayer player) {
        PRESSES.remove(player.getUUID());
    }

    // ---------- клавіша Марення ----------

    public static void onDelirium(ServerPlayer player, boolean pressed) {
        UUID id = player.getUUID();
        if (!pressed) {
            Press press = PRESSES.remove(id);
            if (press != null && !press.consumed) {
                SpiritDelirium.activate(player); // коротке натискання - звичайне Марення
            }
            return;
        }

        if (SpiritPossession.isPossessing(player) && SpiritFormState.get(player).hasAbility(SpiritAbility.DELIRIUM)) {
            steal(player);
            return;
        }
        if (!get(player).stored()) {
            SpiritDelirium.activate(player); // вигляду немає - усе як раніше
            return;
        }
        PRESSES.put(id, new Press(player.level().getGameTime()));
    }

    /** Щотіку: відлік утримування клавіші Марення. */
    public static void tick(ServerPlayer player) {
        if (PRESSES.isEmpty()) {
            return;
        }
        Press press = PRESSES.get(player.getUUID());
        if (press == null) {
            return;
        }
        long elapsed = player.level().getGameTime() - press.start;
        if (elapsed > 400L) {
            PRESSES.remove(player.getUUID()); // клавішу, схоже, давно відпустили
            return;
        }
        if (press.consumed) {
            return;
        }
        if (elapsed >= HOLD_TICKS) {
            press.consumed = true;
            toggleWorn(player);
        } else if (elapsed >= 20L && elapsed % 5L == 0L) {
            player.displayClientMessage(Component.translatable("message.spirit_form.disguise.progress",
                    elapsed * 100L / HOLD_TICKS), true);
        }
    }

    // ---------- крадіжка й надягання ----------

    private static void steal(ServerPlayer player) {
        Mob mob = SpiritPossession.possessedMob(player);
        if (mob == null) {
            return;
        }
        EntityType<?> type = mob.getType();
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (BLOCKED.contains(type) || !type.canSerialize() || key == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.disguise.blocked"), true);
            return;
        }

        CompoundTag tag = new CompoundTag();
        mob.saveWithoutId(tag);
        for (String drop : new String[] {"UUID", "Pos", "Motion", "Rotation", "Passengers", "Leash", "Brain",
                "Attributes", "ActiveEffects", "Air", "Fire", "FallDistance", "Health", "HurtTime", "HurtByTimestamp",
                "DeathTime", "Tags", "ForgeData", "neoforge:attachments", "AbsorptionAmount", "LeftHanded",
                "AngryAt", "Owner", "Target", "home_pos", "PersistenceRequired"}) {
            tag.remove(drop);
        }
        tag.putString("id", key.toString());

        Component name = mob.getDisplayName();
        ServerLevel level = player.serverLevel();
        double x = mob.getX();
        double y = mob.getY();
        double z = mob.getZ();

        SpiritPossession.release(player);
        mob.kill();
        SpiritExistence.onPossessionEnded(player, mob);
        level.sendParticles(ParticleTypes.SMOKE, x, y + mob.getBbHeight() * 0.5, z, 40, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, x, y, z, SoundEvents.GHAST_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.6F);

        set(player, new DisguiseData(key.toString(), tag, true));
        player.displayClientMessage(Component.translatable("message.spirit_form.disguise.stolen", name), true);
    }

    private static void toggleWorn(ServerPlayer player) {
        DisguiseData data = get(player);
        if (!data.stored()) {
            return;
        }
        boolean worn = !data.worn();
        set(player, data.withWorn(worn));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.9, player.getZ(),
                30, 0.4, 0.8, 0.4, 0.04);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GHAST_AMBIENT, SoundSource.PLAYERS, 0.7F, worn ? 0.6F : 1.4F);
        EntityType<?> type = type(data);
        player.displayClientMessage(worn
                ? Component.translatable("message.spirit_form.disguise.worn", type == null ? Component.literal("?") : type.getDescription())
                : Component.translatable("message.spirit_form.disguise.off"), true);
    }

    public static EntityType<?> type(DisguiseData data) {
        ResourceLocation id = ResourceLocation.tryParse(data.type());
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    // ---------- як до гравця ставляться моби ----------

    /** Чи сприйняв би цей моб істоту такого виду як ворога (за звичайними правилами вибору цілей гри). */
    private static boolean hostileTo(Mob mob, EntityType<?> look) {
        if (mob instanceof IronGolem || mob instanceof SnowGolem) {
            return look.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER && look != EntityType.CREEPER;
        }
        if (mob instanceof Zombie) {
            return look == EntityType.VILLAGER || look == EntityType.IRON_GOLEM || look == EntityType.TURTLE;
        }
        if (mob instanceof AbstractIllager || mob instanceof Ravager) {
            return look == EntityType.VILLAGER || look == EntityType.WANDERING_TRADER || look == EntityType.IRON_GOLEM;
        }
        if (mob instanceof AbstractSkeleton || mob instanceof Spider) {
            return look == EntityType.IRON_GOLEM || look == EntityType.TURTLE;
        }
        if (mob instanceof Wolf) {
            return look == EntityType.SHEEP || look == EntityType.RABBIT || look == EntityType.FOX
                    || look == EntityType.SKELETON || look == EntityType.STRAY || look == EntityType.BOGGED;
        }
        if (mob instanceof Fox) {
            return look == EntityType.CHICKEN || look == EntityType.RABBIT || look == EntityType.COD
                    || look == EntityType.SALMON || look == EntityType.TROPICAL_FISH || look == EntityType.TURTLE;
        }
        if (mob instanceof Cat) {
            return look == EntityType.RABBIT || look == EntityType.TURTLE;
        }
        if (mob instanceof Ocelot) {
            return look == EntityType.CHICKEN || look == EntityType.TURTLE;
        }
        return false;
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        DisguiseData data = get(player);
        if (!data.active()) {
            return;
        }
        EntityType<?> look = type(data);
        if (look == null) {
            return;
        }
        // Якщо гравець щойно сам вдарив моба, моб захищається, хоч би яким було обличчя
        if (mob.getLastHurtByMob() == player && mob.tickCount - mob.getLastHurtByMobTimestamp() < 200) {
            return;
        }
        if (!hostileTo(mob, look)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** Моби, що полюють на вигляд, який носить гравець, помічають його самі (наприклад, зомбі - селянина). */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().getGameTime() % 20L != 7L) {
            return;
        }
        DisguiseData data = get(player);
        if (!data.active()) {
            return;
        }
        EntityType<?> look = type(data);
        if (look == null) {
            return;
        }
        List<Mob> mobs = player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(14.0),
                m -> m.getTarget() == null && m.isAlive() && !(m instanceof net.minecraft.world.entity.animal.Animal && m.isBaby()));
        for (Mob mob : mobs) {
            if (hostileTo(mob, look) && mob.hasLineOfSight(player) && !SpiritInvisibility.isActive(player)
                    && !SpiritPossession.isPossessing(player)) {
                mob.setTarget(player);
            }
        }
    }
}
