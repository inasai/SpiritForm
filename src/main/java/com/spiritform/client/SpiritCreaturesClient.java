package com.spiritform.client;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.model.BahatkoModel;
import com.spiritform.client.model.BisykModel;
import com.spiritform.client.model.ChortModel;
import com.spiritform.client.model.ChuhaisterModel;
import com.spiritform.client.model.LisovykModel;
import com.spiritform.client.model.MavkaModel;
import com.spiritform.client.model.PerelisnykModel;
import com.spiritform.client.model.PhoenixModel;
import com.spiritform.client.model.PolovykModel;
import com.spiritform.client.model.PotercharModel;
import com.spiritform.client.model.PovitruliaModel;
import com.spiritform.client.model.SkarbnykModel;
import com.spiritform.client.model.TemplateCreatureModel;
import com.spiritform.client.model.VodianykModel;
import com.spiritform.client.model.VodianytsiaModel;
import com.spiritform.client.model.VovkulakaModel;
import com.spiritform.client.render.BahatkoRenderer;
import com.spiritform.client.render.ForestRenderers;
import com.spiritform.client.render.KlunnykRenderer;
import com.spiritform.client.render.PhoenixRenderer;
import com.spiritform.client.render.TemplateCreatureRenderer;
import com.spiritform.registry.SpiritCreatures;
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
        event.registerLayerDefinition(MavkaModel.LAYER, MavkaModel::createBodyLayer);
        event.registerLayerDefinition(ChuhaisterModel.LAYER, ChuhaisterModel::createBodyLayer);
        event.registerLayerDefinition(BisykModel.LAYER, BisykModel::createBodyLayer);
        event.registerLayerDefinition(SkarbnykModel.LAYER, SkarbnykModel::createBodyLayer);
        event.registerLayerDefinition(VodianykModel.LAYER, VodianykModel::createBodyLayer);
        event.registerLayerDefinition(PovitruliaModel.LAYER, PovitruliaModel::createBodyLayer);
        event.registerLayerDefinition(PerelisnykModel.LAYER, PerelisnykModel::createBodyLayer);
        event.registerLayerDefinition(PotercharModel.LAYER, PotercharModel::createBodyLayer);
        event.registerLayerDefinition(VodianytsiaModel.LAYER, VodianytsiaModel::createBodyLayer);
        event.registerLayerDefinition(PolovykModel.LAYER, PolovykModel::createBodyLayer);
        event.registerLayerDefinition(VovkulakaModel.LAYER, VovkulakaModel::createBodyLayer);
        event.registerLayerDefinition(ChortModel.LAYER, ChortModel::createBodyLayer);
        event.registerLayerDefinition(LisovykModel.LAYER, LisovykModel::createBodyLayer);
        event.registerLayerDefinition(BahatkoModel.LAYER, BahatkoModel::createBodyLayer);
        event.registerLayerDefinition(PhoenixModel.LAYER, PhoenixModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpiritCreatures.TEMPLATE_CREATURE.get(), TemplateCreatureRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.MAVKA.get(), ForestRenderers.MavkaRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.CHUHAISTER.get(), ForestRenderers.ChuhaisterRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.HUKHA.get(), ForestRenderers.HukhaRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.BISYK.get(), ForestRenderers.BisykRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.SKARBNYK.get(), ForestRenderers.SkarbnykRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.ZLYDENY.get(), ForestRenderers.ZlydenyRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.VODIANYK.get(), ForestRenderers.VodianykRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.POVITRULIA.get(), ForestRenderers.PovitruliaRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.PERELISNYK.get(), ForestRenderers.PerelisnykRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.NICHNYTSIA.get(), ForestRenderers.NichnytsiaRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.POTERCHA.get(), ForestRenderers.PotercharRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.VODIANYTSIA.get(), ForestRenderers.VodianytsiaRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.POLOVYK.get(), ForestRenderers.PolovykRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.VOVKULAKA.get(), ForestRenderers.VovkulakaRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.CHORT.get(), ForestRenderers.ChortRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.LISOVYK.get(), ForestRenderers.LisovykRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.KLUNNYK.get(), KlunnykRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.BAHATKO.get(), BahatkoRenderer::new);
        event.registerEntityRenderer(SpiritCreatures.PHOENIX.get(), PhoenixRenderer::new);
    }
}
