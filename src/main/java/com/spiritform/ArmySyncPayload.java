package com.spiritform;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер повідомляє клієнту, які душі лежать у сховищі армії, хто з них викликаний і який наказ діє. */
public record ArmySyncPayload(List<Entry> souls, int order) implements CustomPacketPayload {

    public record Entry(UUID id, String type, String name, int hp, boolean summoned) {
    }

    public static final Type<ArmySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "army_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmySyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.order);
                buf.writeVarInt(payload.souls.size());
                for (Entry entry : payload.souls) {
                    buf.writeUUID(entry.id());
                    buf.writeUtf(entry.type());
                    buf.writeUtf(entry.name());
                    buf.writeVarInt(entry.hp());
                    buf.writeBoolean(entry.summoned());
                }
            },
            buf -> {
                int order = buf.readVarInt();
                int size = buf.readVarInt();
                List<Entry> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    list.add(new Entry(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readBoolean()));
                }
                return new ArmySyncPayload(list, order);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(ArmySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SpiritArmyClient.update(payload));
    }
}
