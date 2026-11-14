package com.spiritform.network;

import com.spiritform.SpiritFormMod;
import com.spiritform.vault.SpiritStations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Дія колеса станцій: відкрити станцію, купити станцію чи купити сторінку (index = номер станції). */
public record StationActionPayload(int action, int index) implements CustomPacketPayload {

    public static final int OPEN = 0;
    public static final int BUY_STATION = 1;
    public static final int BUY_PAGE = 2;

    public static final Type<StationActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "station_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StationActionPayload::action,
                    ByteBufCodecs.VAR_INT, StationActionPayload::index,
                    StationActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(StationActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritStations.handle(player, payload.action(), payload.index());
            }
        });
    }
}
