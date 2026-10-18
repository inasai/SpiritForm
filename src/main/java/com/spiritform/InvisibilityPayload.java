
package com.spiritform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InvisibilityPayload() implements CustomPacketPayload {

    public static final Type<InvisibilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    SpiritFormMod.MODID, "invisibility"));

    public static final net.minecraft.network.codec.StreamCodec<
            net.minecraft.network.RegistryFriendlyByteBuf,
            InvisibilityPayload> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.unit(
                    new InvisibilityPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(
            InvisibilityPayload payload,
            IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritInvisibility.activate(player);
            }
        });
    }
}
