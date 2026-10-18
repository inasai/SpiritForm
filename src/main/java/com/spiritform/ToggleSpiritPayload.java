package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Клієнт натиснув G: просить сервер перемкнути форму.
 */
public record ToggleSpiritPayload() implements CustomPacketPayload {

    public static final Type<ToggleSpiritPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    SpiritFormMod.MODID,
                    "toggle_spirit"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSpiritPayload>
            STREAM_CODEC = StreamCodec.unit(new ToggleSpiritPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(
            ToggleSpiritPayload payload,
            IPayloadContext context
    ) {
        if (context.player() instanceof ServerPlayer player) {
            SpiritForm.toggle(player);
        }
    }
}
