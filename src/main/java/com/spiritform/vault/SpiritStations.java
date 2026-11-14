package com.spiritform.vault;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.ability.SpiritAbility;
import com.spiritform.data.SpiritAttachments;
import com.spiritform.data.StationData;
import com.spiritform.form.SpiritFormState;
import com.spiritform.form.SpiritUpgrades;
import com.spiritform.network.StationActionPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Самодостатність, серверна частина: станції з ванільними вікнами.
 *
 * Вікна верстата, зачарування, ковадла, ковальського столу, каменеріза, точила, картографічного столу й ткацького
 * верстата відкриваються без блока (їхній стан, як у ваніли, повертається гравцеві при закритті).
 *
 * Піч, коптильня, домна та варильна стійка мають власні "віртуальні" блок-сутності, що не стоять у світі: вони
 * зберігають предмети, паливо й прогрес, продовжують працювати після закриття вікна, а їхній стан лежить у
 * вкладенні гравця. Кожну тікає своя сутність, тож черги виробництва незалежні.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritStations {

    /**
     * Позиція віртуальних сутностей: поза висотою світу й у незавантаженому куті, щоб ванільний tick, який пише блок
     * (стан LIT) і викликає setChanged, нічого не змінював у справжньому світі.
     */
    private static final BlockPos VOID = new BlockPos(29_999_000, -4000, 29_999_000);

    private static final class VirtualFurnace extends FurnaceBlockEntity {
        VirtualFurnace() {
            super(VOID, Blocks.FURNACE.defaultBlockState());
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        boolean active() {
            return !isEmpty();
        }
    }

    private static final class VirtualSmoker extends SmokerBlockEntity {
        VirtualSmoker() {
            super(VOID, Blocks.SMOKER.defaultBlockState());
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        boolean active() {
            return !isEmpty();
        }
    }

    private static final class VirtualBlast extends BlastFurnaceBlockEntity {
        VirtualBlast() {
            super(VOID, Blocks.BLAST_FURNACE.defaultBlockState());
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        boolean active() {
            return !isEmpty();
        }
    }

    private static final class VirtualBrewing extends BrewingStandBlockEntity {
        VirtualBrewing() {
            super(VOID, Blocks.BREWING_STAND.defaultBlockState());
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    /** Живий стан станцій одного гравця (лише на сервері). */
    private static final class Live {
        final VirtualFurnace furnace = new VirtualFurnace();
        final VirtualSmoker smoker = new VirtualSmoker();
        final VirtualBlast blast = new VirtualBlast();
        final VirtualBrewing brewing = new VirtualBrewing();
        int brewFinished;
        short lastBrewTime;

        /**
         * Віртуальним сутностям потрібен рівень: без нього ванільний setItem не виставляє тривалість плавки
         * (вона лишається 0, тож лічильник ніколи її не досягає і нічого не виплавляється).
         */
        void level(ServerLevel level) {
            furnace.setLevel(level);
            smoker.setLevel(level);
            blast.setLevel(level);
            brewing.setLevel(level);
        }
    }

    /** Старі збереження мають нульову тривалість плавки: перекладаємо предмет, щоб ваніла порахувала її наново. */
    private static void refreshTotal(AbstractFurnaceBlockEntity furnace, CompoundTag tag) {
        if (has(tag) && tag.getInt("CookTimeTotal") == 0 && !furnace.getItem(0).isEmpty()) {
            ItemStack input = furnace.getItem(0).copy();
            furnace.setItem(0, ItemStack.EMPTY);
            furnace.setItem(0, input);
        }
    }

    private static final Map<UUID, Live> LIVE = new HashMap<>();

    private SpiritStations() {
    }

    // ---------- дані ----------

    public static StationData get(Player player) {
        return player.getData(SpiritAttachments.STATIONS.get());
    }

    private static void set(Player player, StationData data) {
        player.setData(SpiritAttachments.STATIONS.get(), data);
    }

    private static boolean owns(Player player) {
        return SpiritFormState.get(player).hasAbility(SpiritAbility.SELF_SUFFICIENCY);
    }

    /** Скільки комірок станцій дають відкриті сторінки. */
    public static int capacity(StationData data) {
        return Math.max(1, Math.min(Station.pages(data.blocks()), data.pages())) * Station.PER_PAGE;
    }

    public static int pageCost(int pageIndex) {
        return pageIndex == 1 ? Config.STATION_PAGE2_COST.get() : Config.STATION_PAGE3_COST.get();
    }

    private static boolean has(CompoundTag tag) {
        return tag != null && !tag.isEmpty();
    }

    private static Live live(ServerPlayer player) {
        Live live = LIVE.get(player.getUUID());
        if (live != null) {
            return live;
        }
        live = new Live();
        live.level(player.serverLevel());
        HolderLookup.Provider registries = player.level().registryAccess();
        StationData data = get(player);
        if (has(data.furnace())) {
            live.furnace.loadWithComponents(data.furnace(), registries);
            refreshTotal(live.furnace, data.furnace());
        }
        if (has(data.smoker())) {
            live.smoker.loadWithComponents(data.smoker(), registries);
            refreshTotal(live.smoker, data.smoker());
        }
        if (has(data.blast())) {
            live.blast.loadWithComponents(data.blast(), registries);
            refreshTotal(live.blast, data.blast());
        }
        if (has(data.brewing())) {
            live.brewing.loadWithComponents(data.brewing(), registries);
            live.lastBrewTime = data.brewing().getShort("BrewTime");
        }
        LIVE.put(player.getUUID(), live);
        return live;
    }

    private static int readyMask(Live live) {
        int mask = 0;
        if (!live.furnace.getItem(2).isEmpty()) {
            mask |= Station.FURNACE.bit();
        }
        if (!live.smoker.getItem(2).isEmpty()) {
            mask |= Station.SMOKER.bit();
        }
        if (!live.blast.getItem(2).isEmpty()) {
            mask |= Station.BLAST_FURNACE.bit();
        }
        if (live.brewFinished > 0) {
            mask |= Station.BREWING.bit();
        }
        return mask;
    }

    /** Записує живий стан у вкладення гравця (і так надсилає клієнту готовність). */
    private static void flush(ServerPlayer player) {
        Live live = LIVE.get(player.getUUID());
        if (live == null) {
            return;
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        StationData old = get(player);
        StationData next = old.withState(
                live.furnace.saveWithoutMetadata(registries),
                live.smoker.saveWithoutMetadata(registries),
                live.blast.saveWithoutMetadata(registries),
                live.brewing.saveWithoutMetadata(registries),
                readyMask(live));
        if (!next.equals(old)) {
            set(player, next);
        }
    }

    // ---------- тік ----------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || !owns(player)) {
            return;
        }
        StationData data = get(player);
        Live live = LIVE.get(player.getUUID());
        if (live == null) {
            // Ще жодної станції не відкривали: нема що тікати
            if (!(has(data.furnace()) || has(data.smoker()) || has(data.blast()) || has(data.brewing()))) {
                return;
            }
            live = live(player);
        }

        ServerLevel level = player.serverLevel();
        live.level(level);
        if (live.furnace.active()) {
            AbstractFurnaceBlockEntity.serverTick(level, VOID, Blocks.FURNACE.defaultBlockState(), live.furnace);
        }
        if (live.smoker.active()) {
            AbstractFurnaceBlockEntity.serverTick(level, VOID, Blocks.SMOKER.defaultBlockState(), live.smoker);
        }
        if (live.blast.active()) {
            AbstractFurnaceBlockEntity.serverTick(level, VOID, Blocks.BLAST_FURNACE.defaultBlockState(), live.blast);
        }
        if (!live.brewing.isEmpty()) {
            BrewingStandBlockEntity.serverTick(level, VOID, Blocks.BREWING_STAND.defaultBlockState(), live.brewing);
        }

        if (player.tickCount % 10 == 0) {
            // Кінець варіння: час спадає до нуля. Сигнал "готово" знімається, коли гравець відкриє стійку.
            short brewTime = live.brewing.saveWithoutMetadata(level.registryAccess()).getShort("BrewTime");
            if (live.lastBrewTime > 0 && brewTime == 0) {
                live.brewFinished = 1;
            }
            live.lastBrewTime = brewTime;
        }
        if (player.tickCount % 20 == 0) {
            flush(player);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseExternal(player);
            if (LIVE.containsKey(player.getUUID())) {
                flush(player);
            }
        }
    }

    /** Запасний вихід: вікно вже не відкрите, а блок зовнішньої станції ще стоїть - прибираємо його. */
    @SubscribeEvent
    public static void onExternalTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !PENDING.isEmpty()
                && PENDING.containsKey(player.getUUID()) && player.containerMenu == player.inventoryMenu) {
            releaseExternal(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            releaseExternal(player);
        }
        PENDING.clear();
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseExternal(player);
            flush(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Вихід одразу після відкриття зовнішньої станції - майже напевно збій клієнта: запам'ятовуємо її
            Pending pending = PENDING.get(player.getUUID());
            if (pending != null && player.serverLevel().getGameTime() - pending.openedAt() <= 100L) {
                StationData data = get(player);
                CompoundTag all = data.external().copy();
                all.putBoolean("!" + pending.station().id, true);
                set(player, data.withExternal(all));
            }
            releaseExternal(player);
            flush(player);
            LIVE.remove(player.getUUID());
        }
    }

    // ---------- дії ----------

    public static void handle(ServerPlayer player, int action, int index) {
        if (!owns(player)) {
            return;
        }
        Station station = Station.byIndex(index);
        if (station != null && !station.available(get(player).blocks())) {
            return;
        }
        switch (action) {
            case StationActionPayload.OPEN -> {
                if (station != null) {
                    open(player, station);
                }
            }
            case StationActionPayload.BUY_STATION -> {
                if (station != null) {
                    buyStation(player, station);
                }
            }
            case StationActionPayload.BUY_PAGE -> buyPage(player);
            default -> { }
        }
    }

    private static void buyStation(ServerPlayer player, Station station) {
        StationData data = get(player);
        if (data.has(station)) {
            return;
        }
        if (data.unlockedCount() >= capacity(data)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.station.need_page"), true);
            return;
        }
        if (!SpiritUpgrades.pay(player, data.nextStationCost())) {
            return;
        }
        set(player, data.withUnlocked(station));
        player.displayClientMessage(
                Component.translatable("message.spirit_form.station.unlocked", station.displayName(data.blocks())), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private static void buyPage(ServerPlayer player) {
        StationData data = get(player);
        if (data.pages() >= Station.pages(data.blocks())) {
            return;
        }
        if (!SpiritUpgrades.pay(player, pageCost(data.pages()))) {
            return;
        }
        set(player, data.withPages(data.pages() + 1));
        player.displayClientMessage(
                Component.translatable("message.spirit_form.station.page_unlocked", data.pages() + 1), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.0F);
    }

    /**
     * Доступ до "позиції" для меню: усе, що ванільне меню робить через execute (результат крафту, повернення
     * предметів, звуки), працює з реальним світом, а перевірка відстані до блока завжди проходить.
     */
    private static ContainerLevelAccess access(ServerPlayer player, boolean safe) {
        Level level = player.level();
        // Ковадло може зіпсуватися на позиції доступу, тож для нього позиція під світом
        BlockPos pos = safe ? new BlockPos(player.getBlockX(), level.getMinBuildHeight() - 16, player.getBlockZ())
                : player.blockPosition();
        return new ContainerLevelAccess() {
            @Override
            public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> function) {
                return Optional.empty();
            }

            @Override
            public void execute(BiConsumer<Level, BlockPos> consumer) {
                consumer.accept(level, pos);
            }
        };
    }

    /** Стіл зачарування без книжкових полиць: усі три варіанти доступні з максимальною ціною (10, 20, 30 рівнів). */
    private static final class MaxEnchantMenu extends EnchantmentMenu {
        private final net.minecraft.world.entity.player.Player owner;

        MaxEnchantMenu(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
            this.owner = inventory.player;
        }

        @Override
        public void slotsChanged(Container container) {
            super.slotsChanged(container);
            if (container != this.getSlot(0).container) {
                return;
            }
            ItemStack stack = this.getSlot(0).getItem();
            if (!stack.isEmpty() && stack.isEnchantable()) {
                this.costs[0] = 10;
                this.costs[1] = 20;
                this.costs[2] = 30;
                // Справжні підказки (як у звичайному столі): без них екран сірить комірки й пише «обмеження»
                net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> registry =
                        this.owner.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                java.util.Optional<net.minecraft.core.HolderSet.Named<net.minecraft.world.item.enchantment.Enchantment>> tag =
                        registry.getTag(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE);
                for (int i = 0; i < 3; i++) {
                    this.enchantClue[i] = -1;
                    this.levelClue[i] = -1;
                    if (tag.isEmpty()) {
                        continue;
                    }
                    net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
                    random.setSeed((long) this.owner.getEnchantmentSeed() + i);
                    java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance> list =
                            net.minecraft.world.item.enchantment.EnchantmentHelper.selectEnchantment(
                                    random, stack, this.costs[i], tag.get().stream());
                    if (stack.is(net.minecraft.world.item.Items.BOOK) && list.size() > 1) {
                        list.remove(random.nextInt(list.size()));
                    }
                    if (!list.isEmpty()) {
                        net.minecraft.world.item.enchantment.EnchantmentInstance clue = list.get(random.nextInt(list.size()));
                        this.enchantClue[i] = registry.getId(clue.enchantment.value());
                        this.levelClue[i] = clue.level;
                    }
                }
                this.broadcastChanges();
            }
        }
    }

    /** Живий стан із актуальним рівнем гравця (він міг змінити вимір). */
    private static Live liveOf(ServerPlayer player) {
        Live live = live(player);
        live.level(player.serverLevel());
        return live;
    }

    private static void open(ServerPlayer player, Station station) {
        StationData data = get(player);
        if (!data.has(station)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.station.locked"), true);
            return;
        }
        if (station.external()) {
            openExternal(player, station);
            return;
        }

        switch (station) {
            case CRAFTING -> player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CraftingMenu(id, inv, access(player, false)),
                    Component.translatable("container.crafting")));
            case ENCHANTING -> player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new MaxEnchantMenu(id, inv, access(player, false)),
                    Component.translatable("container.enchant")));
            case ANVIL -> player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new AnvilMenu(id, inv, access(player, true)),
                    Component.translatable("container.repair")));
            case SMITHING -> player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new SmithingMenu(id, inv, access(player, false)),
                    Component.translatable("container.upgrade")));
            case STONECUTTER -> player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new StonecutterMenu(id, inv, access(player, false)),
                    Component.translatable("container.stonecutter")));
            case GRINDSTONE -> player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new GrindstoneMenu(id, inv, access(player, false)),
                    Component.translatable("container.grindstone_title")));
            case CARTOGRAPHY -> player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CartographyTableMenu(id, inv, access(player, false)),
                    Component.translatable("container.cartography_table")));
            case LOOM -> player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new LoomMenu(id, inv, access(player, false)),
                    Component.translatable("container.loom")));
            case FURNACE -> player.openMenu(liveOf(player).furnace);
            case SMOKER -> player.openMenu(liveOf(player).smoker);
            case BLAST_FURNACE -> player.openMenu(liveOf(player).blast);
            case BREWING -> {
                Live live = liveOf(player);
                live.brewFinished = 0;
                player.openMenu(live.brewing);
            }
        }
    }

    // ---------- станції з інших модів ----------

    /** Блок зовнішньої станції, що тимчасово стоїть у світі, поки відкрите його вікно. */
    private record Pending(ResourceKey<Level> dimension, BlockPos pos, Block block, Station station, long openedAt) {
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SpiritStations.class);
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** Без оновлення сусідів і без випадання предметів; клієнтам блок не надсилається. */
    private static final int QUIET = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /**
     * Імітує праве клацання по блоку іншого мода: ставить цей блок у вільну клітинку поруч, викликає його власну
     * обробку клацання (так відкривається саме його вікно), а при закритті вікна зберігає вміст і прибирає блок.
     */
    private static void openExternal(ServerPlayer player, Station station) {
        if (station.itemStation(get(player).blocks()) != null) {
            // Станція-предмет (книга Хронік Акаши): блок у світі не потрібен
            releaseExternal(player);
            if (!AkashicCompat.open(player)) {
                player.displayClientMessage(Component.translatable("message.spirit_form.station.external_failed"), true);
            }
            return;
        }
        Block block = station.externalBlock(get(player).blocks());
        if (block == null) {
            return;
        }
        releaseExternal(player);

        // Запобіжник: станція, що викинула гравця зі світу, не відкривається знову без Shift
        StationData current = get(player);
        if (current.external().getBoolean("!" + station.id)) {
            if (!player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("message.spirit_form.station.unstable"), true);
                return;
            }
            CompoundTag cleared = current.external().copy();
            cleared.remove("!" + station.id);
            set(player, current.withExternal(cleared));
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = freeCell(level, player);
        if (pos == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.station.no_room"), true);
            return;
        }

        BlockState state = block.defaultBlockState();
        level.setBlock(pos, state, QUIET);
        BlockEntity entity = level.getBlockEntity(pos);
        CompoundTag saved = get(player).external().getCompound(station.id);
        if (entity != null && !saved.isEmpty()) {
            try {
                entity.loadWithComponents(saved, level.registryAccess());
            } catch (RuntimeException ignored) {
                // Пошкоджений збережений вміст не має заважати відкрити станцію
            }
        }

        // Клієнт мусить знати про блок і його сутність ще до відкриття вікна: багато модів читають їх на клієнті
        if (player.connection != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
            if (entity != null) {
                net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> update =
                        entity.getUpdatePacket();
                if (update != null) {
                    player.connection.send(update);
                }
            }
        }

        boolean opened = false;
        try {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            ItemInteractionResult item = state.useItemOn(ItemStack.EMPTY, level, player, InteractionHand.MAIN_HAND, hit);
            if (item == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                state.useWithoutItem(level, player, hit);
            }
            opened = player.containerMenu != player.inventoryMenu;
        } catch (RuntimeException e) {
            LOGGER.warn("Зовнішня станція {} не відкрилася: {}", block, e.toString());
        }

        PENDING.put(player.getUUID(), new Pending(level.dimension(), pos, block, station, level.getGameTime()));
        if (!opened) {
            releaseExternal(player);
            player.displayClientMessage(Component.translatable("message.spirit_form.station.external_failed"), true);
        }
    }

    /** Найближча порожня клітинка в радіусі двох блоків (блок має стояти в межах дії вікна). */
    private static BlockPos freeCell(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)
                            || !level.getBlockState(pos).isAir() || level.getBlockEntity(pos) != null) {
                        continue;
                    }
                    int distance = dx * dx + dy * dy + dz * dz;
                    if (new net.minecraft.world.phys.AABB(pos).intersects(player.getBoundingBox())) {
                        distance += 100;
                    }
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    /** Зберігає вміст блока станції в даних гравця й прибирає блок зі світу. */
    private static void releaseExternal(ServerPlayer player) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null || player.getServer() == null) {
            return;
        }
        ServerLevel level = player.getServer().getLevel(pending.dimension());
        if (level == null || !level.getBlockState(pending.pos()).is(pending.block())) {
            return;
        }
        BlockEntity entity = level.getBlockEntity(pending.pos());
        if (entity != null) {
            StationData data = get(player);
            CompoundTag all = data.external().copy();
            all.put(pending.station().id, entity.saveWithoutMetadata(level.registryAccess()));
            set(player, data.withExternal(all));
            // Без сутності блок не викидає вміст у світ, коли зникає: усе вже збережене
            level.removeBlockEntity(pending.pos());
        }
        level.setBlock(pending.pos(), Blocks.AIR.defaultBlockState(), QUIET);
        if (player.connection != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(
                    pending.pos(), Blocks.AIR.defaultBlockState()));
        }
    }

    // ---------- керування списком зовнішніх станцій (меню розвитку) ----------

    public static final int EXT_ADD = 0;
    public static final int EXT_REMOVE = 1;

    public static void handleExternal(ServerPlayer player, int action, int cell, int slot) {
        if (!owns(player)) {
            return;
        }
        StationData data = get(player);
        if (action == EXT_ADD) {
            if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
                return;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            String id;
            if (stack.getItem() instanceof BlockItem blockItem) {
                id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString();
            } else if (AkashicCompat.isBook(stack.getItem())) {
                id = AkashicCompat.BOOK_ID.toString();
            } else {
                player.displayClientMessage(Component.translatable("message.spirit_form.station.not_block"), true);
                return;
            }
            int target = -1;
            for (int i = 0; i < StationData.EXTERNAL_MAX; i++) {
                if (id.equals(data.blockAt(i))) {
                    player.displayClientMessage(Component.translatable("message.spirit_form.station.duplicate"), true);
                    return;
                }
                if (data.blockAt(i).isEmpty() && (target < 0 || i == cell)) {
                    target = i;
                }
            }
            if (target < 0) {
                player.displayClientMessage(Component.translatable("message.spirit_form.station.ext_full"), true);
                return;
            }
            stack.shrink(1);
            set(player, data.withBlock(target, id));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
            return;
        }
        if (action == EXT_REMOVE) {
            if (cell < 0 || cell >= StationData.EXTERNAL_MAX || data.blockAt(cell).isEmpty()) {
                return;
            }
            releaseExternal(player);
            data = get(player);
            Station station = Station.byIndex(Station.BUILTIN + cell);
            Block block = station == null ? null : station.externalBlock(data.blocks());
            net.minecraft.world.item.Item itemStation = station == null ? null : station.itemStation(data.blocks());
            if (itemStation != null) {
                give(player, new ItemStack(itemStation));
            }
            if (block != null) {
                CompoundTag tag = data.external().getCompound(station.id);
                if (!tag.isEmpty() && block instanceof EntityBlock entityBlock) {
                    BlockEntity entity = entityBlock.newBlockEntity(VOID, block.defaultBlockState());
                    if (entity != null) {
                        try {
                            entity.loadWithComponents(tag, player.level().registryAccess());
                        } catch (RuntimeException ignored) {
                            // Нечитабельний вміст пропускаємо
                        }
                        if (entity instanceof Container container) {
                            for (int i = 0; i < container.getContainerSize(); i++) {
                                give(player, container.getItem(i).copy());
                            }
                        }
                    }
                }
                if (block.asItem() != net.minecraft.world.item.Items.AIR) {
                    give(player, new ItemStack(block.asItem()));
                }
            }
            CompoundTag all = data.external().copy();
            if (station != null) {
                all.remove(station.id);
            }
            StationData next = data.withBlock(cell, "").withExternal(all);
            if (station != null) {
                next = next.withUnlockedMask(next.unlocked() & ~station.bit());
            }
            set(player, next);
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
