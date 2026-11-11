package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Сервер повідомляє клієнту стан шкали переслідування: справжнє значення (може бути від'ємним і понад 100),
 * індекс наступного Вісника, індекс активного (-1, якщо немає) і чи живий Вісник Смерті.
 * Клієнт лише показує ці числа, а вирішує все сервер.
 */
public record HeraldSyncPayload(int meter, int next, int active, boolean deathActive) implements CustomPacketPayload {

    public static final Type<HeraldSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "herald_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeraldSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.meter);
                buf.writeVarInt(payload.next);
                buf.writeInt(payload.active);
                buf.writeBoolean(payload.deathActive);
            },
            buf -> new HeraldSyncPayload(buf.readInt(), buf.readVarInt(), buf.readInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(HeraldSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SpiritHeraldClient.update(payload));
    }
}
