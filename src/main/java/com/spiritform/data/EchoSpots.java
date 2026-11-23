package com.spiritform.data;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Відлуння Смерті, які гравець зараз лишив у світі (назва, вимір і позиція). Виводяться зі світового обліку
 * SpiritEchoIndex і їдуть на клієнт власнику, щоб колесо Відлунь показало назви й координати.
 */
public record EchoSpots(List<Spot> spots) {

    public static final EchoSpots EMPTY = new EchoSpots(List.of());

    /** Довжина назви Відлуння, яку можна ввести. */
    public static final int NAME_MAX = 24;

    public record Spot(String name, String dimension, BlockPos pos) {
        public static final Codec<Spot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("name", "").forGetter(Spot::name),
                Codec.STRING.fieldOf("dim").forGetter(Spot::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Spot::pos)
        ).apply(instance, Spot::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Spot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Spot::name,
                ByteBufCodecs.STRING_UTF8, Spot::dimension,
                BlockPos.STREAM_CODEC, Spot::pos,
                Spot::new
        );
    }

    public static final Codec<EchoSpots> CODEC = Spot.CODEC.listOf().xmap(EchoSpots::new, EchoSpots::spots);

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoSpots> STREAM_CODEC =
            Spot.STREAM_CODEC.apply(ByteBufCodecs.list(32)).map(EchoSpots::new, EchoSpots::spots);
}
