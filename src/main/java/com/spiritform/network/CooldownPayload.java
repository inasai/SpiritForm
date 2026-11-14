package com.spiritform.network;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.SpiritCooldownClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер повідомляє клієнту, що здібність пішла на перезарядку (id іконки й тривалість у тіках). */
public record CooldownPayload(String ability, int ticks) implements CustomPacketPayload {

    public static final Type<CooldownPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "ability_cooldown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CooldownPayload::ability,
            ByteBufCodecs.VAR_INT, CooldownPayload::ticks,
            CooldownPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(CooldownPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SpiritCooldownClient.start(payload.ability(), payload.ticks()));
    }
}
