package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Колесико миші під час Доторку Привида: відсунути (+1) або наблизити (-1) схопленого моба. */
public record TouchDistancePayload(int steps) implements CustomPacketPayload {

    public static final Type<TouchDistancePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "touch_distance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TouchDistancePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TouchDistancePayload::steps,
                    TouchDistancePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(TouchDistancePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritTouch.adjustDistance(player, payload.steps());
            }
        });
    }
}
