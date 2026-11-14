package com.spiritform.vault;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.data.SpiritAttachments;
import com.spiritform.data.VaultData;
import com.spiritform.form.SpiritUpgrades;
import com.spiritform.registry.SpiritItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Сховище артефактів і спорядження (дані в VaultData, вікно в меню розвитку на H).
 *
 * Артефакт діє, поки лежить у відкритій комірці сховища й не вимкнений гравцем. Усі артефакти мода перевіряють
 * свою присутність через SpiritAccessories, а той тепер питає й сховище, тож самі предмети нічого про сховище не знають.
 * Спорядження в комірках діє віртуально: броня дає свої показники (лише коли відповідний слот персонажа порожній,
 * щоб не подвоювати захист), предмети мода працюють завжди, предмети в руках діють лише пасивно.
 * Предмети в сховищі не випадають після смерті.
 *
 * Додати новий артефакт: допишіть його до ARTIFACTS, і з'явиться ще одна комірка для розблокування.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritVault {

    public static final int KIND_ARTIFACT = 0;
    public static final int KIND_GEAR = 1;

    public static final int ACT_PUT = 0;
    public static final int ACT_TAKE = 1;
    public static final int ACT_UNLOCK = 2;
    public static final int ACT_TOGGLE_VISIBLE = 3;
    public static final int ACT_TOGGLE_ACTIVE = 4;

    /** Артефакти, які можна спорядити. Кількість комірок артефактів дорівнює розміру цього списку. */
    public static final List<Supplier<? extends Item>> ARTIFACTS = List.of(
            SpiritItems.FENTONS_BRACELET,
            SpiritItems.NECKLACE,
            SpiritItems.GLOVES_OF_THE_LIVING,
            SpiritItems.SOUL_PLUNDER,
            SpiritItems.SHACKLES,
            SpiritItems.SOUL_PULL,
            SpiritItems.SOUL_MEMORY
    );

    /** Комірки спорядження по порядку: шолом, нагрудник, штани, чоботи, основна рука, допоміжна рука. */
    public static final EquipmentSlot[] GEAR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();

    private record Applied(Holder<Attribute> attribute, ResourceLocation id) {
    }

    private static final Map<UUID, List<Applied>> APPLIED = new HashMap<>();

    private SpiritVault() {
    }

    // ---------- дані ----------

    public static VaultData get(Player player) {
        return player.getData(SpiritAttachments.VAULT.get());
    }

    private static void set(Player player, VaultData data) {
        player.setData(SpiritAttachments.VAULT.get(), data);
    }

    public static int maxArtifactCells() {
        return ARTIFACTS.size();
    }

    public static boolean isArtifact(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (Supplier<? extends Item> artifact : ARTIFACTS) {
            if (stack.is(artifact.get())) {
                return true;
            }
        }
        return false;
    }

    /** Чи підходить предмет до комірки: артефакти в комірки артефактів, броня у свій слот, у руки будь-що. */
    public static boolean eligible(int kind, int cell, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (kind == KIND_ARTIFACT) {
            return isArtifact(stack);
        }
        if (cell < 0 || cell >= GEAR_SLOTS.length) {
            return false;
        }
        if (cell >= 4) {
            return true;
        }
        return stack.getItem() instanceof Equipable equipable && equipable.getEquipmentSlot() == GEAR_SLOTS[cell];
    }

    private static boolean exists(int kind, int cell) {
        return cell >= 0 && cell < (kind == KIND_ARTIFACT ? maxArtifactCells() : VaultData.GEAR_SIZE);
    }

    public static boolean unlocked(VaultData data, int kind, int cell) {
        return kind == KIND_ARTIFACT ? data.artifactUnlocked(cell) : data.gearUnlocked(cell);
    }

    public static ItemStack stackAt(VaultData data, int kind, int cell) {
        return kind == KIND_ARTIFACT ? data.artifact(cell) : data.gearStack(cell);
    }

    public static boolean hiddenAt(VaultData data, int kind, int cell) {
        return kind == KIND_ARTIFACT ? data.artifactHidden(cell) : data.gearHidden(cell);
    }

    public static boolean disabledAt(VaultData data, int kind, int cell) {
        return kind == KIND_ARTIFACT ? data.artifactDisabled(cell) : data.gearDisabled(cell);
    }

    /** Скільки комірок цього виду відкрито. */
    public static int unlockedCount(VaultData data, int kind) {
        return kind == KIND_ARTIFACT ? data.artifactCells() : data.gearUnlockedCount();
    }

    public static int maxCells(int kind) {
        return kind == KIND_ARTIFACT ? maxArtifactCells() : VaultData.GEAR_SIZE;
    }

    /** Вартість відкриття наступної комірки: N-та коштує N рівнів досвіду. */
    public static int nextCost(VaultData data, int kind) {
        return unlockedCount(data, kind) + 1;
    }

    private static VaultData withStack(VaultData data, int kind, int cell, ItemStack stack) {
        // Новий предмет у комірці завжди починає видимим і ввімкненим
        return kind == KIND_ARTIFACT
                ? data.withArtifact(cell, stack).withArtifactHidden(cell, false).withArtifactDisabled(cell, false)
                : data.withGear(cell, stack).withGearHidden(cell, false).withGearDisabled(cell, false);
    }

    // ---------- запити від предметів мода (через SpiritAccessories) ----------

    /** Стан предмета в сховищі: ніде, прихований на персонажі або видимий. Вимкнені гравцем предмети не рахуються. */
    public static SpiritAccessories.State state(LivingEntity entity, Item item) {
        if (!(entity instanceof Player player)) {
            return SpiritAccessories.State.NONE;
        }
        VaultData data = get(player);
        SpiritAccessories.State result = SpiritAccessories.State.NONE;
        for (int i = 0; i < data.artifactCells(); i++) {
            if (data.artifact(i).is(item) && !data.artifactDisabled(i)) {
                if (!data.artifactHidden(i)) {
                    return SpiritAccessories.State.VISIBLE;
                }
                result = SpiritAccessories.State.HIDDEN;
            }
        }
        for (int i = 0; i < VaultData.GEAR_SIZE; i++) {
            if (data.gearUnlocked(i) && data.gearStack(i).is(item) && !data.gearDisabled(i)) {
                if (!data.gearHidden(i)) {
                    return SpiritAccessories.State.VISIBLE;
                }
                result = SpiritAccessories.State.HIDDEN;
            }
        }
        return result;
    }

    /** Сам предмет зі сховища (його можна змінювати, наприклад міцність) або порожній стек. */
    public static ItemStack findStack(LivingEntity entity, Item item) {
        if (!(entity instanceof Player player)) {
            return ItemStack.EMPTY;
        }
        VaultData data = get(player);
        for (int i = 0; i < data.artifactCells(); i++) {
            if (data.artifact(i).is(item) && !data.artifactDisabled(i)) {
                return data.artifact(i);
            }
        }
        for (int i = 0; i < VaultData.GEAR_SIZE; i++) {
            if (data.gearUnlocked(i) && data.gearStack(i).is(item) && !data.gearDisabled(i)) {
                return data.gearStack(i);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Номери комірок рук у сховищі спорядження. */
    public static final int CELL_MAIN_HAND = 4;
    public static final int CELL_OFF_HAND = 5;

    /**
     * Предмет у комірці руки (CELL_MAIN_HAND або CELL_OFF_HAND), якщо комірку відкрито, "живлення" не вимкнено
     * і це потрібний предмет. Повертає сам стек зі сховища (його можна змінювати) або порожній стек.
     */
    public static ItemStack handStack(Player player, int cell, Item item) {
        VaultData data = get(player);
        if (!data.gearUnlocked(cell) || data.gearDisabled(cell)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = data.gearStack(cell);
        return stack.is(item) ? stack : ItemStack.EMPTY;
    }

    /**
     * Предмет мода в комірці спорядження для цього слота діє, якщо комірку відкрито й дію не вимкнено.
     * Справжній слот персонажа не обмежує: ефекти Обладунків Потойбіччя працюють, навіть коли на персонажі
     * одягнена інша броня. (Показники самої броні додаються лише в порожній слот, див. refreshGear.)
     */
    public static boolean wearsGear(Player player, EquipmentSlot slot, Item item) {
        for (int i = 0; i < 4; i++) {
            if (GEAR_SLOTS[i] != slot) {
                continue;
            }
            VaultData data = get(player);
            return data.gearUnlocked(i) && !data.gearDisabled(i) && data.gearStack(i).is(item);
        }
        return false;
    }

    /**
     * Зміни предметів у сховищі "на місці" (міцність) самі собою не синхронізуються з клієнтом.
     * Цей виклик створює свіжу копію даних і записує її, тож вікно H бачить актуальну міцність.
     */
    public static void touch(Player player) {
        VaultData data = get(player);
        set(player, data.withArtifactCells(data.artifactCells()));
    }

    // ---------- бій ----------

    private static void markCombat(ServerPlayer player) {
        LAST_COMBAT.put(player.getUUID(), player.level().getGameTime());
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof ServerPlayer victim && attacker != null) {
            markCombat(victim);
        }
        if (attacker instanceof ServerPlayer player && event.getEntity() != player) {
            markCombat(player);
        }
    }

    private static boolean inCombat(ServerPlayer player) {
        int seconds = Config.VAULT_COMBAT_SECONDS.get();
        if (seconds <= 0) {
            return false;
        }
        Long last = LAST_COMBAT.get(player.getUUID());
        return last != null && player.level().getGameTime() - last < seconds * 20L;
    }

    // ---------- дії гравця ----------

    private static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    public static void handle(ServerPlayer player, int action, int kind, int cell, int slot) {
        if (kind != KIND_ARTIFACT && kind != KIND_GEAR) {
            return;
        }
        switch (action) {
            case ACT_PUT -> put(player, kind, cell, slot);
            case ACT_TAKE -> take(player, kind, cell);
            case ACT_UNLOCK -> unlock(player, kind, cell);
            case ACT_TOGGLE_VISIBLE -> toggle(player, kind, cell, true);
            case ACT_TOGGLE_ACTIVE -> toggle(player, kind, cell, false);
            default -> {
            }
        }
        refreshGear(player);
    }

    /** Перша підходяща вільна відкрита комірка для предмета або -1. */
    private static int firstFree(VaultData data, int kind, ItemStack stack) {
        for (int i = 0; i < maxCells(kind); i++) {
            if (unlocked(data, kind, i) && stackAt(data, kind, i).isEmpty() && eligible(kind, i, stack)) {
                return i;
            }
        }
        return -1;
    }

    private static void put(ServerPlayer player, int kind, int cell, int slot) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= 36) {
            return;
        }
        ItemStack source = inventory.getItem(slot);
        if (source.isEmpty()) {
            return;
        }
        if (inCombat(player)) {
            say(player, "message.spirit_form.vault.combat");
            return;
        }

        VaultData data = get(player);
        int target = cell < 0 ? firstFree(data, kind, source) : cell;
        if (target < 0 || !exists(kind, target)) {
            say(player, "message.spirit_form.vault.not_allowed");
            return;
        }
        if (!unlocked(data, kind, target)) {
            say(player, "message.spirit_form.vault.locked");
            return;
        }
        if (!eligible(kind, target, source)) {
            say(player, "message.spirit_form.vault.not_allowed");
            return;
        }
        if (kind == KIND_ARTIFACT) {
            // Один артефакт займає одну комірку: однакові не складаються
            for (int i = 0; i < maxCells(kind); i++) {
                if (i != target && data.artifact(i).is(source.getItem())) {
                    say(player, "message.spirit_form.vault.duplicate");
                    return;
                }
            }
        }

        ItemStack old = stackAt(data, kind, target);
        ItemStack moved = source.split(1);
        set(player, withStack(data, kind, target, moved));

        if (!old.isEmpty()) {
            if (source.isEmpty()) {
                inventory.setItem(slot, old);
            } else {
                inventory.placeItemBackInInventory(old);
            }
        } else if (source.isEmpty()) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static void take(ServerPlayer player, int kind, int cell) {
        VaultData data = get(player);
        if (!exists(kind, cell) || !unlocked(data, kind, cell)) {
            return;
        }
        ItemStack stack = stackAt(data, kind, cell);
        if (stack.isEmpty()) {
            return;
        }
        if (inCombat(player)) {
            say(player, "message.spirit_form.vault.combat");
            return;
        }
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) {
            say(player, "message.spirit_form.vault.no_space");
            return;
        }
        set(player, withStack(data, kind, cell, ItemStack.EMPTY));
    }

    private static void unlock(ServerPlayer player, int kind, int cell) {
        VaultData data = get(player);
        int count = unlockedCount(data, kind);
        if (count >= maxCells(kind)) {
            say(player, "message.spirit_form.vault.max");
            return;
        }
        if (kind == KIND_GEAR && (!exists(kind, cell) || data.gearUnlocked(cell))) {
            return;
        }
        if (!SpiritUpgrades.pay(player, count + 1)) {
            return;
        }
        set(player, kind == KIND_ARTIFACT ? data.withArtifactCells(count + 1) : data.withGearUnlocked(cell));
        say(player, "message.spirit_form.vault.unlocked");
    }

    private static void toggle(ServerPlayer player, int kind, int cell, boolean visual) {
        VaultData data = get(player);
        if (!exists(kind, cell) || !unlocked(data, kind, cell) || stackAt(data, kind, cell).isEmpty()) {
            return;
        }
        if (visual) {
            boolean hidden = hiddenAt(data, kind, cell);
            set(player, kind == KIND_ARTIFACT ? data.withArtifactHidden(cell, !hidden) : data.withGearHidden(cell, !hidden));
        } else {
            boolean disabled = disabledAt(data, kind, cell);
            set(player, kind == KIND_ARTIFACT ? data.withArtifactDisabled(cell, !disabled) : data.withGearDisabled(cell, !disabled));
        }
    }

    // ---------- віртуальна броня ----------

    /**
     * Показники броні з комірок шолома, нагрудника, штанів і чобіт додаються гравцеві тимчасовими модифікаторами
     * з окремими ідентифікаторами, тож не плутаються з модифікаторами справжньої броні. Руки показників не дають.
     */
    public static void refreshGear(ServerPlayer player) {
        VaultData data = get(player);
        Map<ResourceLocation, Holder<Attribute>> holders = new HashMap<>();
        Map<ResourceLocation, AttributeModifier> modifiers = new HashMap<>();

        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = GEAR_SLOTS[i];
            ItemStack stack = data.gearStack(i);
            if (!data.gearUnlocked(i) || data.gearDisabled(i) || stack.isEmpty() || !player.getItemBySlot(slot).isEmpty()) {
                continue;
            }
            stack.forEachModifier(slot, (attribute, modifier) -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID,
                        "vault/" + slot.getName() + "/" + modifier.id().getNamespace() + "/" + modifier.id().getPath());
                holders.put(id, attribute);
                modifiers.put(id, new AttributeModifier(id, modifier.amount(), modifier.operation()));
            });
        }

        UUID uuid = player.getUUID();
        for (Applied applied : APPLIED.getOrDefault(uuid, List.of())) {
            if (!modifiers.containsKey(applied.id())) {
                AttributeInstance instance = player.getAttribute(applied.attribute());
                if (instance != null) {
                    instance.removeModifier(applied.id());
                }
            }
        }

        List<Applied> now = new ArrayList<>();
        for (Map.Entry<ResourceLocation, AttributeModifier> entry : modifiers.entrySet()) {
            Holder<Attribute> holder = holders.get(entry.getKey());
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) {
                continue;
            }
            AttributeModifier existing = instance.getModifier(entry.getKey());
            AttributeModifier wanted = entry.getValue();
            if (existing == null || existing.amount() != wanted.amount() || existing.operation() != wanted.operation()) {
                instance.removeModifier(entry.getKey());
                instance.addTransientModifier(wanted);
            }
            now.add(new Applied(holder, entry.getKey()));
        }

        if (now.isEmpty()) {
            APPLIED.remove(uuid);
        } else {
            APPLIED.put(uuid, now);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) {
            refreshGear(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            APPLIED.remove(player.getUUID());
            refreshGear(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        APPLIED.remove(event.getEntity().getUUID());
        LAST_COMBAT.remove(event.getEntity().getUUID());
    }
}
