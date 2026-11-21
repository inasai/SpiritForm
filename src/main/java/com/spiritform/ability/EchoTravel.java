package com.spiritform.ability;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.data.SpiritAttachments;
import com.spiritform.data.SpiritData;
import com.spiritform.data.SpiritEchoIndex;
import com.spiritform.event.SpiritAdvancements;
import com.spiritform.form.SpiritBeyond;
import com.spiritform.form.SpiritFormState;
import com.spiritform.herald.SpiritHeralds;
import com.spiritform.item.FentonBracelet;
import com.spiritform.registry.SpiritParticles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Подорож між вимірами через колесо Відлуння Смерті (ПКМ по Відлунню в колесі пасивних здібностей).
 *
 * Гравець може обрати лише той вимір, де вже бував. Запис про візит ведемо самі (вкладення гравця),
 * а для тих, хто бував у Незері, Енді й Потойбіччі до цього оновлення, беремо досягнення.
 * Для кожного виміру пам'ятаємо останнє безпечне місце гравця: туди й повертаємо. Якщо його немає чи воно
 * зайняте, шукаємо безпечне місце поруч; у крайньому разі стандартна точка виміру.
 * Перезарядка спільна з іншими здібностями (abilityCooldownSeconds).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class EchoTravel {

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();
    private static final ResourceLocation NETHER_ADVANCEMENT = ResourceLocation.withDefaultNamespace("story/enter_the_nether");
    private static final ResourceLocation END_ADVANCEMENT = ResourceLocation.withDefaultNamespace("story/enter_the_end");
    private static final BlockPos END_ARRIVAL = new BlockPos(100, 49, 0);

    private EchoTravel() {
    }

    // ---------- облік візитів ----------

    public static List<String> visited(net.minecraft.world.entity.player.Player player) {
        return player.getData(SpiritAttachments.VISITED_DIMENSIONS.get());
    }

    public static void markVisited(ServerPlayer player, ResourceLocation dimension) {
        List<String> list = visited(player);
        String id = dimension.toString();
        if (!list.contains(id)) {
            List<String> next = new ArrayList<>(list);
            next.add(id);
            player.setData(SpiritAttachments.VISITED_DIMENSIONS.get(), List.copyOf(next));
        }
    }

    private static void saveSpot(ServerPlayer player, ResourceLocation dimension, BlockPos pos) {
        Map<String, BlockPos> spots = player.getData(SpiritAttachments.DIMENSION_SPOTS.get());
        String id = dimension.toString();
        if (pos.equals(spots.get(id))) {
            return;
        }
        Map<String, BlockPos> next = new HashMap<>(spots);
        next.put(id, pos.immutable());
        player.setData(SpiritAttachments.DIMENSION_SPOTS.get(), Map.copyOf(next));
    }

    private static boolean hasAdvancement(ServerPlayer player, MinecraftServer server, ResourceLocation id) {
        AdvancementHolder holder = server.getAdvancements().get(id);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        markVisited(player, player.level().dimension().location());
        markVisited(player, Level.OVERWORLD.location());
        if (hasAdvancement(player, server, NETHER_ADVANCEMENT)) {
            markVisited(player, Level.NETHER.location());
        }
        if (hasAdvancement(player, server, END_ADVANCEMENT)) {
            markVisited(player, Level.END.location());
        }
        if (hasAdvancement(player, server, ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "beyond_home"))) {
            markVisited(player, SpiritBeyond.KEY.location());
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            markVisited(player, event.getTo().location());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 40 != 7) {
            return;
        }
        markVisited(player, player.level().dimension().location());
        if (player.onGround() && !player.isSpectator() && player.level() instanceof ServerLevel level) {
            BlockPos pos = player.blockPosition();
            if (isSafe(level, pos)) {
                saveSpot(player, level.dimension().location(), pos);
            }
        }
    }

    // ---------- подорож ----------

    public static void travel(ServerPlayer player, ResourceLocation dimension) {
        MinecraftServer server = player.getServer();
        if (server == null || dimension == null || !(player.level() instanceof ServerLevel from)) {
            return;
        }
        SpiritData data = SpiritFormState.get(player);
        if (!data.hasAbility(SpiritAbility.ECHO)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.passive.locked", SpiritAbility.ECHO.displayName()), true);
            return;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
        if (key.equals(from.dimension())) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.here"), true);
            return;
        }
        if (!visited(player).contains(dimension.toString())) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.not_visited"), true);
            return;
        }
        ServerLevel target = server.getLevel(key);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.missing"), true);
            return;
        }
        if (SpiritPossession.isPossessing(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.possessing"), true);
            return;
        }
        long now = from.getGameTime();
        long until = COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L);
        if (until > now) {
            SpiritCooldowns.notifyTired(player, until);
            return;
        }

        BlockPos arrival = findArrival(player, from, target);
        if (arrival == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.no_safe"), true);
            return;
        }

        BlockPos here = player.blockPosition();
        if (isSafe(from, here)) {
            saveSpot(player, from.dimension().location(), here);
        }
        boolean leavingBeyond = from.dimension().equals(SpiritBeyond.KEY);
        boolean enteringBeyond = key.equals(SpiritBeyond.KEY);
        if (leavingBeyond) {
            SpiritBeyond.rememberExit(player);
        }
        if (enteringBeyond) {
            SpiritEchoIndex.get(server).putReturn(player.getUUID(),
                    new SpiritEchoIndex.Entry(from.dimension().location().toString(), here));
        }
        COOLDOWN_UNTIL.put(player.getUUID(), now + FentonBracelet.cooldown(player, Config.ABILITY_COOLDOWN_SECONDS.get() * 20L));

        player.stopRiding();
        puff(from, player.getX(), player.getY(), player.getZ());
        player.teleportTo(target, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        puff(target, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
        player.displayClientMessage(Component.translatable("message.spirit_form.echo.travel", dimensionName(dimension)), true);

        if (leavingBeyond) {
            SpiritHeralds.onReturn(player);
        }
        if (enteringBeyond) {
            SpiritAdvancements.grant(player, "beyond_home");
            SpiritHeralds.onEnterBeyond(player);
        }
    }

    /** Назва виміру для гравця: переклад (dimension.<простір>.<назва>) або гарна назва з ідентифікатора. */
    public static Component dimensionName(ResourceLocation id) {
        String path = id.getPath();
        String key = "dimension." + id.getNamespace() + "." + path.replace('/', '.');
        String last = path.substring(path.lastIndexOf('/') + 1);
        StringBuilder pretty = new StringBuilder();
        for (String word : last.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!pretty.isEmpty()) {
                pretty.append(' ');
            }
            pretty.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return Component.translatableWithFallback(key, pretty.toString());
    }

    // ---------- пошук безпечного місця ----------

    private static BlockPos findArrival(ServerPlayer player, ServerLevel from, ServerLevel target) {
        BlockPos saved = player.getData(SpiritAttachments.DIMENSION_SPOTS.get()).get(target.dimension().location().toString());
        if (saved != null) {
            BlockPos near = findNear(target, saved);
            if (near != null) {
                return near;
            }
        }
        if (target.dimension().equals(Level.END)) {
            makeEndPlatform(target);
            return END_ARRIVAL;
        }
        if (target.dimension().equals(Level.OVERWORLD)) {
            BlockPos respawn = player.getRespawnPosition();
            if (respawn != null && Level.OVERWORLD.equals(player.getRespawnDimension())) {
                BlockPos near = findNear(target, respawn);
                if (near != null) {
                    return near;
                }
            }
            BlockPos spawn = target.getSharedSpawnPos();
            return findAround(target, spawn.getX(), spawn.getZ());
        }
        double scale = from.dimensionType().coordinateScale() / target.dimensionType().coordinateScale();
        return findAround(target, (int) Math.floor(player.getX() * scale), (int) Math.floor(player.getZ() * scale));
    }

    /** Безпечне місце поруч із відомою позицією: спершу по висоті, потім по сусідніх стовпчиках. */
    private static BlockPos findNear(ServerLevel level, BlockPos hint) {
        for (int radius = 0; radius <= 8; radius += 2) {
            for (int dx = -radius; dx <= radius; dx += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int k = 0; k <= 24; k++) {
                        int dy = (k + 1) / 2 * (k % 2 == 0 ? 1 : -1);
                        BlockPos pos = hint.offset(dx, dy, dz);
                        if (isSafe(level, pos)) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Безпечне місце навколо координат (для виміру, де гравець ще не стояв після цього оновлення). */
    private static BlockPos findAround(ServerLevel level, int x, int z) {
        for (int radius = 0; radius <= 32; radius += 8) {
            for (int dx = -radius; dx <= radius; dx += 8) {
                for (int dz = -radius; dz <= radius; dz += 8) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    BlockPos found = scanColumn(level, x + dx, z + dz);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos scanColumn(ServerLevel level, int x, int z) {
        int min = level.getMinBuildHeight() + 1;
        int top;
        if (level.dimensionType().hasCeiling()) {
            top = level.getMinBuildHeight() + level.dimensionType().logicalHeight() - 2;
        } else {
            top = Math.min(level.getMaxBuildHeight() - 2, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top; y >= min; y--) {
            pos.setY(y);
            if (isSafe(level, pos)) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** Ноги й голова вільні, під ногами твердо, без рідини, вогню й небезпечної підлоги. */
    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockPos above = pos.above();
        if (!level.isInWorldBounds(below) || !level.isInWorldBounds(above)) {
            return false;
        }
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(above);
        BlockState floor = level.getBlockState(below);
        if (!feet.getCollisionShape(level, pos).isEmpty() || !head.getCollisionShape(level, above).isEmpty()) {
            return false;
        }
        if (floor.getCollisionShape(level, below).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(above).isEmpty()) {
            return false;
        }
        return !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CACTUS) && !floor.is(BlockTags.CAMPFIRES)
                && !feet.is(BlockTags.FIRE) && !feet.is(Blocks.SWEET_BERRY_BUSH) && !feet.is(Blocks.POWDER_SNOW);
    }

    /** Платформа Енду, як після ванільного порталу: 5x5 обсидіану й три блоки повітря над нею. */
    private static void makeEndPlatform(ServerLevel level) {
        BlockPos center = END_ARRIVAL.below();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(center.offset(dx, 0, dz), Blocks.OBSIDIAN.defaultBlockState());
                for (int dy = 1; dy <= 3; dy++) {
                    level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void puff(ServerLevel level, double x, double y, double z) {
        level.sendParticles(SpiritParticles.MINT, x, y + 0.9, z, 40, 0.4, 0.8, 0.4, 0.08);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
    }
}
