package com.spiritform.network;

import com.spiritform.SpiritFormMod;
import com.spiritform.item.SoulShardTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** ПКМ по Інстинктах Привида в колесі: Уламок Душі телепортується до гравця (немає - повертається з копії, як /spirit_shard). */
public record ShardRecallPayload() implements CustomPacketPayload {

    public static final Type<ShardRecallPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "shard_recall"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShardRecallPayload> STREAM_CODEC =
            StreamCodec.unit(new ShardRecallPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(ShardRecallPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SoulShardTools.summon(player);
            }
        });
    }
}
