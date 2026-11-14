package com.spiritform.herald;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.army.SpiritArmy;
import com.spiritform.data.HeraldStore;
import com.spiritform.form.SpiritBeyond;
import com.spiritform.form.SpiritFormState;
import com.spiritform.network.HeraldSyncPayload;
import com.spiritform.registry.SpiritBlocks;
import com.spiritform.registry.SpiritItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Серце системи Вісників Потойбіччя (лише сервер, він єдиний авторитет).
 *
 * Шкала. Кожне НОВЕ потрапляння в Потойбіччя додає наступне ціле число (+1, +2, +3 ...). Коли шкала дійшла до 100
 * і гравець повертається у світ, з'являється черговий Вісник (за колом Буря, Голод, Холод) і від шкали віднімається
 * рівно 100. Серце віднімає ще 100 і не зачіпає ні чергу, ні лічильник.
 *
 * Зустріч закінчується: перемогою (Вісник загинув: серце випадає з нього), поразкою (гравець загинув, вийшов зі
 * світу, пішов у Потойбіччя або Вісник зник), за будь-якого кінця черга зсувається на наступного Вісника.
 * Вся кадрова робота ідемпотентна: finishRegular виконується лише поки в запису є активна зустріч.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritHeralds {

    public static final int THRESHOLD = 100;

    /** Скільки перевірок (по секунді) Вісник може бути відсутнім, перш ніж зустріч зараховується як програна. */
    private static final int MISSING_LIMIT = 10;
    private static final Map<UUID, Integer> MISSING = new HashMap<>();

    private SpiritHeralds() {
    }

    // ---------- шкала ----------

    /** Гравець щойно потрапив у Потойбіччя через Відлуння Смерті: нарахувати приріст (один раз). */
    public static void onEnterBeyond(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        HeraldStore store = HeraldStore.get(server);
        HeraldStore.Rec rec = store.rec(player.getUUID());
        long now = server.overworld().getGameTime();
        if (rec.lastEntryTick == now) {
            return;
        }
        rec.lastEntryTick = now;

        int step = (Config.HERALD_PROGRESSION_RESTARTS.get() ? rec.sinceSpawn : rec.total) + 1;
        rec.sinceSpawn++;
        rec.total++;
        boolean before = rec.meter >= THRESHOLD;
        rec.meter += step;
        store.setDirty();

        // Гравець пішов від Вісника в Потойбіччя: зустріч вважається програною, Вісник втратив слід
        if (rec.activeKind >= 0) {
            finishRegular(server, player.getUUID(), false);
            player.sendSystemMessage(Component.translatable("message.spirit_form.herald.fled"));
        }
        if (!before && rec.meter >= THRESHOLD) {
            player.sendSystemMessage(Component.translatable("message.spirit_form.herald.ready",
                    HeraldType.byIndex(rec.next).displayName()));
        }
        sync(player);
    }

    /** Гравець повернувся з Потойбіччя у світ: якщо шкала повна, з'являється Вісник. */
    public static void onReturn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || !(player.level() instanceof ServerLevel level) || !player.isAlive()
                || level.dimension().equals(SpiritBeyond.KEY)) {
            return;
        }
        HeraldStore store = HeraldStore.get(server);
        HeraldStore.Rec rec = store.rec(player.getUUID());
        if (rec.meter < THRESHOLD || rec.activeKind >= 0 || store.worldHerald != null) {
            return;
        }
        startRegular(player, level, HeraldType.byIndex(rec.next), true, false);
    }

    /** Створює звичайного Вісника. fromMeter: відняти 100 від шкали (звичайна поява); forced: виклик командою. */
    private static boolean startRegular(ServerPlayer player, ServerLevel level, HeraldType kind, boolean fromMeter,
            boolean forced) {
        MinecraftServer server = level.getServer();
        HeraldStore store = HeraldStore.get(server);
        HeraldStore.Rec rec = store.rec(player.getUUID());
        if (kind == HeraldType.DEATH || rec.activeKind >= 0 || store.worldHerald != null) {
            return false;
        }
        HeraldBase herald = kind.entityType().create(level);
        if (herald == null) {
            return false;
        }
        Vec3 pos = findSpawn(level, player.position(), kind, 14.0, 22.0);
        herald.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        herald.setOwner(player.getUUID());
        herald.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.getEyePosition());

        // Спершу запис у сховищі, потім додавання у світ: інакше обробник приєднання вважав би Вісника сиротою
        rec.activeKind = indexOf(kind);
        rec.activeHerald = herald.getUUID();
        rec.activeForced = forced;
        if (fromMeter) {
            rec.meter -= THRESHOLD;
        }
        store.worldHerald = herald.getUUID();
        store.worldOwner = player.getUUID();
        store.setDirty();
        level.addFreshEntity(herald);

        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 0.7F);
        player.sendSystemMessage(Component.translatable("message.spirit_form.herald.spawned", kind.displayName()));
        sync(player);
        return true;
    }

    private static int indexOf(HeraldType kind) {
        for (int i = 0; i < HeraldType.CYCLE.length; i++) {
            if (HeraldType.CYCLE[i] == kind) {
                return i;
            }
        }
        return -1;
    }

    /** Кінець зустрічі зі звичайним Вісником (перемога або поразка). Виконується один раз на зустріч. */
    private static void finishRegular(MinecraftServer server, UUID ownerId, boolean won) {
        HeraldStore store = HeraldStore.get(server);
        HeraldStore.Rec rec = store.rec(ownerId);
        if (rec.activeKind < 0) {
            return;
        }
        UUID heraldId = rec.activeHerald;
        int kindIndex = rec.activeKind;
        boolean forced = rec.activeForced;

        rec.activeKind = -1;
        rec.activeHerald = null;
        rec.activeForced = false;
        if (!forced) {
            rec.next = Math.floorMod(kindIndex + 1, HeraldType.CYCLE.length);
        }
        if (ownerId.equals(store.worldOwner) || (heraldId != null && heraldId.equals(store.worldHerald))) {
            store.worldHerald = null;
            store.worldOwner = null;
        }
        store.setDirty();
        MISSING.remove(ownerId);

        if (!won && heraldId != null) {
            Entity herald = find(server, heraldId);
            if (herald != null) {
                herald.discard();
            }
        }
        cleanupEnvironment(server);

        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            HeraldType kind = HeraldType.byIndex(kindIndex);
            owner.sendSystemMessage(Component.translatable(
                    won ? "message.spirit_form.herald.won" : "message.spirit_form.herald.lost", kind.displayName()));
            sync(owner);
        }
    }

    /** Прибирає погоду, зиму та уповільнення польоту, якщо звичайного Вісника вже немає. */
    private static void cleanupEnvironment(MinecraftServer server) {
        HeraldStore store = HeraldStore.get(server);
        if (store.worldHerald != null) {
            return;
        }
        SpiritHeraldWorld.restoreWeather(server);
        SpiritHeraldWorld.cleanupWinter(server, 1500);
        SpiritHeraldWorld.releaseAllFliers(server);
    }

    /** Викликається з HeraldBase.die(). */
    public static void onHeraldDied(ServerLevel level, HeraldBase herald, DamageSource source) {
        MinecraftServer server = level.getServer();
        HeraldStore store = HeraldStore.get(server);
        if (herald.kind() == HeraldType.DEATH) {
            if (!herald.getUUID().equals(store.deathHerald)) {
                return;
            }
            UUID ownerId = store.deathOwner;
            store.deathHerald = null;
            store.deathOwner = null;
            store.setDirty();
            MISSING.remove(herald.getUUID());
            ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
            server.getPlayerList().broadcastSystemMessage(Component.translatable("message.spirit_form.herald.death_defeated"), false);
            NeoForge.EVENT_BUS.post(new DeathHeraldDefeatedEvent(level, owner, herald.position()));
            if (owner != null) {
                sync(owner);
            }
            return;
        }
        UUID ownerId = herald.owner();
        if (ownerId == null) {
            return;
        }
        HeraldStore.Rec rec = store.rec(ownerId);
        if (herald.getUUID().equals(rec.activeHerald)) {
            finishRegular(server, ownerId, true);
        }
    }

    // ---------- серця ----------

    /** Серце з'їдено (предмет уже зменшено): шкала мінус 100. Черга, лічильник і зустріч не змінюються. */
    public static void onHeartUsed(ServerPlayer player, HeraldType kind) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        HeraldStore store = HeraldStore.get(server);
        HeraldStore.Rec rec = store.rec(player.getUUID());
        rec.meter -= THRESHOLD;
        store.setDirty();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("message.spirit_form.herald.heart_used", kind.displayName(),
                THRESHOLD), true);
        sync(player);
    }

    // ---------- Вісник Смерті ----------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(SpiritItems.PROOF_OF_STRENGTH.get())) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        // Лише блок Відлуння Потойбіччя; на будь-якому іншому серце просто лежить у руці
        if (!level.getBlockState(event.getPos()).is(SpiritBlocks.ECHO_BLOCK.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        MinecraftServer server = level.getServer();
        HeraldStore store = HeraldStore.get(server);
        if (!SpiritFormState.isActive(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.herald.death_need_form"), true);
            return;
        }
        if (level.dimension() != Level.OVERWORLD) {
            player.displayClientMessage(Component.translatable("message.spirit_form.herald.death_need_overworld"), true);
            return;
        }
        if (store.deathHerald != null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.herald.death_exists"), true);
            return;
        }
        if (!startDeath(player, level, event.getPos().getCenter())) {
            player.displayClientMessage(Component.translatable("message.spirit_form.herald.death_failed"), true);
            return;
        }
        // Доказ сили витрачається лише після успішного виклику
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    /** Створює Вісника Смерті біля точки. Повертає false, якщо не вдалося (тоді нічого не змінюється). */
    private static boolean startDeath(ServerPlayer player, ServerLevel level, Vec3 around) {
        HeraldStore store = HeraldStore.get(level.getServer());
        if (store.deathHerald != null) {
            return false;
        }
        HeraldBase herald = HeraldType.DEATH.entityType().create(level);
        if (herald == null) {
            return false;
        }
        Vec3 pos = findSpawn(level, around, HeraldType.DEATH, 5.0, 9.0);
        herald.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        herald.setOwner(player.getUUID());
        herald.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        store.deathHerald = herald.getUUID();
        store.deathOwner = player.getUUID();
        store.setDirty();
        level.addFreshEntity(herald);

        SpiritHeraldWorld.makeNight(level.getServer());
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.5F);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.spirit_form.herald.death_summoned", player.getDisplayName()), false);
        sync(player);
        return true;
    }

    /** Прибирає Вісника Смерті без нагороди (викликач загинув або вийшов зі світу). */
    private static void dismissDeath(MinecraftServer server) {
        HeraldStore store = HeraldStore.get(server);
        UUID id = store.deathHerald;
        UUID ownerId = store.deathOwner;
        if (id == null) {
            return;
        }
        store.deathHerald = null;
        store.deathOwner = null;
        store.setDirty();
        MISSING.remove(id);
        Entity herald = find(server, id);
        if (herald != null) {
            herald.discard();
        }
        ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            sync(owner);
        }
    }

    /** Лікування Вісника Смерті, коли він б'є звичайну істоту. */
    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof DeathHerald boss) || !boss.isAlive()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player || victim instanceof HeraldBase || victim.getType().is(EntityTypeTags.UNDEAD)
                || SpiritArmy.isMinion(victim)) {
            return;
        }
        float heal = (float) (event.getNewDamage() * Config.HERALD_DEATH_HEAL_MULTIPLIER.get());
        if (heal > 0.0F) {
            boss.heal(heal);
        }
    }

    // ---------- життєвий цикл гравця й світу ----------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        finishRegular(server, player.getUUID(), false);
        HeraldStore store = HeraldStore.get(server);
        if (player.getUUID().equals(store.deathOwner)) {
            dismissDeath(server);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        finishRegular(server, player.getUUID(), false);
        if (player.getUUID().equals(HeraldStore.get(server).deathOwner)) {
            dismissDeath(server);
        }
        SpiritHeraldWorld.forget(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    /** Вісник без запису в сховищі (старий світ, зламаний запис, дублікат) у світ не потрапляє. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof HeraldBase herald)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        HeraldStore store = HeraldStore.get(level.getServer());
        boolean known = herald.kind() == HeraldType.DEATH
                ? herald.getUUID().equals(store.deathHerald)
                : herald.getUUID().equals(store.worldHerald);
        if (!known) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 7) {
            return;
        }
        HeraldStore store = HeraldStore.get(server);

        for (Map.Entry<UUID, HeraldStore.Rec> entry : new HashMap<>(store.all()).entrySet()) {
            HeraldStore.Rec rec = entry.getValue();
            if (rec.activeKind < 0) {
                continue;
            }
            UUID ownerId = entry.getKey();
            if (server.getPlayerList().getPlayer(ownerId) == null) {
                finishRegular(server, ownerId, false);
                continue;
            }
            Entity herald = rec.activeHerald == null ? null : find(server, rec.activeHerald);
            if (herald == null || !herald.isAlive()) {
                int missing = MISSING.merge(ownerId, 1, Integer::sum);
                if (missing >= MISSING_LIMIT) {
                    finishRegular(server, ownerId, false);
                }
            } else {
                MISSING.remove(ownerId);
            }
        }

        if (store.deathHerald != null) {
            Entity boss = find(server, store.deathHerald);
            boolean ownerOnline = store.deathOwner != null && server.getPlayerList().getPlayer(store.deathOwner) != null;
            if (!ownerOnline) {
                dismissDeath(server);
            } else if (boss == null || !boss.isAlive()) {
                int missing = MISSING.merge(store.deathHerald, 1, Integer::sum);
                if (missing >= MISSING_LIMIT) {
                    dismissDeath(server);
                }
            } else {
                MISSING.remove(store.deathHerald);
            }
        }

        // Страховка: якщо звичайного Вісника немає, зима, погода й уповільнення не повинні лишатися
        if (store.worldHerald == null) {
            if (!store.winterSnow.isEmpty() || !store.winterIce.isEmpty()) {
                SpiritHeraldWorld.cleanupWinter(server, 1500);
            }
            if (store.weatherSaved) {
                SpiritHeraldWorld.restoreWeather(server);
            }
        }
    }

    // ---------- допоміжне ----------

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity found = level.getEntity(id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Місце появи на відстані min..max від точки: вільне для Вісника, на твердій землі (Буря ще й вище). */
    private static Vec3 findSpawn(ServerLevel level, Vec3 around, HeraldType kind, double min, double max) {
        RandomSource random = level.random;
        net.minecraft.world.entity.EntityDimensions size = kind.entityType().getDimensions();
        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = min + random.nextDouble() * (max - min);
            double x = around.x + Math.cos(angle) * dist;
            double z = around.z + Math.sin(angle) * dist;
            for (int dy = 6; dy >= -8; dy--) {
                double y = Math.floor(around.y) + dy;
                if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 6) {
                    continue;
                }
                AABB box = size.makeBoundingBox(x, y, z);
                if (!level.noCollision(box)) {
                    continue;
                }
                net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.containing(x, y - 0.5, z);
                boolean solid = !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
                boolean wet = !level.getFluidState(below).isEmpty();
                if (!solid || wet) {
                    continue;
                }
                double top = kind == HeraldType.STORM && level.noCollision(size.makeBoundingBox(x, y + 5, z)) ? y + 5 : y;
                return new Vec3(x, top, z);
            }
        }
        return new Vec3(around.x + 4.0, around.y + 1.0, around.z);
    }

    public static void sync(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        HeraldStore store = HeraldStore.get(server);
        HeraldStore.Rec rec = store.rec(player.getUUID());
        PacketDistributor.sendToPlayer(player,
                new HeraldSyncPayload(rec.meter, rec.next, rec.activeKind, store.deathHerald != null));
    }

    // ---------- команди для перевірки ----------

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spirit_herald")
                .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("meter")
                .then(Commands.literal("get").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    HeraldStore.Rec rec = HeraldStore.get(player.getServer()).rec(player.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.spirit_form.herald.meter",
                            rec.meter, rec.sinceSpawn, rec.total, HeraldType.byIndex(rec.next).displayName()), false);
                    return rec.meter;
                }))
                .then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(ctx -> changeMeter(ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "value"), false))))
                .then(Commands.literal("add").then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(ctx -> changeMeter(ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "value"), true)))));

        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn");
        for (HeraldType kind : HeraldType.values()) {
            spawn.then(Commands.literal(kind.name().toLowerCase(java.util.Locale.ROOT)).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                if (!(player.level() instanceof ServerLevel level)) {
                    return 0;
                }
                boolean ok = kind == HeraldType.DEATH
                        ? startDeath(player, level, player.position())
                        : startRegular(player, level, kind, false, true);
                if (!ok) {
                    ctx.getSource().sendFailure(Component.translatable("command.spirit_form.herald.spawn_failed"));
                    return 0;
                }
                return 1;
            }));
        }
        root.then(spawn);

        root.then(Commands.literal("clear").executes(ctx -> {
            MinecraftServer server = ctx.getSource().getServer();
            HeraldStore store = HeraldStore.get(server);
            for (UUID id : new java.util.ArrayList<>(store.all().keySet())) {
                HeraldStore.Rec rec = store.rec(id);
                if (rec.activeKind >= 0) {
                    rec.activeForced = true;
                    finishRegular(server, id, false);
                }
            }
            dismissDeath(server);
            ctx.getSource().sendSuccess(() -> Component.translatable("command.spirit_form.herald.cleared"), false);
            return 1;
        }));

        root.then(Commands.literal("reset").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            HeraldStore store = HeraldStore.get(player.getServer());
            HeraldStore.Rec rec = store.rec(player.getUUID());
            if (rec.activeKind >= 0) {
                rec.activeForced = true;
                finishRegular(player.getServer(), player.getUUID(), false);
            }
            rec.meter = 0;
            rec.sinceSpawn = 0;
            rec.total = 0;
            rec.next = 0;
            store.setDirty();
            sync(player);
            ctx.getSource().sendSuccess(() -> Component.translatable("command.spirit_form.herald.reset"), false);
            return 1;
        }));

        event.getDispatcher().register(root);
    }

    private static int changeMeter(ServerPlayer player, int value, boolean add) {
        HeraldStore store = HeraldStore.get(player.getServer());
        HeraldStore.Rec rec = store.rec(player.getUUID());
        rec.meter = add ? rec.meter + value : value;
        store.setDirty();
        sync(player);
        player.sendSystemMessage(Component.translatable("command.spirit_form.herald.meter_now", rec.meter));
        return rec.meter;
    }
}
