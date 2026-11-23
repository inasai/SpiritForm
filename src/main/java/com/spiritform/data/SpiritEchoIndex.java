package com.spiritform.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Де лежить Відлуння кожного гравця (вимір і позиція). Зберігається у світі. */
public class SpiritEchoIndex extends SavedData {

    public record Entry(String dimension, BlockPos pos) {
    }

    public static final SavedData.Factory<SpiritEchoIndex> FACTORY =
            new SavedData.Factory<>(SpiritEchoIndex::new, SpiritEchoIndex::load, null);

    /** Одне Відлуння Смерті: вимір, позиція й назва (порожня = стандартна). */
    public record Echo(String dimension, BlockPos pos, String name) {
    }

    /** Відлуння кожного гравця (їх може бути кілька), у порядку створення. */
    private final Map<UUID, List<Echo>> echoes = new HashMap<>();

    /** Звідки гравець зайшов у Потойбіччя: туди він повертається. */
    private final Map<UUID, Entry> returns = new HashMap<>();

    /** Де гравець востаннє був у Потойбіччі: туди він потрапляє наступного разу. */
    private final Map<UUID, Entry> exits = new HashMap<>();

    public static SpiritEchoIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "spirit_form_echoes");
    }

    /** Відлуння гравця (копія списку). */
    public List<Echo> echoesOf(UUID owner) {
        List<Echo> list = echoes.get(owner);
        return list == null ? List.of() : List.copyOf(list);
    }

    public void add(UUID owner, Echo echo) {
        echoes.computeIfAbsent(owner, key -> new ArrayList<>()).add(echo);
        setDirty();
    }

    /** Забирає Відлуння з обліку й повертає його (null, якщо такого номера немає). */
    public Echo removeAt(UUID owner, int index) {
        List<Echo> list = echoes.get(owner);
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        Echo removed = list.remove(index);
        if (list.isEmpty()) {
            echoes.remove(owner);
        }
        setDirty();
        return removed;
    }

    public void rename(UUID owner, int index, String name) {
        List<Echo> list = echoes.get(owner);
        if (list != null && index >= 0 && index < list.size()) {
            Echo old = list.get(index);
            list.set(index, new Echo(old.dimension(), old.pos(), name));
            setDirty();
        }
    }

    public Entry getExit(UUID owner) {
        return exits.get(owner);
    }

    public void putExit(UUID owner, Entry entry) {
        if (!entry.equals(exits.get(owner))) {
            exits.put(owner, entry);
            setDirty();
        }
    }

    public Entry getReturn(UUID owner) {
        return returns.get(owner);
    }

    public void putReturn(UUID owner, Entry entry) {
        returns.put(owner, entry);
        setDirty();
    }

    public void removeReturn(UUID owner) {
        if (returns.remove(owner) != null) {
            setDirty();
        }
    }

    public static SpiritEchoIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        SpiritEchoIndex index = new SpiritEchoIndex();
        ListTag list = tag.getList("Echoes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Owner")) {
                continue;
            }
            index.echoes.computeIfAbsent(entry.getUUID("Owner"), key -> new ArrayList<>())
                    .add(new Echo(entry.getString("Dim"), BlockPos.of(entry.getLong("Pos")), entry.getString("Name")));
        }
        ListTag back = tag.getList("Returns", Tag.TAG_COMPOUND);
        for (int i = 0; i < back.size(); i++) {
            CompoundTag entry = back.getCompound(i);
            if (entry.hasUUID("Owner")) {
                index.returns.put(entry.getUUID("Owner"),
                        new Entry(entry.getString("Dim"), BlockPos.of(entry.getLong("Pos"))));
            }
        }
        ListTag out = tag.getList("Exits", Tag.TAG_COMPOUND);
        for (int i = 0; i < out.size(); i++) {
            CompoundTag entry = out.getCompound(i);
            if (entry.hasUUID("Owner")) {
                index.exits.put(entry.getUUID("Owner"),
                        new Entry(entry.getString("Dim"), BlockPos.of(entry.getLong("Pos"))));
            }
        }
        return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, List<Echo>> entry : echoes.entrySet()) {
            for (Echo echo : entry.getValue()) {
                CompoundTag item = new CompoundTag();
                item.putUUID("Owner", entry.getKey());
                item.putString("Dim", echo.dimension());
                item.putLong("Pos", echo.pos().asLong());
                item.putString("Name", echo.name());
                list.add(item);
            }
        }
        tag.put("Echoes", list);
        ListTag back = new ListTag();
        for (Map.Entry<UUID, Entry> entry : returns.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("Owner", entry.getKey());
            item.putString("Dim", entry.getValue().dimension());
            item.putLong("Pos", entry.getValue().pos().asLong());
            back.add(item);
        }
        tag.put("Returns", back);
        ListTag out = new ListTag();
        for (Map.Entry<UUID, Entry> entry : exits.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("Owner", entry.getKey());
            item.putString("Dim", entry.getValue().dimension());
            item.putLong("Pos", entry.getValue().pos().asLong());
            out.add(item);
        }
        tag.put("Exits", out);
        return tag;
    }
}
