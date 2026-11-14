package com.spiritform.client;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.model.AccessoryModels;
import com.spiritform.client.model.NecklaceModel;
import com.spiritform.client.render.BraceletLayer;
import com.spiritform.client.render.NecklaceLayer;
import com.spiritform.client.render.ShacklesLayer;
import com.spiritform.client.render.SoulMirrorLayer;
import com.spiritform.client.render.SpiritArmorLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Підключає шар Намиста Потойбіччя до обох типів гравців (широкі й тонкі руки). */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public final class SpiritClientRender {

    private SpiritClientRender() {
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NecklaceModel.LAYER_LOCATION, NecklaceModel::createBodyLayer);
        event.registerLayerDefinition(AccessoryModels.MIRROR, AccessoryModels::mirrorLayer);
        event.registerLayerDefinition(AccessoryModels.SHACKLES, AccessoryModels::shacklesLayer);
        event.registerLayerDefinition(AccessoryModels.BRACELET, AccessoryModels::braceletLayer);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new NecklaceLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new SoulMirrorLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new ShacklesLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new BraceletLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new SpiritArmorLayer(renderer));
            }
        }
    }
}
