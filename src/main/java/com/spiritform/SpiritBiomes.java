package com.spiritform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Ключі біомів лісової родини Потойбіччя й тег "схожі на ліс" (дерева Мавки й Титан ростуть в усіх). */
public final class SpiritBiomes {

    public static final ResourceKey<Biome> NIGHT_FOREST = key("beyond_night_forest");
    public static final ResourceKey<Biome> MIST_FOREST = key("beyond_mist_forest");

    public static final ResourceKey<Biome> PLAINS = key("beyond_plains");

    /** Жарокрай: вулканічний біом з Титанічним вулканом. */
    public static final ResourceKey<Biome> HEATLAND = key("beyond_heatland");

    /** data/spirit_form/tags/worldgen/biome/forest_like.json */
    public static final TagKey<Biome> FOREST_LIKE = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "forest_like"));

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, name));
    }

    private SpiritBiomes() {
    }
}
