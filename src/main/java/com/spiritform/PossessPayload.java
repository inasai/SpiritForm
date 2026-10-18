package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клавіша вселення: вселитися в моба, на якого дивиться гравець, або вийти з нього. */
public record PossessPayload() implements CustomPacketPayload {

    public static final Type<PossessPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "possess"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PossessPayload> STREAM_CODEC =
            StreamCodec.unit(new PossessPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(PossessPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritPossession.toggle(player);
            }
        });
    }
}
