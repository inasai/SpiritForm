
package com.spiritform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TeleportPayload() implements CustomPacketPayload {
    public static final Type<TeleportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    SpiritFormMod.MODID, "teleport"));

    public static final net.minecraft.network.codec.StreamCodec<
            net.minecraft.network.RegistryFriendlyByteBuf,
            TeleportPayload> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.unit(
                    new TeleportPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(
            TeleportPayload payload,
            IPayloadContext context) {

        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritTeleport.activate(player);
            }
        });
    }
}
