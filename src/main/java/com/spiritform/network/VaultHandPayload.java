package com.spiritform.network;

import com.spiritform.SpiritFormMod;
import com.spiritform.vault.SpiritVaultHands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "Клієнт затискає ПКМ порожньою рукою": надсилається кожні кілька тіків, поки кнопка затиснута.
 * kind: 0 = Коса в комірці основної руки (активація), 1 = Ловець Душ у комірці допоміжної руки (поглинання).
 * Сервер сам перевіряє умови й веде відлік, клієнт лише повідомляє, що кнопка затиснута.
 */
public record VaultHandPayload(int kind) implements CustomPacketPayload {

    public static final Type<VaultHandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "vault_hand"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VaultHandPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, VaultHandPayload::kind,
                    VaultHandPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(VaultHandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpiritVaultHands.hold(player, payload.kind());
            }
        });
    }
}
