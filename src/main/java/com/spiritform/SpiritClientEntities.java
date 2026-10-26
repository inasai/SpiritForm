package com.spiritform;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Уламок Душі нічого не малює сам: чорний дим створює сервер частинками. */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public final class SpiritClientEntities {

    private SpiritClientEntities() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpiritEntities.SOUL_SHARD.get(), NoopRenderer::new);
        event.registerEntityRenderer(SpiritEntities.SOUL_WISP.get(), NoopRenderer::new);
    }
}
