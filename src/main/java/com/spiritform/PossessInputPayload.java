package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Щотіку, поки гравець керує мобом: куди він хоче йти та куди дивиться. */
public record PossessInputPayload(float forward, float strafe, boolean jump, float yaw, float pitch)
        implements CustomPacketPayload {

    public static final Type<PossessInputPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "possess_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PossessInputPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, PossessInputPayload::forward,
                    ByteBufCodecs.FLOAT, PossessInputPayload::strafe,
                    ByteBufCodecs.BOOL, PossessInputPayload::jump,
                    ByteBufCodecs.FLOAT, PossessInputPayload::yaw,
                    ByteBufCodecs.FLOAT, PossessInputPayload::pitch,
                    PossessInputPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(PossessInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritPossession.setInput(player, payload);
            }
        });
    }
}
