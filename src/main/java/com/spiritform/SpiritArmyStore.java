package com.spiritform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Сховище армії кожного гравця: список збережених душ. Зберігається у світі, тож переживає смерть гравця. */
public class SpiritArmyStore extends SavedData {

    /** Душа: тип моба, його повні характеристики (NBT), ім'я (якщо було) і здоров'я. */
    public static final class Soul {
        public final UUID id;
        public final String type;
        public final String name;
        public final float maxHealth;
        public final CompoundTag nbt;

        public Soul(UUID id, String type, String name, float maxHealth, CompoundTag nbt) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.maxHealth = maxHealth;
            this.nbt = nbt;
        }

        public static Soul fromTag(UUID id, CompoundTag tag) {
            return new Soul(id, tag.getString("type"), tag.getString("name"), tag.getFloat("hp"), tag.getCompound("nbt"));
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("type", type);
            tag.putString("name", name);
            tag.putFloat("hp", maxHealth);
            tag.put("nbt", nbt);
            return tag;
        }
    }

    public static final SavedData.Factory<SpiritArmyStore> FACTORY =
            new SavedData.Factory<>(SpiritArmyStore::new, SpiritArmyStore::load, null);

    private final Map<UUID, List<Soul>> souls = new HashMap<>();

    public static SpiritArmyStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "spirit_form_army");
    }

    /** Душі гравця (список можна змінювати, після змін викликайте setDirty). */
    public List<Soul> list(UUID owner) {
        return souls.computeIfAbsent(owner, key -> new ArrayList<>());
    }

    public static SpiritArmyStore load(CompoundTag tag, HolderLookup.Provider registries) {
        SpiritArmyStore store = new SpiritArmyStore();
        ListTag owners = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < owners.size(); i++) {
            CompoundTag entry = owners.getCompound(i);
            if (!entry.hasUUID("Owner")) {
                continue;
            }
            List<Soul> list = store.list(entry.getUUID("Owner"));
            ListTag items = entry.getList("Souls", Tag.TAG_COMPOUND);
            for (int j = 0; j < items.size(); j++) {
                CompoundTag item = items.getCompound(j);
                if (item.hasUUID("Id")) {
                    list.add(Soul.fromTag(item.getUUID("Id"), item));
                }
            }
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag owners = new ListTag();
        for (Map.Entry<UUID, List<Soul>> entry : souls.entrySet()) {
            CompoundTag owner = new CompoundTag();
            owner.putUUID("Owner", entry.getKey());
            ListTag items = new ListTag();
            for (Soul soul : entry.getValue()) {
                items.add(soul.toTag());
            }
            owner.put("Souls", items);
            owners.add(owner);
        }
        tag.put("Owners", owners);
        return tag;
    }
}
