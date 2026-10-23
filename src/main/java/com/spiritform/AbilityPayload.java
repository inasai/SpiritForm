package com.spiritform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Клієнт просить сервер застосувати активну здібність.
 * ability = номер здібності в SpiritAbility, pressed = клавішу натиснуто (true) або відпущено (false).
 * Відпускання має значення лише для Доторку Привида.
 */
public record AbilityPayload(int ability, boolean pressed) implements CustomPacketPayload {

    public static final Type<AbilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AbilityPayload::ability,
                    ByteBufCodecs.BOOL, AbilityPayload::pressed,
                    AbilityPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(AbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            SpiritAbility[] all = SpiritAbility.values();
            if (payload.ability() < 0 || payload.ability() >= all.length) {
                return;
            }

            SpiritAbility ability = all[payload.ability()];
            switch (ability) {
                case TOUCH -> SpiritTouch.set(player, payload.pressed());
                case VANISH -> {
                    if (payload.pressed()) {
                        SpiritInvisibility.activate(player);
                    }
                }
                case STEP -> {
                    if (payload.pressed()) {
                        SpiritTeleport.activate(player);
                    }
                }
                case POSSESSION -> {
                    if (payload.pressed()) {
                        SpiritPossession.toggle(player);
                    }
                }
                case DELIRIUM -> {
                    if (payload.pressed()) {
                        SpiritDelirium.activate(player);
                    }
                }
                case IMMUNITY -> {
                    if (payload.pressed()) {
                        SpiritImmunity.activate(player);
                    }
                }
                case LIFE_GIFT, SCALES -> SpiritCharge.set(player, ability, payload.pressed());
                default -> {
                    // Чуття працює на клієнті, пасивні вмикаються через вікно прокачки
                }
            }
        });
    }
}
