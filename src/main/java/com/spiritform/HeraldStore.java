package com.spiritform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Усе, що система Вісників мусить пам'ятати між перезапусками світу.
 *
 * Шкала, лічильник прогресії та черга Вісників персональні: у кожного гравця свої. Активний звичайний Вісник
 * у світі лише один (worldHerald), тож двоє гравців не створять двох одночасно.
 */
public class HeraldStore extends SavedData {

    /** Стан одного гравця. */
    public static final class Rec {
        /** Справжнє значення шкали в пунктах (100 = 100%). Може бути від'ємним і більшим за 100. */
        public int meter;
        /** Скільки потраплянь було після останньої появи Вісника (звідси береться приріст). */
        public int sinceSpawn;
        /** Скільки потраплянь було взагалі; ніколи не скидається. */
        public int total;
        /** Індекс наступного Вісника в циклі Буря, Голод, Холод. */
        public int next;
        /** Індекс активного Вісника в циклі або -1, якщо зустрічі немає. */
        public int activeKind = -1;
        public UUID activeHerald;
        /** Зустріч викликана командою для перевірки: після неї черга Вісників не зсувається. */
        public boolean activeForced;
        /** Ігровий час останнього зарахованого потрапляння (захист від подвійного підрахунку). */
        public long lastEntryTick = -1;
    }

    public static final SavedData.Factory<HeraldStore> FACTORY =
            new SavedData.Factory<>(HeraldStore::new, HeraldStore::load, null);

    private final Map<UUID, Rec> records = new HashMap<>();

    /** Активний звичайний Вісник і його власник. */
    public UUID worldHerald;
    public UUID worldOwner;

    /** Вісник Смерті (якщо він є) і той, хто його викликав. */
    public UUID deathHerald;
    public UUID deathOwner;

    /** Погода до втручання Вісників: щоб після їхнього зникнення повернути її. */
    public boolean weatherSaved;
    public boolean weatherRain;
    public boolean weatherThunder;

    /** Блоки, які змінила штучна зима: сніг на поверхні та лід на місці води (BlockPos.asLong). */
    public final Set<Long> winterSnow = new HashSet<>();
    public final Set<Long> winterIce = new HashSet<>();
    public String winterDimension = "";

    public static HeraldStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "spirit_form_heralds");
    }

    public Rec rec(UUID player) {
        return records.computeIfAbsent(player, id -> new Rec());
    }

    public Map<UUID, Rec> all() {
        return records;
    }

    public static HeraldStore load(CompoundTag tag, HolderLookup.Provider registries) {
        HeraldStore store = new HeraldStore();
        ListTag list = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Id")) {
                continue;
            }
            Rec rec = new Rec();
            rec.meter = entry.getInt("Meter");
            rec.sinceSpawn = entry.getInt("SinceSpawn");
            rec.total = entry.getInt("Total");
            rec.next = Math.floorMod(entry.getInt("Next"), HeraldType.CYCLE.length);
            rec.activeKind = entry.contains("ActiveKind") ? entry.getInt("ActiveKind") : -1;
            rec.activeHerald = entry.hasUUID("ActiveHerald") ? entry.getUUID("ActiveHerald") : null;
            rec.activeForced = entry.getBoolean("ActiveForced");
            rec.lastEntryTick = entry.contains("LastEntry") ? entry.getLong("LastEntry") : -1;
            store.records.put(entry.getUUID("Id"), rec);
        }
        store.worldHerald = tag.hasUUID("WorldHerald") ? tag.getUUID("WorldHerald") : null;
        store.worldOwner = tag.hasUUID("WorldOwner") ? tag.getUUID("WorldOwner") : null;
        store.deathHerald = tag.hasUUID("DeathHerald") ? tag.getUUID("DeathHerald") : null;
        store.deathOwner = tag.hasUUID("DeathOwner") ? tag.getUUID("DeathOwner") : null;
        store.weatherSaved = tag.getBoolean("WeatherSaved");
        store.weatherRain = tag.getBoolean("WeatherRain");
        store.weatherThunder = tag.getBoolean("WeatherThunder");
        store.winterDimension = tag.getString("WinterDimension");
        for (long value : tag.getLongArray("WinterSnow")) {
            store.winterSnow.add(value);
        }
        for (long value : tag.getLongArray("WinterIce")) {
            store.winterIce.add(value);
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Rec> entry : records.entrySet()) {
            Rec rec = entry.getValue();
            CompoundTag item = new CompoundTag();
            item.putUUID("Id", entry.getKey());
            item.putInt("Meter", rec.meter);
            item.putInt("SinceSpawn", rec.sinceSpawn);
            item.putInt("Total", rec.total);
            item.putInt("Next", rec.next);
            item.putInt("ActiveKind", rec.activeKind);
            if (rec.activeHerald != null) {
                item.putUUID("ActiveHerald", rec.activeHerald);
            }
            item.putBoolean("ActiveForced", rec.activeForced);
            item.putLong("LastEntry", rec.lastEntryTick);
            list.add(item);
        }
        tag.put("Players", list);
        if (worldHerald != null) {
            tag.putUUID("WorldHerald", worldHerald);
        }
        if (worldOwner != null) {
            tag.putUUID("WorldOwner", worldOwner);
        }
        if (deathHerald != null) {
            tag.putUUID("DeathHerald", deathHerald);
        }
        if (deathOwner != null) {
            tag.putUUID("DeathOwner", deathOwner);
        }
        tag.putBoolean("WeatherSaved", weatherSaved);
        tag.putBoolean("WeatherRain", weatherRain);
        tag.putBoolean("WeatherThunder", weatherThunder);
        tag.putString("WinterDimension", winterDimension);
        tag.putLongArray("WinterSnow", winterSnow.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("WinterIce", winterIce.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
}
