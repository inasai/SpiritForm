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

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }
}
