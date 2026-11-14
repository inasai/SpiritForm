package com.spiritform.client;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.gui.SpiritIcons;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Скидає кеш розмірів іконок, коли гравець перезавантажує ресурси (F3+T). */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public final class SpiritClientReload {

    private SpiritClientReload() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                manager -> {
                    SpiritIcons.clearCache();
                    SpiritSenseVision.close();
                });
    }
}
