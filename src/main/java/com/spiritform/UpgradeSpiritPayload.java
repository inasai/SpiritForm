package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Клієнт просить сервер про дію у вікні прокачки.
 * 0 = відкрити форму (рівень 1), 1..3 = підвищити гілку, 10..12 = скинути гілку.
 */
public record UpgradeSpiritPayload(int action) implements CustomPacketPayload {

    public static final int UNLOCK = 0;
    public static final int RESET_BASE = 10;

    public static final Type<UpgradeSpiritPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    SpiritFormMod.MODID,
                    "upgrade_spirit"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeSpiritPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, UpgradeSpiritPayload::action,
                    UpgradeSpiritPayload::new
            );

    public static int actionFor(SpiritBranch branch) {
        return branch.ordinal() + 1;
    }

    public static SpiritBranch branchFor(int action) {
        return branchAt(action - 1);
    }

    public static int resetActionFor(SpiritBranch branch) {
        return RESET_BASE + branch.ordinal();
    }

    public static SpiritBranch resetBranchFor(int action) {
        return branchAt(action - RESET_BASE);
    }

    private static SpiritBranch branchAt(int index) {
        SpiritBranch[] branches = SpiritBranch.values();
        return index >= 0 && index < branches.length ? branches[index] : null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(
            UpgradeSpiritPayload payload,
            IPayloadContext context
    ) {
        if (context.player() instanceof ServerPlayer player) {
            SpiritUpgrades.handle(player, payload.action());
        }
    }
}
