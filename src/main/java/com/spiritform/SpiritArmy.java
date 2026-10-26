package com.spiritform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Армія Потойбіччя. Вбиті істоти лишають м'ятну душу (SoulWispEntity), гравець забирає її до сховища
 * (SpiritArmyStore) і потім викликає: викликаний моб це точна копія вбитого (зі збереженого NBT).
 * Накази: охороняти, атакувати, позиція (відступити повертає всіх до сховища).
 * Викликані моби живуть лише в пам'яті сервера й не лишаються у світі після виходу власника чи перезапуску.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritArmy {

    public enum Order {
        GUARD,
        ATTACK,
        HOLD,
        /** Охороняти й атакувати разом, але з "упертістю": ціль не міняється, поки жива. */
        FURY
    }

    public static final int ACT_SUMMON = 0;
    public static final int ACT_ORDER = 1;
    public static final int ACT_DELETE = 2;
    /** Відступ: усі викликані повертаються до сховища. */
    public static final int ACT_RECALL = 3;
    /** Номер "наказу" відступу: це не стан, а дія. */
    public static final int ORDER_RETREAT = 4;

    private static final String OWNER_KEY = "spirit_form_army_owner";
    private static final String SOUL_KEY = "spirit_form_army_soul";

    /** Скільки тіків пам'ятаємо, кого вдарив гравець чи хто вдарив його. */
    private static final int MEMORY_TICKS = 200;

    /** власник -> (душа -> моб) */
    private static final Map<UUID, Map<UUID, UUID>> SUMMONED = new HashMap<>();
    private static final Map<UUID, Order> ORDERS = new HashMap<>();
    /** моб -> ціль, яку йому дозволено мати (все інше скасовуємо) */
    private static final Map<UUID, UUID> APPROVED = new HashMap<>();
    /** моб -> точка, де він стоїть за наказом "Позиція" */
    private static final Map<UUID, Vec3> ANCHORS = new HashMap<>();

    private static boolean spawning;

    private SpiritArmy() {
    }

    // ---------- допоміжне ----------

    public static boolean has(ServerPlayer player) {
        return SpiritFormState.get(player).hasAbility(SpiritAbility.ARMY);
    }

    public static Order order(UUID owner) {
        return ORDERS.getOrDefault(owner, Order.GUARD);
    }

    /** Власник, якщо цей моб викликаний армією. */
    private static UUID ownerOf(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(OWNER_KEY) ? data.getUUID(OWNER_KEY) : null;
    }

    public static boolean isAllyOf(UUID owner, Entity other) {
        return other.getUUID().equals(owner) || owner.equals(ownerOf(other));
    }

    private static Entity entity(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity found = level.getEntity(id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean isSummoned(MinecraftServer server, UUID owner, UUID soul) {
        Map<UUID, UUID> map = SUMMONED.get(owner);
        UUID id = map == null ? null : map.get(soul);
        if (id == null) {
            return false;
        }
        Entity found = entity(server, id);
        return found != null && found.isAlive();
    }

    static void puff(ServerLevel level, Entity entity, int count) {
        level.sendParticles(SpiritParticles.MINT, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                count, entity.getBbWidth() / 2.0, entity.getBbHeight() / 3.0, entity.getBbWidth() / 2.0, 0.0);
    }

    public static Component typeName(String type) {
        ResourceLocation id = ResourceLocation.tryParse(type);
        if (id != null) {
            return EntityType.byString(type).map(EntityType::getDescription).orElse(Component.literal(type));
        }
        return Component.literal(type);
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity.getType().is(Tags.EntityTypes.BOSSES) || entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    // ---------- синхронізація ----------

    public static void sync(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID owner = player.getUUID();
        List<ArmySyncPayload.Entry> list = new ArrayList<>();
        for (SpiritArmyStore.Soul soul : SpiritArmyStore.get(server).list(owner)) {
            list.add(new ArmySyncPayload.Entry(soul.id, soul.type, soul.name,
                    Math.round(soul.maxHealth), isSummoned(server, owner, soul.id)));
        }
        PacketDistributor.sendToPlayer(player, new ArmySyncPayload(list, order(owner).ordinal()));
    }

    // ---------- збір душ ----------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (victim instanceof ServerPlayer dying) {
            recallAll(dying, false);
            return;
        }
        if (ownerOf(victim) != null) {
            forget(victim);
            return;
        }
        if (!(victim instanceof Mob mobVictim)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Перки обладунків за вбивство (Невловимість, Слід)
        SpiritReaper.onKill(player, mobVictim);

        // Чи мала б з'явитися душа: бос завжди, повний комплект ("Жнець Потойбіччя") завжди, інакше за шансом
        boolean scytheKill = event.getSource().getDirectEntity() == player
                && player.getMainHandItem().is(SpiritItems.SCYTHE.get());
        boolean armyOwner = has(player);
        if (!scytheKill && (!armyOwner || !victim.getType().canSerialize())) {
            return;
        }
        boolean soul = isBoss(victim) || SpiritReaper.fullSet(player)
                || level.random.nextDouble() < Config.ARMY_SOUL_CHANCE.get();
        if (!soul) {
            return;
        }
        if (scytheKill) {
            // Замість душі Коса отримує заряд
            SpiritReaper.addCharge(player);
            return;
        }

        SoulWispEntity wisp = new SoulWispEntity(SpiritEntities.SOUL_WISP.get(), level);
        double y = Math.max(victim.getY(), level.getMinBuildHeight() + 1.0);
        wisp.moveTo(victim.getX(), y, victim.getZ(), 0.0F, 0.0F);
        wisp.setSoul(player.getUUID(), makeSoul(victim), Config.ARMY_WISP_SECONDS.get() * 20);
        level.addFreshEntity(wisp);
        puff(level, wisp, 10);
    }

    /** Знімок моба: тип, ім'я, здоров'я та повний NBT (без того, що пов'язане зі смертю чи конкретним світом). */
    private static CompoundTag makeSoul(LivingEntity victim) {
        CompoundTag nbt = new CompoundTag();
        victim.saveWithoutId(nbt);
        for (String key : new String[] {"UUID", "Passengers", "Leash", "leash", "active_effects", "Motion", "AngryAt",
                "DragonDeathTime", "DeathTime", "HurtTime", "HurtByTimestamp", "Brain", "DeathLootTable",
                "DeathLootTableSeed"}) {
            nbt.remove(key);
        }
        nbt.putFloat("Health", victim.getMaxHealth());
        nbt.putShort("Fire", (short) -1);
        nbt.putFloat("AbsorptionAmount", 0.0F);

        CompoundTag soul = new CompoundTag();
        soul.putString("type", EntityType.getKey(victim.getType()).toString());
        soul.putString("name", victim.hasCustomName() && victim.getCustomName() != null
                ? victim.getCustomName().getString() : "");
        soul.putFloat("hp", victim.getMaxHealth());
        soul.put("nbt", nbt);
        return soul;
    }

    /** ПКМ по м'ятному диму. true: душу взято, дим можна прибрати. */
    public static boolean capture(ServerPlayer player, CompoundTag soulTag) {
        MinecraftServer server = player.getServer();
        if (server == null || !has(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.army.locked"), true);
            return false;
        }
        SpiritArmyStore store = SpiritArmyStore.get(server);
        List<SpiritArmyStore.Soul> list = store.list(player.getUUID());
        if (list.size() >= SpiritFormState.get(player).armySlots()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.army.full"), true);
            return false;
        }
        list.add(SpiritArmyStore.Soul.fromTag(UUID.randomUUID(), soulTag));
        store.setDirty();
        sync(player);
        player.displayClientMessage(Component.translatable("message.spirit_form.army.saved"), true);
        SpiritAdvancements.grant(player, "retinue");
        return true;
    }

    // ---------- дії гравця ----------

    public static void handle(ServerPlayer player, int action, String arg) {
        if (!has(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.army.locked"), true);
            return;
        }
        switch (action) {
            case ACT_SUMMON -> summonAction(player, arg);
            case ACT_ORDER -> orderAction(player, arg);
            case ACT_DELETE -> deleteAction(player, arg);
            case ACT_RECALL -> recallAll(player, true);
            default -> {
            }
        }
    }

    private static void summonAction(ServerPlayer player, String arg) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID owner = player.getUUID();
        List<SpiritArmyStore.Soul> souls = SpiritArmyStore.get(server).list(owner);
        if (souls.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.army.empty"), true);
            return;
        }
        // Повторне клацання по викликаній душі відкликає її
        if (!arg.isEmpty()) {
            for (SpiritArmyStore.Soul soul : souls) {
                if (soul.id.toString().equals(arg) && isSummoned(server, owner, soul.id)) {
                    recallOne(player, soul.id);
                    return;
                }
            }
        }
        int count = 0;
        boolean already = false;
        for (SpiritArmyStore.Soul soul : new ArrayList<>(souls)) {
            if (!arg.isEmpty() && !soul.id.toString().equals(arg)) {
                continue;
            }
            if (isSummoned(server, owner, soul.id)) {
                already = true;
                continue;
            }
            if (summon(player, soul)) {
                count++;
            }
        }
        if (count > 0) {
            player.displayClientMessage(Component.translatable("message.spirit_form.army.summoned", count), true);
        } else if (already) {
            player.displayClientMessage(Component.translatable("message.spirit_form.army.already"), true);
        }
        sync(player);
    }

    private static void orderAction(ServerPlayer player, String arg) {
        int index;
        try {
            index = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return;
        }
        if (index == ORDER_RETREAT) {
            recallAll(player, true);
            return;
        }
        if (index < 0 || index >= Order.values().length) {
            return;
        }
        Order order = Order.values()[index];
        UUID owner = player.getUUID();
        ORDERS.put(owner, order);

        Map<UUID, UUID> map = SUMMONED.get(owner);
        if (map != null && order == Order.HOLD) {
            for (UUID id : map.values()) {
                Entity minion = entity(player.getServer(), id);
                if (minion != null) {
                    ANCHORS.put(id, minion.position());
                }
            }
        }
        player.displayClientMessage(Component.translatable("message.spirit_form.army.order",
                Component.translatable("order.spirit_form." + order.name().toLowerCase())), true);
        sync(player);
    }

    private static void deleteAction(ServerPlayer player, String arg) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID owner = player.getUUID();
        SpiritArmyStore store = SpiritArmyStore.get(server);
        Iterator<SpiritArmyStore.Soul> it = store.list(owner).iterator();
        while (it.hasNext()) {
            SpiritArmyStore.Soul soul = it.next();
            if (!soul.id.toString().equals(arg)) {
                continue;
            }
            Map<UUID, UUID> map = SUMMONED.get(owner);
            UUID minionId = map == null ? null : map.remove(soul.id);
            if (minionId != null) {
                Entity minion = entity(server, minionId);
                if (minion != null) {
                    if (minion.level() instanceof ServerLevel level) {
                        puff(level, minion, 12);
                    }
                    minion.discard();
                }
            }
            it.remove();
            store.setDirty();
            player.displayClientMessage(Component.translatable("message.spirit_form.army.deleted"), true);
            break;
        }
        sync(player);
    }

    // ---------- виклик і відступ ----------

    private static boolean summon(ServerPlayer player, SpiritArmyStore.Soul soul) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        CompoundTag tag = soul.nbt.copy();
        tag.putString("id", soul.type);

        Mob mob;
        spawning = true;
        try {
            Entity created = EntityType.create(tag, level).orElse(null);
            if (!(created instanceof Mob made)) {
                return false;
            }
            mob = made;
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double distance = 1.5 + level.random.nextDouble() * 1.5;
            mob.moveTo(player.getX() + Math.cos(angle) * distance, player.getY(),
                    player.getZ() + Math.sin(angle) * distance, player.getYRot(), 0.0F);
            mob.setPersistenceRequired();
            mob.setCanPickUpLoot(false);
            mob.setTarget(null);
            if (mob instanceof Warden warden) {
                keepWardenUp(warden, null);
            }
            mob.getPersistentData().putUUID(OWNER_KEY, player.getUUID());
            mob.getPersistentData().putUUID(SOUL_KEY, soul.id);
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION,
                    0, true, false));
            if (!level.addFreshEntity(mob)) {
                return false;
            }
        } finally {
            spawning = false;
        }

        SUMMONED.computeIfAbsent(player.getUUID(), key -> new HashMap<>()).put(soul.id, mob.getUUID());
        if (order(player.getUUID()) == Order.HOLD) {
            ANCHORS.put(mob.getUUID(), mob.position());
        }
        puff(level, mob, 14);
        return true;
    }

    /** Відкликає одну душу до сховища. */
    private static void recallOne(ServerPlayer player, UUID soulId) {
        MinecraftServer server = player.getServer();
        Map<UUID, UUID> map = SUMMONED.get(player.getUUID());
        UUID minionId = map == null ? null : map.remove(soulId);
        if (server == null) {
            return;
        }
        if (minionId != null) {
            Entity minion = entity(server, minionId);
            if (minion != null) {
                if (minion.level() instanceof ServerLevel level) {
                    puff(level, minion, 12);
                }
                minion.discard();
            }
            APPROVED.remove(minionId);
            ANCHORS.remove(minionId);
        }
        player.displayClientMessage(Component.translatable("message.spirit_form.army.recalled"), true);
        sync(player);
    }

    /** Повертає всіх викликаних до сховища. */
    public static void recallAll(ServerPlayer player, boolean message) {
        MinecraftServer server = player.getServer();
        Map<UUID, UUID> map = SUMMONED.remove(player.getUUID());
        if (server == null) {
            return;
        }
        if (map != null) {
            for (UUID id : map.values()) {
                Entity minion = entity(server, id);
                if (minion != null) {
                    if (minion.level() instanceof ServerLevel level) {
                        puff(level, minion, 12);
                    }
                    minion.discard();
                }
                APPROVED.remove(id);
                ANCHORS.remove(id);
            }
        }
        if (message) {
            player.displayClientMessage(Component.translatable("message.spirit_form.army.retreat"), true);
            sync(player);
        }
    }

    /** Викликаний моб загинув або зник: душа лишається у сховищі й готова до нового виклику. */
    private static void forget(Entity minion) {
        UUID owner = ownerOf(minion);
        UUID id = minion.getUUID();
        APPROVED.remove(id);
        ANCHORS.remove(id);
        Map<UUID, UUID> map = SUMMONED.get(owner);
        if (map != null) {
            map.values().remove(id);
        }
        MinecraftServer server = minion.getServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player != null) {
                sync(player);
            }
        }
    }

    // ---------- розум армії ----------

    private static LivingEntity valid(UUID owner, LivingEntity target) {
        if (target == null || !target.isAlive() || isAllyOf(owner, target) || target.isInvulnerable()) {
            return null;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return null;
        }
        return target;
    }

    private static LivingEntity nearestStranger(UUID owner, Mob mob) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class,
                mob.getBoundingBox().inflate(7.0), e -> e instanceof Mob || e instanceof Player)) {
            if (valid(owner, candidate) == null) {
                continue;
            }
            double distance = candidate.distanceToSqr(mob);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static LivingEntity chooseTarget(ServerPlayer player, Mob mob, Order order) {
        UUID owner = player.getUUID();
        LivingEntity target = null;
        switch (order) {
            case ATTACK -> {
                if (player.tickCount - player.getLastHurtMobTimestamp() <= MEMORY_TICKS) {
                    target = valid(owner, player.getLastHurtMob());
                }
            }
            case GUARD -> {
                if (player.tickCount - player.getLastHurtByMobTimestamp() <= MEMORY_TICKS) {
                    target = valid(owner, player.getLastHurtByMob());
                }
            }
            case HOLD -> target = nearestStranger(owner, mob);
            case FURY -> {
                // Моб доводить бій до кінця: поки поточна ціль жива й недалеко, нову не обирає
                LivingEntity current = approvedTarget(mob);
                if (current != null && valid(owner, current) != null && current.distanceToSqr(mob) < 32.0 * 32.0) {
                    return current;
                }
                LivingEntity threat = player.tickCount - player.getLastHurtByMobTimestamp() <= MEMORY_TICKS
                        ? valid(owner, player.getLastHurtByMob()) : null;
                LivingEntity victim = player.tickCount - player.getLastHurtMobTimestamp() <= MEMORY_TICKS
                        ? valid(owner, player.getLastHurtMob()) : null;
                // Нову ціль кожен моб бере найближчу з двох, тож армія розподіляється між ними
                if (threat == null || victim == null) {
                    target = threat == null ? victim : threat;
                } else {
                    target = threat.distanceToSqr(mob) <= victim.distanceToSqr(mob) ? threat : victim;
                }
            }
        }
        if (target == null && mob.tickCount - mob.getLastHurtByMobTimestamp() <= MEMORY_TICKS) {
            target = valid(owner, mob.getLastHurtByMob());
        }
        return target;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        UUID owner = player.getUUID();
        Map<UUID, UUID> map = SUMMONED.get(owner);
        if (map == null || map.isEmpty() || player.getServer() == null) {
            return;
        }
        Order order = order(owner);
        boolean changed = false;

        Iterator<Map.Entry<UUID, UUID>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            UUID id = it.next().getValue();
            Entity found = entity(player.getServer(), id);
            if (!(found instanceof Mob mob) || !mob.isAlive()) {
                it.remove();
                APPROVED.remove(id);
                ANCHORS.remove(id);
                changed = true;
                continue;
            }
            // Гравець пішов в інший вимір: армія повертається до сховища
            if (mob.level() != player.level()) {
                mob.discard();
                it.remove();
                APPROVED.remove(id);
                ANCHORS.remove(id);
                changed = true;
                continue;
            }

            if (mob.level() instanceof ServerLevel level) {
                level.sendParticles(SpiritParticles.MINT, mob.getX(), mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(),
                        2, mob.getBbWidth() / 2.0, mob.getBbHeight() / 3.0, mob.getBbWidth() / 2.0, 0.0);
            }

            LivingEntity target = chooseTarget(player, mob, order);
            if (mob instanceof Warden warden) {
                keepWardenUp(warden, target);
            }
            if (target != null) {
                APPROVED.put(id, target.getUUID());
                if (mob.getTarget() != target) {
                    mob.setTarget(target);
                }
                continue;
            }

            if (mob.getTarget() != null) {
                APPROVED.remove(id);
                mob.setTarget(null);
            }
            if (order == Order.HOLD) {
                Vec3 anchor = ANCHORS.computeIfAbsent(id, key -> mob.position());
                if (mob.position().distanceToSqr(anchor) > 4.0) {
                    mob.getNavigation().moveTo(anchor.x, anchor.y, anchor.z, 1.1);
                }
            } else {
                double distance = mob.distanceToSqr(player);
                if (distance > 28.0 * 28.0) {
                    double angle = player.level().random.nextDouble() * Math.PI * 2.0;
                    mob.getNavigation().stop();
                    mob.moveTo(player.getX() + Math.cos(angle) * 2.0, player.getY(),
                            player.getZ() + Math.sin(angle) * 2.0, mob.getYRot(), 0.0F);
                } else if (distance > 6.0 * 6.0) {
                    mob.getNavigation().moveTo(player, 1.25);
                }
            }
        }
        if (changed) {
            sync(player);
        }
    }

    /**
     * Наглядач керується не звичайною ціллю, а пам'яттю мозку: без "кулдауну копання" він одразу закопується,
     * а власні цілі мозок обирає сам. Тому тримаємо кулдаун і виставляємо ціль вручну.
     */
    private static void keepWardenUp(Warden warden, LivingEntity target) {
        Brain<Warden> brain = warden.getBrain();
        brain.setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
        if (target != null) {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } else {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    private static LivingEntity approvedTarget(Mob mob) {
        UUID approved = APPROVED.get(mob.getUUID());
        if (approved == null || !(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity found = level.getEntity(approved);
        return found instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    /**
     * Дракон Енду без битви не має куди летіти й кружляє навколо центру світу. Тому фазу "висіти" ставимо щотіку,
     * а летить він сам: до цілі, до точки "Позиції" або слідом за власником.
     */
    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || dragon.level().isClientSide()) {
            return;
        }
        UUID owner = ownerOf(dragon);
        MinecraftServer server = dragon.getServer();
        if (owner == null || server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null || player.level() != dragon.level()) {
            return;
        }

        // Скидаємо "стару" точку призначення висіння
        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
        dragon.setDeltaMovement(Vec3.ZERO);

        Vec3 position = dragon.position();
        LivingEntity target = approvedTarget(dragon);
        Vec3 goal = null;
        if (target != null) {
            goal = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        } else if (order(owner) == Order.HOLD) {
            goal = ANCHORS.get(dragon.getUUID());
        } else if (position.distanceToSqr(player.position()) > 14.0 * 14.0) {
            goal = player.position().add(0.0, 6.0, 0.0);
        }
        if (goal == null) {
            return;
        }

        Vec3 delta = goal.subtract(position);
        double length = delta.length();
        if (length < 1.0) {
            return;
        }
        Vec3 step = delta.scale(Math.min(length, 0.8) / length);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) + 90.0F;
        dragon.moveTo(position.x + step.x, position.y + step.y, position.z + step.z, yaw, 0.0F);
    }

    /** Візер стріляє не лише головною головою: бічні голови самі обирають цілі серед усіх, зокрема власника. */
    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof WitherBoss wither) || wither.level().isClientSide() || ownerOf(wither) == null) {
            return;
        }
        LivingEntity target = approvedTarget(wither);
        int id = target == null ? 0 : target.getId();
        for (int head = 0; head < 3; head++) {
            if (wither.getAlternativeTarget(head) != id) {
                wither.setAlternativeTarget(head, id);
            }
        }
    }

    // ---------- правила для викликаних ----------

    /** Викликані моби атакують лише тих, кого вказала армія (звичайні цілі ванільного ШІ скасовуємо). */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity self = event.getEntity();
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || ownerOf(self) == null) {
            return;
        }
        if (!target.getUUID().equals(APPROVED.get(self.getUUID()))) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** Власник і викликані ним моби не завдають шкоди одне одному. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            return;
        }
        UUID attackerOwner = ownerOf(attacker);
        if (attackerOwner != null && isAllyOf(attackerOwner, event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        UUID targetOwner = ownerOf(event.getEntity());
        if (targetOwner != null && attacker.getUUID().equals(targetOwner)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (ownerOf(event.getEntity()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (ownerOf(event.getEntity()) != null) {
            event.setCanceled(true);
        }
    }

    /** Моби армії, яких немає в пам'яті сервера (світ перезапущено), не повертаються у світ. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || spawning) {
            return;
        }
        Entity entity = event.getEntity();
        UUID owner = ownerOf(entity);
        if (owner == null) {
            return;
        }
        Map<UUID, UUID> map = SUMMONED.get(owner);
        if (map == null || !map.containsValue(entity.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recallAll(player, false);
            ORDERS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SUMMONED.clear();
        ORDERS.clear();
        APPROVED.clear();
        ANCHORS.clear();
    }
}
