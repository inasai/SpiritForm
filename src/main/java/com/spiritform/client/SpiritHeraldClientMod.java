package com.spiritform.client;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.model.HeraldModel;
import com.spiritform.client.render.HeraldRenderer;
import com.spiritform.herald.HeraldBase;
import com.spiritform.registry.SpiritHeraldEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Клієнтська реєстрація Вісників: шари моделей і відображення. */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public final class SpiritHeraldClientMod {

    private SpiritHeraldClientMod() {
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (HeraldModel.Variant variant : HeraldModel.Variant.values()) {
            event.registerLayerDefinition(HeraldModel.layer(variant), () -> HeraldModel.create(variant));
        }
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.<HeraldBase>registerEntityRenderer(SpiritHeraldEntities.STORM.get(),
                context -> new HeraldRenderer(context, HeraldModel.Variant.STORM, 1.0F, 0.6F));
        event.<HeraldBase>registerEntityRenderer(SpiritHeraldEntities.HUNGER.get(),
                context -> new HeraldRenderer(context, HeraldModel.Variant.HUNGER, 1.0F, 0.5F));
        event.<HeraldBase>registerEntityRenderer(SpiritHeraldEntities.COLD.get(),
                context -> new HeraldRenderer(context, HeraldModel.Variant.COLD, 1.0F, 0.9F));
        event.<HeraldBase>registerEntityRenderer(SpiritHeraldEntities.DEATH.get(),
                context -> new HeraldRenderer(context, HeraldModel.Variant.DEATH, 1.4F, 1.0F));
    }
}
