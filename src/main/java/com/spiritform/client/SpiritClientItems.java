package com.spiritform.client;

import com.spiritform.SpiritFormMod;
import com.spiritform.item.SoulCatcherItem;
import com.spiritform.registry.SpiritItems;
import com.spiritform.vault.SpiritStations;
import com.spiritform.vault.Station;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Клієнтські властивості предметів: за ними модель Ловця Душ міняється залежно від того, чи є всередині моб. */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public final class SpiritClientItems {

    private SpiritClientItems() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> Station.setSource(() -> {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            return player == null ? java.util.List.of() : SpiritStations.get(player).blocks();
        }));
        event.enqueueWork(() -> ItemProperties.register(
                SpiritItems.SOUL_CATCHER.get(),
                ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "filled"),
                (stack, level, entity, seed) -> SoulCatcherItem.isFull(stack) ? 1.0F : 0.0F));
    }
}
