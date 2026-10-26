package com.spiritform;

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
        SpiritCreatures.init();
        SpiritEntities.ENTITY_TYPES.register(modEventBus);
        SpiritItems.CREATIVE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }
}
