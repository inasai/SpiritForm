package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Дія зі сховищем артефактів і спорядження (див. SpiritVault.ACT_*).
 * kind: 0 = артефакти, 1 = спорядження; cell: номер комірки (-1 для "у першу вільну"); slot: номер слота інвентаря.
 */
public record VaultActionPayload(int action, int kind, int cell, int slot) implements CustomPacketPayload {

    public static final Type<VaultActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "vault_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VaultActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, VaultActionPayload::action,
                    ByteBufCodecs.VAR_INT, VaultActionPayload::kind,
                    ByteBufCodecs.VAR_INT, VaultActionPayload::cell,
                    ByteBufCodecs.VAR_INT, VaultActionPayload::slot,
                    VaultActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(VaultActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritVault.handle(player, payload.action(), payload.kind(), payload.cell(), payload.slot());
            }
        });
    }
}
