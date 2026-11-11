package com.spiritform;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Скільки Водяників уже з'явилося у світі. Річки діляться на ділянки по CELL блоків, у кожній ділянці
 * Водяник з'являється лише один раз (як "один на біом").
 */
public class VodianykIndex extends SavedData {

    /** Розмір ділянки річки в блоках. */
    public static final int CELL = 224;

    public static final SavedData.Factory<VodianykIndex> FACTORY =
            new SavedData.Factory<>(VodianykIndex::new, VodianykIndex::load, null);

    private final Set<Long> cells = new HashSet<>();

    public static VodianykIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "spirit_form_vodianyks");
    }

    /** Ключ ділянки, якій належить точка. */
    public static long cellOf(int x, int z) {
        return ChunkPos.asLong(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
    }

    public boolean has(long cell) {
        return cells.contains(cell);
    }

    public void add(long cell) {
        if (cells.add(cell)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (long c : cells) {
            list.add(LongTag.valueOf(c));
        }
        tag.put("Cells", list);
        return tag;
    }

    public static VodianykIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        VodianykIndex index = new VodianykIndex();
        ListTag list = tag.getList("Cells", Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            index.cells.add(((LongTag) list.get(i)).getAsLong());
        }
        return index;
    }
}
