package com.spiritform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Коса Потойбіччя та обладунки Потойбіччя: уся їхня логіка на сервері.
 *
 * Коса: лікування від завданої шкоди, заряд від вбивств (замість душі), ПКМ: коло диму, що зупиняє мобів,
 * а за повного заряду й повного комплекту режим "Жниво" (усі удари смертельні).
 * Корона: "Погляд з порожнечі" (показ здоров'я й ефектів, див. SpiritGearClient).
 * Накидка: "Межа". Штани: "Невловимість". Черевики: "Слід". Комплект: "Жнець Потойбіччя".
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritReaper {

    private static final String FROZEN_KEY = "spirit_form_frozen_until";
    private static final String WAS_NOAI_KEY = "spirit_form_frozen_noai";

    private record TrailPoint(ResourceKey<Level> dimension, Vec3 pos, long until) {
    }

    /** моб -> тік, коли він розморожується */
    private static final Map<UUID, Long> FROZEN = new HashMap<>();
    /** гравець -> тік, до якого його сила помножена */
    private static final Map<UUID, Long> POWER_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> TRAIL_UNTIL = new HashMap<>();
    private static final Map<UUID, List<TrailPoint>> TRAILS = new HashMap<>();
    private static final Set<UUID> EDGE_SHIELD = new HashSet<>();
    private static final Set<UUID> EDGE_STRIKE = new HashSet<>();

    private SpiritReaper() {
    }

    // ---------- предмети на гравцеві ----------

    private static boolean wears(Player player, EquipmentSlot slot, Item item) {
        return player.getItemBySlot(slot).is(item);
    }

    public static boolean hasCrown(Player player) {
        return wears(player, EquipmentSlot.HEAD, SpiritItems.SPIRIT_HELMET.get());
    }

    public static boolean hasCloak(Player player) {
        return wears(player, EquipmentSlot.CHEST, SpiritItems.SPIRIT_CHESTPLATE.get());
    }

    public static boolean hasTrousers(Player player) {
        return wears(player, EquipmentSlot.LEGS, SpiritItems.SPIRIT_LEGGINGS.get());
    }

    public static boolean hasBoots(Player player) {
        return wears(player, EquipmentSlot.FEET, SpiritItems.SPIRIT_BOOTS.get());
    }

    public static boolean fullSet(Player player) {
        return hasCrown(player) && hasCloak(player) && hasTrousers(player) && hasBoots(player);
    }

    public static boolean holdsScythe(Player player) {
        return player.getMainHandItem().is(SpiritItems.SCYTHE.get())
                || player.getOffhandItem().is(SpiritItems.SCYTHE.get());
    }

    public static boolean holdsScytheInMain(Player player) {
        return player.getMainHandItem().is(SpiritItems.SCYTHE.get());
    }

    // ---------- дані Коси (у самому предметі) ----------

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void edit(ItemStack stack, Consumer<CompoundTag> change) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
    }

    public static int charge(ItemStack stack) {
        return tag(stack).getInt("Charge");
    }

    public static boolean active(ItemStack stack) {
        return tag(stack).getBoolean("Active");
    }

    public static long lockUntil(ItemStack stack) {
        return tag(stack).getLong("Lock");
    }

    /** +1 до заряду Коси в основній руці (якщо Коса не в режимі "Жниво" і не заблокована). */
    public static void addCharge(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(SpiritItems.SCYTHE.get())) {
            return;
        }
        long now = player.level().getGameTime();
        int max = Config.SCYTHE_MAX_CHARGE.get();
        int charge = charge(stack);
        if (active(stack) || lockUntil(stack) > now || charge >= max) {
            return;
        }
        edit(stack, t -> t.putInt("Charge", charge + 1));
        player.displayClientMessage(Component.translatable("message.spirit_form.scythe.charge", charge + 1, max), true);
    }

    /** ПКМ із Косою в руці. */
    public static void onUse(ServerPlayer player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level) || active(stack)) {
            return;
        }
        long now = level.getGameTime();
        int max = Config.SCYTHE_MAX_CHARGE.get();
        int charge = charge(stack);
        if (charge <= 0) {
            player.displayClientMessage(Component.translatable("message.spirit_form.scythe.empty"), true);
            return;
        }

        // Повний заряд і повний комплект: Жниво
        if (charge >= max && fullSet(player)) {
            edit(stack, t -> {
                t.putBoolean("Active", true);
                t.putLong("Next", now + 20);
            });
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 0.6F);
            ring(level, player, 4.0, 30);
            player.displayClientMessage(Component.translatable("message.spirit_form.scythe.reaper_on"), true);
            return;
        }

        // Звичайне використання: коло диму
        double radius = Config.SCYTHE_FREEZE_RADIUS.get();
        long freezeUntil = now + Config.SCYTHE_FREEZE_SECONDS.get() * 20L;
        UUID owner = player.getUUID();
        int frozen = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                m -> m.isAlive() && m.distanceToSqr(player) <= radius * radius && !SpiritArmy.isAllyOf(owner, m))) {
            freeze(mob, freezeUntil);
            frozen++;
        }
        POWER_UNTIL.put(owner, freezeUntil);
        edit(stack, t -> t.putInt("Charge", charge - 1));
        ring(level, player, radius, 96);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1.5F, 0.5F);
        player.displayClientMessage(Component.translatable("message.spirit_form.scythe.freeze", frozen, charge - 1, max), true);
    }

    private static void ring(ServerLevel level, Player player, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2.0 / points;
            level.sendParticles(SpiritParticles.MINT, player.getX() + Math.cos(angle) * radius, player.getY() + 0.2,
                    player.getZ() + Math.sin(angle) * radius, 1, 0.0, 0.1, 0.0, 0.0);
        }
        level.sendParticles(SpiritParticles.MINT, player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.5, 0.8, 0.5, 0.02);
    }

    /** Щотіку для Коси в інвентарі: витрата заряду в режимі "Жниво" і зміна моделі. */
    public static void tickScythe(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        int max = Config.SCYTHE_MAX_CHARGE.get();
        CompoundTag data = tag(stack);
        boolean active = data.getBoolean("Active");
        int charge = data.getInt("Charge");

        if (charge > max) {
            edit(stack, t -> t.putInt("Charge", max));
            charge = max;
        }

        if (active && now >= data.getLong("Next")) {
            if (charge - 1 <= 0) {
                long lock = now + Config.SCYTHE_LOCK_SECONDS.get() * 20L;
                edit(stack, t -> {
                    t.putInt("Charge", 0);
                    t.putBoolean("Active", false);
                    t.putLong("Lock", lock);
                });
                active = false;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
                player.displayClientMessage(Component.translatable("message.spirit_form.scythe.reaper_off",
                        Config.SCYTHE_LOCK_SECONDS.get()), true);
            } else {
                int next = charge - 1;
                edit(stack, t -> {
                    t.putInt("Charge", next);
                    t.putLong("Next", now + 20);
                });
            }
        }

        // Інша модель для повного заряду з повним комплектом (і поки триває Жниво)
        boolean full = active || (charge >= max && fullSet(player));
        CustomModelData current = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        boolean shown = current != null && current.value() == 1;
        if (full && !shown) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        } else if (!full && shown) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    // ---------- заморожування мобів ----------

    private static void freeze(Mob mob, long until) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(FROZEN_KEY)) {
            data.putBoolean(WAS_NOAI_KEY, mob.isNoAi());
        }
        data.putLong(FROZEN_KEY, until);
        mob.setNoAi(true);
        mob.getNavigation().stop();
        FROZEN.put(mob.getUUID(), until);
    }

    private static void unfreeze(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(WAS_NOAI_KEY)) {
            mob.setNoAi(false);
        }
        data.remove(FROZEN_KEY);
        data.remove(WAS_NOAI_KEY);
    }

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity found = level.getEntity(id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (FROZEN.isEmpty() || server.getTickCount() % 5 != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = FROZEN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now < entry.getValue()) {
                continue;
            }
            if (find(server, entry.getKey()) instanceof Mob mob) {
                unfreeze(mob);
            }
            it.remove();
        }
    }

    /** Моб, що завантажився зі слідами заморозки (світ перезапущено), одразу розморожується. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob && mob.getPersistentData().contains(FROZEN_KEY)
                && !FROZEN.containsKey(mob.getUUID())) {
            unfreeze(mob);
        }
    }

    // ---------- форма Привида: Межа ----------

    /** Гравець став Привидом: перший удар у цій формі отримає х10 (з Накидкою). */
    public static void onFormEntered(ServerPlayer player) {
        EDGE_SHIELD.remove(player.getUUID());
        if (hasCloak(player)) {
            EDGE_STRIKE.add(player.getUUID());
        }
    }

    /** Гравець став людиною: перша шкода буде проігнорована, а форма отримає перезарядку (з Накидкою). */
    public static void onFormLeft(ServerPlayer player) {
        EDGE_STRIKE.remove(player.getUUID());
        if (hasCloak(player)) {
            EDGE_SHIELD.add(player.getUUID());
            SpiritForm.extendCooldown(player, Config.FORM_EDGE_COOLDOWN_SECONDS.get() * 20L);
        }
    }

    // ---------- вбивства: Невловимість, Слід ----------

    public static void onKill(ServerPlayer player, Mob victim) {
        long now = player.level().getGameTime();
        int ticks = Config.GEAR_PERK_SECONDS.get() * 20;
        if (hasTrousers(player)) {
            // Швидкість у два рази більша: Швидкість V дає +100%
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 4, false, true));
        }
        if (hasBoots(player)) {
            TRAIL_UNTIL.put(player.getUUID(), now + ticks);
        }
    }

    // ---------- щотіку для гравця ----------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        UUID id = player.getUUID();
        long now = level.getGameTime();

        // Коса не дає бути Привидом
        if (player.tickCount % 5 == 0 && SpiritFormState.isActive(player) && holdsScythe(player)) {
            if (SpiritPossession.isPossessing(player)) {
                SpiritPossession.drainAndExit(player);
            } else {
                SpiritForm.deactivate(player, "message.spirit_form.scythe.no_form");
            }
        }

        // Черевики: вогонь не шкодить
        if (player.tickCount % 10 == 0 && player.isOnFire() && hasBoots(player)) {
            player.clearFire();
        }

        // Слід
        Long until = TRAIL_UNTIL.get(id);
        if (until != null) {
            if (now >= until || !hasBoots(player)) {
                TRAIL_UNTIL.remove(id);
            } else if (player.tickCount % 2 == 0) {
                List<TrailPoint> list = TRAILS.computeIfAbsent(id, key -> new ArrayList<>());
                Vec3 pos = player.position();
                if (list.isEmpty() || list.get(list.size() - 1).pos().distanceToSqr(pos) > 0.16) {
                    list.add(new TrailPoint(level.dimension(), pos, now + 100));
                }
            }
        }
        List<TrailPoint> points = TRAILS.get(id);
        if (points != null) {
            points.removeIf(p -> p.until() <= now || !p.dimension().equals(level.dimension()));
            if (points.isEmpty()) {
                TRAILS.remove(id);
            } else {
                if (player.tickCount % 2 == 0) {
                    for (TrailPoint p : points) {
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.pos().x, p.pos().y + 0.1, p.pos().z,
                                1, 0.2, 0.1, 0.2, 0.01);
                    }
                }
                if (player.tickCount % 10 == 0) {
                    Set<Mob> burned = new HashSet<>();
                    for (TrailPoint p : points) {
                        AABB box = new AABB(p.pos().x - 0.7, p.pos().y - 0.2, p.pos().z - 0.7,
                                p.pos().x + 0.7, p.pos().y + 1.8, p.pos().z + 0.7);
                        burned.addAll(level.getEntitiesOfClass(Mob.class, box,
                                m -> m.isAlive() && !SpiritArmy.isAllyOf(id, m)));
                    }
                    for (Mob mob : burned) {
                        mob.hurt(level.damageSources().inFire(), 2.0F);
                    }
                }
            }
        }
    }

    // ---------- шкода ----------

    private static boolean lifeless(LivingEntity target) {
        return target.getType().is(EntityTypeTags.UNDEAD) || target instanceof IronGolem || target instanceof SnowGolem;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        long now = victim.level().getGameTime();

        if (victim instanceof ServerPlayer player) {
            // Накидка: перша шкода після виходу з форми Привида ігнорується
            if (EDGE_SHIELD.contains(player.getUUID()) && hasCloak(player)
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                EDGE_SHIELD.remove(player.getUUID());
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("message.spirit_form.cloak.shield"), true);
                return;
            }
            // Черевики: вогонь не шкодить
            if (hasBoots(player) && source.is(DamageTypeTags.IS_FIRE)) {
                event.setCanceled(true);
                return;
            }
        }

        if (source.getEntity() instanceof ServerPlayer attacker && !SpiritArmy.isAllyOf(attacker.getUUID(), victim)) {
            float amount = event.getAmount();
            // Накидка: перший удар у формі Привида х10
            if (EDGE_STRIKE.contains(attacker.getUUID()) && hasCloak(attacker)) {
                EDGE_STRIKE.remove(attacker.getUUID());
                amount *= 10.0F;
            }
            // Сила від Коси
            Long power = POWER_UNTIL.get(attacker.getUUID());
            if (power != null) {
                if (now < power) {
                    amount *= Config.SCYTHE_POWER_MULTIPLIER.get().floatValue();
                } else {
                    POWER_UNTIL.remove(attacker.getUUID());
                }
            }
            // Жниво: удар Косою смертельний
            if (source.getDirectEntity() == attacker && attacker.getMainHandItem().is(SpiritItems.SCYTHE.get())
                    && active(attacker.getMainHandItem())) {
                amount = 1.0E6F;
            }
            event.setAmount(amount);
        }
    }

    /** Коса лікує на частину реально завданої шкоди (не більше, ніж лишалося здоров'я цілі) і трохи годує. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !player.getMainHandItem().is(SpiritItems.SCYTHE.get()) || lifeless(target)
                || SpiritArmy.isAllyOf(player.getUUID(), target)) {
            return;
        }
        float effective = Math.max(0.0F, event.getNewDamage() - target.getAbsorptionAmount());
        float applied = Math.min(effective, target.getHealth());
        if (applied <= 0.0F) {
            return;
        }
        double min = Config.SCYTHE_HEAL_MIN_PERCENT.get() / 100.0;
        double max = Math.max(min, Config.SCYTHE_HEAL_MAX_PERCENT.get() / 100.0);
        float share = (float) (min + player.getRandom().nextDouble() * (max - min));
        player.heal(applied * share);
        player.getFoodData().eat(1, 0.0F);
    }

    // ---------- вихід з гри ----------

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        POWER_UNTIL.remove(id);
        TRAIL_UNTIL.remove(id);
        TRAILS.remove(id);
        EDGE_SHIELD.remove(id);
        EDGE_STRIKE.remove(id);
    }
}
