package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Дія зі списком зовнішніх станцій (див. SpiritStations.EXT_*): 0 - додати блок із слота інвентаря в комірку
 * (-1 - у першу вільну), 1 - прибрати станцію з комірки й повернути предмет.
 */
public record ExternalStationPayload(int action, int cell, int slot) implements CustomPacketPayload {

    public static final Type<ExternalStationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "external_station"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExternalStationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ExternalStationPayload::action,
                    ByteBufCodecs.VAR_INT, ExternalStationPayload::cell,
                    ByteBufCodecs.VAR_INT, ExternalStationPayload::slot,
                    ExternalStationPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(ExternalStationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritStations.handleExternal(player, payload.action(), payload.cell(), payload.slot());
            }
        });
    }
}
