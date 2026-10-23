package com.spiritform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Облік чинних Уламків Душі кожного гравця (зберігається у світі). Старіші за ліміт зникають. */
public class SoulShardIndex extends SavedData {

    public static final SavedData.Factory<SoulShardIndex> FACTORY =
            new SavedData.Factory<>(SoulShardIndex::new, SoulShardIndex::load, null);

    private final Map<UUID, List<UUID>> shards = new HashMap<>();

    public static SoulShardIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "spirit_form_shards");
    }

    /** Додає новий уламок. Повертає ті, що перевищили ліміт і мають зникнути. */
    public List<UUID> register(UUID owner, UUID shard, int limit) {
        List<UUID> list = shards.computeIfAbsent(owner, key -> new ArrayList<>());
        list.add(shard);
        List<UUID> removed = new ArrayList<>();
        while (list.size() > Math.max(1, limit)) {
            removed.add(list.remove(0));
        }
        setDirty();
        return removed;
    }

    /**
     * Чи лишається уламок чинним. Якщо запису про власника немає (облік загубився), уламок усиновлюємо,
     * щоб не втратити речі.
     */
    public boolean keep(UUID owner, UUID shard, int limit) {
        List<UUID> list = shards.get(owner);
        if (list == null || list.isEmpty()) {
            register(owner, shard, limit);
            return true;
        }
        return list.contains(shard);
    }

    public void remove(UUID owner, UUID shard) {
        List<UUID> list = shards.get(owner);
        if (list != null && list.remove(shard)) {
            setDirty();
        }
    }

    public static SoulShardIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        SoulShardIndex index = new SoulShardIndex();
        ListTag owners = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < owners.size(); i++) {
            CompoundTag entry = owners.getCompound(i);
            if (!entry.hasUUID("Owner")) {
                continue;
            }
            List<UUID> list = new ArrayList<>();
            ListTag ids = entry.getList("Shards", Tag.TAG_STRING);
            for (int j = 0; j < ids.size(); j++) {
                try {
                    list.add(UUID.fromString(ids.getString(j)));
                } catch (IllegalArgumentException ignored) {
                    // пошкоджений запис пропускаємо
                }
            }
            index.shards.put(entry.getUUID("Owner"), list);
        }
        return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag owners = new ListTag();
        for (Map.Entry<UUID, List<UUID>> entry : shards.entrySet()) {
            CompoundTag owner = new CompoundTag();
            owner.putUUID("Owner", entry.getKey());
            ListTag ids = new ListTag();
            for (UUID id : entry.getValue()) {
                ids.add(StringTag.valueOf(id.toString()));
            }
            owner.put("Shards", ids);
            owners.add(owner);
        }
        tag.put("Owners", owners);
        return tag;
    }
}
