package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Дія з армією. action: 0 = викликати (arg: id душі, порожньо = усіх), 1 = наказ (arg: номер),
 * 2 = видалити душу (arg: id душі).
 */
public record ArmyActionPayload(int action, String arg) implements CustomPacketPayload {

    public static final Type<ArmyActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "army_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmyActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ArmyActionPayload::action,
                    ByteBufCodecs.STRING_UTF8, ArmyActionPayload::arg,
                    ArmyActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(ArmyActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritArmy.handle(player, payload.action(), payload.arg());
            }
        });
    }
}
