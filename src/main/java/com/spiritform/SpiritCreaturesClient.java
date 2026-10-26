package com.spiritform;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Клієнтська частина істот: яка модель і яке відображення в кожної. Для нової істоти додається рядок в обидва методи. */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public final class SpiritCreaturesClient {

    private SpiritCreaturesClient() {
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TemplateCreatureModel.LAYER, TemplateCreatureModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpiritCreatures.TEMPLATE_CREATURE.get(), TemplateCreatureRenderer::new);
    }
}
