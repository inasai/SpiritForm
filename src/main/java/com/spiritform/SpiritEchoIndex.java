package com.spiritform;

import java.util.HashMap;
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

    private final Map<UUID, Entry> echoes = new HashMap<>();

    /** Звідки гравець зайшов у Потойбіччя: туди він повертається. */
    private final Map<UUID, Entry> returns = new HashMap<>();

    /** Де гравець востаннє був у Потойбіччі: туди він потрапляє наступного разу. */
    private final Map<UUID, Entry> exits = new HashMap<>();

    public static SpiritEchoIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "spirit_form_echoes");
    }

    public Entry get(UUID owner) {
        return echoes.get(owner);
    }

    public void put(UUID owner, Entry entry) {
        echoes.put(owner, entry);
        setDirty();
    }

    public void remove(UUID owner) {
        if (echoes.remove(owner) != null) {
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
            index.echoes.put(entry.getUUID("Owner"),
                    new Entry(entry.getString("Dim"), BlockPos.of(entry.getLong("Pos"))));
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
        for (Map.Entry<UUID, Entry> entry : echoes.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putUUID("Owner", entry.getKey());
            item.putString("Dim", entry.getValue().dimension());
            item.putLong("Pos", entry.getValue().pos().asLong());
            list.add(item);
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
