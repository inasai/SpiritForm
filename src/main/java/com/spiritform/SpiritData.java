package com.spiritform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Усі дані гравця про форму Привида.
 *
 * unlocked - куплено спільний рівень 1 (форма відкрита);
 * active   - гравець зараз у формі Привида;
 * hunger, movement, darkness - рівні гілок (від 1 до 15, 1 = базовий).
 */
public record SpiritData(boolean unlocked, boolean active, boolean invisible, int hunger, int movement, int darkness) {
    public static final SpiritData DEFAULT = new SpiritData(false, false, false, 1, 1, 1);

    public static final Codec<SpiritData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(SpiritData::unlocked),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(SpiritData::active),
            Codec.BOOL.optionalFieldOf("invisible", false).forGetter(SpiritData::invisible),
            Codec.INT.optionalFieldOf("hunger", 1).forGetter(SpiritData::hunger),
            Codec.INT.optionalFieldOf("movement", 1).forGetter(SpiritData::movement),
            Codec.INT.optionalFieldOf("darkness", 1).forGetter(SpiritData::darkness)
    ).apply(instance, SpiritData::new));

    public static final StreamCodec<ByteBuf, SpiritData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SpiritData::unlocked,
            ByteBufCodecs.BOOL, SpiritData::active,
            ByteBufCodecs.BOOL, SpiritData::invisible,
            ByteBufCodecs.VAR_INT, SpiritData::hunger,
            ByteBufCodecs.VAR_INT, SpiritData::movement,
            ByteBufCodecs.VAR_INT, SpiritData::darkness,
            SpiritData::new
    );

    public SpiritData {
        hunger = clampLevel(hunger);
        movement = clampLevel(movement);
        darkness = clampLevel(darkness);
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(SpiritBalance.MAX_LEVEL, level));
    }

    public int level(SpiritBranch branch) {
        return switch (branch) {
            case HUNGER -> hunger;
            case MOVEMENT -> movement;
            case DARKNESS -> darkness;
        };
    }

    public SpiritData withUnlocked(boolean value) {
        return new SpiritData(value, active, invisible, hunger, movement, darkness);
    }

    public SpiritData withActive(boolean value) {
        return new SpiritData(unlocked, value, invisible, hunger, movement, darkness);
    }

    public SpiritData withInvisible(boolean value) {
        return new SpiritData(unlocked, active, value, hunger, movement, darkness);
    }

    public SpiritData withLevel(SpiritBranch branch, int level) {
        return switch (branch) {
            case HUNGER -> new SpiritData(unlocked, active, invisible, level, movement, darkness);
            case MOVEMENT -> new SpiritData(unlocked, active, invisible, hunger, level, darkness);
            case DARKNESS -> new SpiritData(unlocked, active, invisible, hunger, movement, level);
        };
    }

    public SpiritData withAllBranchesReset() {
        return new SpiritData(unlocked, active, invisible, 1, 1, 1);
    }
}
