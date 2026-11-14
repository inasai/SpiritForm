package com.spiritform.data;

import com.spiritform.vault.Station;
import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Дані здібності Самодостатність. Незмінні: кожна зміна дає новий екземпляр.
 *
 * unlocked - маска відкритих станцій (біти за Station.ordinal);
 * pages    - скільки сторінок колеса відкрито (1..3, перша завжди);
 * ready    - маска станцій, де лежить готовий вміст (лише на клієнт, щоб показати крапку біля комірки);
 * external - збережений вміст блоків зовнішніх станцій (за ідентифікатором станції). На клієнт не надсилається.
 * furnace, smoker, blast, brewing - збережений стан станцій, що працюють у фоні (предмети, паливо, прогрес).
 *            На клієнт не надсилаються.
 */
public record StationData(int unlocked, int pages, int ready,
                          CompoundTag furnace, CompoundTag smoker, CompoundTag blast, CompoundTag brewing,
                          CompoundTag external, List<String> blocks) {

    public static final StationData EMPTY = new StationData(0, 1, 0,
            new CompoundTag(), new CompoundTag(), new CompoundTag(), new CompoundTag(), new CompoundTag(),
            List.of("", "", "", "", "", "", "", ""));

    /** Скільки зовнішніх станцій можна додати. */
    public static final int EXTERNAL_MAX = 8;

    public static final Codec<StationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("unlocked", 0).forGetter(StationData::unlocked),
            Codec.INT.optionalFieldOf("pages", 1).forGetter(StationData::pages),
            Codec.INT.optionalFieldOf("ready", 0).forGetter(StationData::ready),
            CompoundTag.CODEC.optionalFieldOf("furnace", new CompoundTag()).forGetter(StationData::furnace),
            CompoundTag.CODEC.optionalFieldOf("smoker", new CompoundTag()).forGetter(StationData::smoker),
            CompoundTag.CODEC.optionalFieldOf("blast", new CompoundTag()).forGetter(StationData::blast),
            CompoundTag.CODEC.optionalFieldOf("brewing", new CompoundTag()).forGetter(StationData::brewing),
            CompoundTag.CODEC.optionalFieldOf("external", new CompoundTag()).forGetter(StationData::external),
            Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(StationData::blocks)
    ).apply(instance, StationData::new));

    /** На клієнт їде лише те, що потрібно інтерфейсу. */
    public static final StreamCodec<RegistryFriendlyByteBuf, StationData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.unlocked);
                ByteBufCodecs.VAR_INT.encode(buf, data.pages);
                ByteBufCodecs.VAR_INT.encode(buf, data.ready);
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(EXTERNAL_MAX)).encode(buf, data.blocks);
            },
            buf -> new StationData(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    new CompoundTag(), new CompoundTag(), new CompoundTag(), new CompoundTag(), new CompoundTag(),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(EXTERNAL_MAX)).decode(buf))
    );

    public boolean has(Station station) {
        return (unlocked & station.bit()) != 0;
    }

    public boolean isReady(Station station) {
        return (ready & station.bit()) != 0;
    }

    public int unlockedCount() {
        return Integer.bitCount(unlocked);
    }

    /** Вартість відкриття наступної станції: N-та коштує N рівнів досвіду. */
    public int nextStationCost() {
        return unlockedCount() + 1;
    }

    public StationData withUnlocked(Station station) {
        return new StationData(unlocked | station.bit(), pages, ready, furnace, smoker, blast, brewing, external, blocks);
    }

    public StationData withPages(int value) {
        return new StationData(unlocked, value, ready, furnace, smoker, blast, brewing, external, blocks);
    }

    public StationData withReady(int value) {
        return new StationData(unlocked, pages, value, furnace, smoker, blast, brewing, external, blocks);
    }

    /** Блок зовнішньої станції в комірці (порожній рядок, якщо комірка вільна). */
    public String blockAt(int cell) {
        return cell >= 0 && cell < blocks.size() ? blocks.get(cell) : "";
    }

    public StationData withBlock(int cell, String blockId) {
        List<String> next = new ArrayList<>(blocks);
        while (next.size() < EXTERNAL_MAX) {
            next.add("");
        }
        next.set(cell, blockId);
        return new StationData(unlocked, pages, ready, furnace, smoker, blast, brewing, external, List.copyOf(next));
    }

    public StationData withUnlockedMask(int mask) {
        return new StationData(mask, pages, ready, furnace, smoker, blast, brewing, external, blocks);
    }

    public StationData withExternal(CompoundTag value) {
        return new StationData(unlocked, pages, ready, furnace, smoker, blast, brewing, value, blocks);
    }

    public StationData withState(CompoundTag furnaceTag, CompoundTag smokerTag, CompoundTag blastTag,
                                 CompoundTag brewingTag, int readyMask) {
        return new StationData(unlocked, pages, readyMask, furnaceTag, smokerTag, blastTag, brewingTag, external, blocks);
    }
}
