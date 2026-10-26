package com.spiritform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Усі дані гравця про форму Привида.
 *
 * unlocked  - куплено спільний рівень 1 (форма відкрита);
 * active    - гравець зараз у формі Привида;
 * invisible - увімкнена здібність Зникнення;
 * hunger, movement, darkness - рівні гілок (від 1 до 15, 1 = базовий);
 * abilities - бітова маска відкритих здібностей (SpiritAbility);
 * instinct  - увімкнений "Інстинкт Привида": форма вмикається сама, коли лишається одне сердечко;
 * icy       - увімкнений "Крижаний Доторк";
 * presence  - увімкнена "Присутність";
 * gaze      - увімкнений "Погляд Привида";
 * echo      - увімкнене "Відлуння Смерті" (де лежить Відлуння, знає SpiritEchoIndex);
 * shards    - скільки Уламків Душі гравець відкрив (стільки їх може існувати одночасно);
 * armySlots - скільки душ вміщує сховище Армії Потойбіччя (спершу одна);
 * blockGrab - Доторк Привида зараз тримає ціль, моба чи блок (клієнту потрібно знати це, щоб колесо миші рухало її).
 */
public record SpiritData(
        boolean unlocked,
        boolean active,
        boolean invisible,
        int hunger,
        int movement,
        int darkness,
        int abilities,
        boolean instinct,
        boolean icy,
        boolean presence,
        boolean gaze,
        boolean echo,
        int shards,
        boolean blockGrab,
        int armySlots
) {
    public static final SpiritData DEFAULT =
            new SpiritData(false, false, false, 1, 1, 1, 0, false, false, false, false, false, 0, false, 1);

    public static final Codec<SpiritData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(SpiritData::unlocked),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(SpiritData::active),
            Codec.BOOL.optionalFieldOf("invisible", false).forGetter(SpiritData::invisible),
            Codec.INT.optionalFieldOf("hunger", 1).forGetter(SpiritData::hunger),
            Codec.INT.optionalFieldOf("movement", 1).forGetter(SpiritData::movement),
            Codec.INT.optionalFieldOf("darkness", 1).forGetter(SpiritData::darkness),
            Codec.INT.optionalFieldOf("abilities", 0).forGetter(SpiritData::abilities),
            Codec.BOOL.optionalFieldOf("instinct", false).forGetter(SpiritData::instinct),
            Codec.BOOL.optionalFieldOf("icy", false).forGetter(SpiritData::icy),
            Codec.BOOL.optionalFieldOf("presence", false).forGetter(SpiritData::presence),
            Codec.BOOL.optionalFieldOf("gaze", false).forGetter(SpiritData::gaze),
            Codec.BOOL.optionalFieldOf("echo", false).forGetter(SpiritData::echo),
            Codec.INT.optionalFieldOf("shards", 0).forGetter(SpiritData::shards),
            Codec.BOOL.optionalFieldOf("blockGrab", false).forGetter(SpiritData::blockGrab),
            Codec.INT.optionalFieldOf("armySlots", 1).forGetter(SpiritData::armySlots)
    ).apply(instance, SpiritData::new));

    public static final StreamCodec<ByteBuf, SpiritData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeBoolean(data.unlocked);
                buf.writeBoolean(data.active);
                buf.writeBoolean(data.invisible);
                ByteBufCodecs.VAR_INT.encode(buf, data.hunger);
                ByteBufCodecs.VAR_INT.encode(buf, data.movement);
                ByteBufCodecs.VAR_INT.encode(buf, data.darkness);
                ByteBufCodecs.VAR_INT.encode(buf, data.abilities);
                buf.writeBoolean(data.instinct);
                buf.writeBoolean(data.icy);
                buf.writeBoolean(data.presence);
                buf.writeBoolean(data.gaze);
                buf.writeBoolean(data.echo);
                ByteBufCodecs.VAR_INT.encode(buf, data.shards);
                buf.writeBoolean(data.blockGrab);
                ByteBufCodecs.VAR_INT.encode(buf, data.armySlots);
            },
            buf -> new SpiritData(
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readBoolean(),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );

    public SpiritData {
        hunger = clampLevel(hunger);
        movement = clampLevel(movement);
        darkness = clampLevel(darkness);
        shards = Math.max(0, shards);
        armySlots = Math.max(1, armySlots);
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

    public boolean hasAbility(SpiritAbility ability) {
        return (abilities & ability.bit()) != 0;
    }

    public SpiritData withUnlocked(boolean value) {
        return new SpiritData(value, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots);
    }

    public SpiritData withActive(boolean value) {
        return new SpiritData(unlocked, value, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots);
    }

    public SpiritData withInvisible(boolean value) {
        return new SpiritData(unlocked, active, value, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots);
    }

    public SpiritData withInstinct(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, value, icy, presence, gaze, echo, shards, blockGrab, armySlots);
    }

    public SpiritData withIcy(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, value, presence, gaze, echo, shards, blockGrab, armySlots);
    }

    public SpiritData withPresence(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, value, gaze, echo, shards, blockGrab, armySlots);
    }

    public SpiritData withGaze(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, value, echo, shards, blockGrab, armySlots);
    }

    public SpiritData withEcho(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, value, shards, blockGrab, armySlots);
    }

    public SpiritData withShards(int value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, value, blockGrab, armySlots);
    }

    public SpiritData withBlockGrab(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, value, armySlots);
    }

    public SpiritData withArmySlots(int value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, value);
    }

    public SpiritData withAbility(SpiritAbility ability, boolean value) {
        int mask = value ? (abilities | ability.bit()) : (abilities & ~ability.bit());
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, mask, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots);
    }

    public SpiritData withLevel(SpiritBranch branch, int level) {
        return switch (branch) {
            case HUNGER -> new SpiritData(unlocked, active, invisible, level, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots);
            case MOVEMENT -> new SpiritData(unlocked, active, invisible, hunger, level, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots);
            case DARKNESS -> new SpiritData(unlocked, active, invisible, hunger, movement, level, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots);
        };
    }

    public SpiritData withAllBranchesReset() {
        return new SpiritData(unlocked, active, invisible, 1, 1, 1, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots);
    }
}
