package com.spiritform.network;

import com.spiritform.SpiritFormMod;
import com.spiritform.ability.SpiritEcho;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Дії з колеса Відлунь Смерті: повернутися до Відлуння, лишити нове або перейменувати. Сервер усе перевіряє. */
public record EchoSpotPayload(int action, int index, String name) implements CustomPacketPayload {

    public static final int GO = 0;
    public static final int PLACE = 1;
    public static final int RENAME = 2;

    public static final Type<EchoSpotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "echo_spot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoSpotPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, EchoSpotPayload::action,
                    ByteBufCodecs.VAR_INT, EchoSpotPayload::index,
                    ByteBufCodecs.STRING_UTF8, EchoSpotPayload::name,
                    EchoSpotPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(EchoSpotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case GO -> SpiritEcho.goTo(player, payload.index());
                case PLACE -> SpiritEcho.place(player);
                case RENAME -> SpiritEcho.rename(player, payload.index(), payload.name());
                default -> { }
            }
        });
    }
}
