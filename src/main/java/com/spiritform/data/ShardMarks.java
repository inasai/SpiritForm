package com.spiritform.data;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Де лежать Уламки Душі гравця: їх чує на клієнті Чуття Привида й веде до найближчого слід диму. */
public record ShardMarks(List<Mark> marks) {

    public static final ShardMarks EMPTY = new ShardMarks(List.of());

    public record Mark(String dimension, double x, double y, double z) {
        public static final Codec<Mark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dim").forGetter(Mark::dimension),
                Codec.DOUBLE.fieldOf("x").forGetter(Mark::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Mark::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Mark::z)
        ).apply(instance, Mark::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Mark> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Mark::dimension,
                ByteBufCodecs.DOUBLE, Mark::x,
                ByteBufCodecs.DOUBLE, Mark::y,
                ByteBufCodecs.DOUBLE, Mark::z,
                Mark::new
        );
    }

    public static final Codec<ShardMarks> CODEC = Mark.CODEC.listOf().xmap(ShardMarks::new, ShardMarks::marks);

    public static final StreamCodec<RegistryFriendlyByteBuf, ShardMarks> STREAM_CODEC =
            Mark.STREAM_CODEC.apply(ByteBufCodecs.list()).map(ShardMarks::new, ShardMarks::marks);
}
