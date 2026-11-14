package com.spiritform.data;

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

    /** Де лежить кожен уламок (виміри та координати), щоб Чуття могло вести до нього навіть із незавантаженого чанка. */
    public record Position(String dimension, double x, double y, double z) {
    }

    private final Map<UUID, Position> positions = new HashMap<>();
    /** Копія предметів кожного уламка: з неї команду повернення відновлює втрачений уламок. */
    private final Map<UUID, List<CompoundTag>> backups = new HashMap<>();

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
        for (UUID id : removed) {
            positions.remove(id);
            backups.remove(id);
        }
        setDirty();
        return removed;
    }

    /** Усі чинні уламки гравця від найстаріших до найновіших. */
    public List<UUID> shardsOf(UUID owner) {
        List<UUID> list = shards.get(owner);
        return list == null ? List.of() : new ArrayList<>(list);
    }

    public Position position(UUID shard) {
        return positions.get(shard);
    }

    public List<CompoundTag> backupOf(UUID shard) {
        return backups.get(shard);
    }

    /**
     * Запам'ятовує місце й предмети уламка. Повертає true, якщо щось змінилося
     * (тоді гравцеві треба оновити мітки).
     */
    public boolean touch(UUID shard, Position position, List<CompoundTag> items) {
        boolean changed = false;
        if (!position.equals(positions.get(shard))) {
            positions.put(shard, position);
            changed = true;
        }
        if (!backups.containsKey(shard)) {
            backups.put(shard, new ArrayList<>(items));
            changed = true;
        }
        if (changed) {
            setDirty();
        }
        return changed;
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
        boolean changed = list != null && list.remove(shard);
        changed |= positions.remove(shard) != null;
        changed |= backups.remove(shard) != null;
        if (changed) {
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
        ListTag places = tag.getList("Positions", Tag.TAG_COMPOUND);
        for (int i = 0; i < places.size(); i++) {
            CompoundTag entry = places.getCompound(i);
            if (entry.hasUUID("Id")) {
                index.positions.put(entry.getUUID("Id"), new Position(entry.getString("Dim"),
                        entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")));
            }
        }
        ListTag saved = tag.getList("Backups", Tag.TAG_COMPOUND);
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag entry = saved.getCompound(i);
            if (entry.hasUUID("Id")) {
                List<CompoundTag> items = new ArrayList<>();
                ListTag list = entry.getList("Items", Tag.TAG_COMPOUND);
                for (int j = 0; j < list.size(); j++) {
                    items.add(list.getCompound(j));
                }
                index.backups.put(entry.getUUID("Id"), items);
            }
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

        ListTag places = new ListTag();
        for (Map.Entry<UUID, Position> entry : positions.entrySet()) {
            CompoundTag place = new CompoundTag();
            place.putUUID("Id", entry.getKey());
            place.putString("Dim", entry.getValue().dimension());
            place.putDouble("X", entry.getValue().x());
            place.putDouble("Y", entry.getValue().y());
            place.putDouble("Z", entry.getValue().z());
            places.add(place);
        }
        tag.put("Positions", places);

        ListTag saved = new ListTag();
        for (Map.Entry<UUID, List<CompoundTag>> entry : backups.entrySet()) {
            CompoundTag backup = new CompoundTag();
            backup.putUUID("Id", entry.getKey());
            ListTag items = new ListTag();
            items.addAll(entry.getValue());
            backup.put("Items", items);
            saved.add(backup);
        }
        tag.put("Backups", saved);
        return tag;
    }
}
