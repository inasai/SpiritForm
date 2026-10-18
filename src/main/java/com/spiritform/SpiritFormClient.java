package com.spiritform;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = SpiritFormMod.MODID, dist = Dist.CLIENT)
public class SpiritFormClient {

    public SpiritFormClient(ModContainer container) {
        // Кнопка "Налаштування" у списку модів
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
