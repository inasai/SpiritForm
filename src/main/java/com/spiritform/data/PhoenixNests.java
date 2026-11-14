package com.spiritform.data;

import com.spiritform.worldgen.HeatlandSites;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Коли над кожним вулканом може з'явитися нова Жар-птиця (після загибелі діє перезарядка). */
public class PhoenixNests extends SavedData {

    public static final SavedData.Factory<PhoenixNests> FACTORY =
            new SavedData.Factory<>(PhoenixNests::new, PhoenixNests::load, null);

    private final Map<Long, Long> next = new HashMap<>();

    public static PhoenixNests get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "spirit_form_phoenix_nests");
    }

    public static long key(HeatlandSites.Volcano v) {
        return ((long) v.cone().x() << 32) ^ (v.cone().z() & 0xFFFFFFFFL);
    }

    public long nextSpawn(long key) {
        return next.getOrDefault(key, 0L);
    }

    public void setNext(long key, long gameTime) {
        next.put(key, gameTime);
        setDirty();
    }

    public static PhoenixNests load(CompoundTag tag, HolderLookup.Provider registries) {
        PhoenixNests nests = new PhoenixNests();
        CompoundTag map = tag.getCompound("Next");
        for (String k : map.getAllKeys()) {
            try {
                nests.next.put(Long.parseLong(k), map.getLong(k));
            } catch (NumberFormatException ignored) {
                // пошкоджений запис пропускаємо
            }
        }
        return nests;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag map = new CompoundTag();
        for (Map.Entry<Long, Long> e : next.entrySet()) {
            map.putLong(Long.toString(e.getKey()), e.getValue());
        }
        tag.put("Next", map);
        return tag;
    }
}
