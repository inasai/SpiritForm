package com.spiritform.worldgen;

import com.spiritform.SpiritFormMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Реєстрація власних фіч генерації світу: Дерево Мавки й фільтр "не біля Дерева Мавки". */
public final class SpiritWorldgen {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, SpiritFormMod.MODID);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, SpiritFormMod.MODID);

    public static final DeferredHolder<Feature<?>, MavkaTreeFeature> MAVKA_TREE =
            FEATURES.register("mavka_tree", () -> new MavkaTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, TitanTreeFeature> TITAN_TREE =
            FEATURES.register("titan_tree", () -> new TitanTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, TitanRootsFeature> TITAN_ROOTS =
            FEATURES.register("titan_roots", () -> new TitanRootsFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, SpiritLeavesFeature> SPIRIT_LEAVES_SCATTER =
            FEATURES.register("spirit_leaves_scatter", () -> new SpiritLeavesFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, GlowFernFeature> GLOW_FERN_PATCH =
            FEATURES.register("glow_fern_patch", () -> new GlowFernFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, PoludnytsiaRingsFeature> POLUDNYTSIA_RINGS =
            FEATURES.register("poludnytsia_rings", () -> new PoludnytsiaRingsFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HeatlandTerrainFeature> HEATLAND_TERRAIN =
            FEATURES.register("heatland_terrain", () -> new HeatlandTerrainFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<NotNearMavkaTreeFilter>> NOT_NEAR_MAVKA_TREE =
            PLACEMENT_TYPES.register("not_near_mavka_tree", () -> () -> NotNearMavkaTreeFilter.CODEC);

    private SpiritWorldgen() {
    }
}
