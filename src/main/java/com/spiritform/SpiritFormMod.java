package com.spiritform;

import com.spiritform.data.SpiritAttachments;
import com.spiritform.registry.SpiritArmorMaterials;
import com.spiritform.registry.SpiritBlocks;
import com.spiritform.registry.SpiritCreatures;
import com.spiritform.registry.SpiritEffects;
import com.spiritform.registry.SpiritEntities;
import com.spiritform.registry.SpiritGameRules;
import com.spiritform.registry.SpiritHeraldEntities;
import com.spiritform.registry.SpiritItems;
import com.spiritform.registry.SpiritRecipes;
import com.spiritform.worldgen.SpiritWorldgen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(SpiritFormMod.MODID)
public class SpiritFormMod {

    public static final String MODID = "spirit_form";

    public SpiritFormMod(IEventBus modEventBus, ModContainer modContainer) {
        SpiritAttachments.ATTACHMENT_TYPES.register(modEventBus);
        SpiritBlocks.BLOCKS.register(modEventBus);
        SpiritBlocks.BLOCK_ENTITIES.register(modEventBus);
        SpiritArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        SpiritItems.ITEMS.register(modEventBus);
        SpiritRecipes.SERIALIZERS.register(modEventBus);
        SpiritEffects.EFFECTS.register(modEventBus);
        SpiritGameRules.init();
        SpiritCreatures.init();
        SpiritHeraldEntities.init();
        SpiritEntities.ENTITY_TYPES.register(modEventBus);
        SpiritWorldgen.FEATURES.register(modEventBus);
        SpiritWorldgen.PLACEMENT_TYPES.register(modEventBus);
        SpiritItems.CREATIVE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }
}
