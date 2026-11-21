package com.spiritform.network;

import com.spiritform.SpiritFormMod;
import com.spiritform.ability.EchoTravel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Вибір виміру на колесі Відлуння Смерті: клієнт надсилає ідентифікатор виміру, сервер усе перевіряє. */
public record EchoTravelPayload(String dimension) implements CustomPacketPayload {

    public static final Type<EchoTravelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "echo_travel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoTravelPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, EchoTravelPayload::dimension,
                    EchoTravelPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(EchoTravelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                EchoTravel.travel(player, ResourceLocation.tryParse(payload.dimension()));
            }
        });
    }
}
