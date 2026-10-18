package com.spiritform;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(
        modid = SpiritFormMod.MODID,
        bus = EventBusSubscriber.Bus.MOD
)
public class SpiritNetworking {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                ToggleSpiritPayload.TYPE,
                ToggleSpiritPayload.STREAM_CODEC,
                ToggleSpiritPayload::handleData
        );

        registrar.playToServer(
                UpgradeSpiritPayload.TYPE,
                UpgradeSpiritPayload.STREAM_CODEC,
                UpgradeSpiritPayload::handleData
        );

        registrar.playToServer(
                InvisibilityPayload.TYPE,
                InvisibilityPayload.STREAM_CODEC,
                InvisibilityPayload::handleData
        );
        registrar.playToServer(
                TeleportPayload.TYPE,
                TeleportPayload.STREAM_CODEC,
                TeleportPayload::handleData
        );

        registrar.playToServer(
                PossessPayload.TYPE,
                PossessPayload.STREAM_CODEC,
                PossessPayload::handleData
        );

        registrar.playToServer(
                PossessInputPayload.TYPE,
                PossessInputPayload.STREAM_CODEC,
                PossessInputPayload::handleData
        );
    }
}
