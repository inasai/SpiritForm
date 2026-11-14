package com.spiritform.data;

import com.spiritform.ability.SpiritAbility;
import com.spiritform.form.SpiritBalance;
import com.spiritform.form.SpiritBranch;
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
 * lord      - поглинуто Серце Смерті (Володар Потойбіччя): перезарядка й голод удвічі менші, Уламків і місць в Армії удвічі більше;
 * armySlots - скільки душ вміщує сховище Армії Потойбіччя (спершу одна);
 * form      - рівень здібності «Форма Привида» (біти 0-2, від 0 до 4; 1 = лише відкрита форма), прапорець режиму Overworld (біт 3: усе одразу на максимумі) і прапорець увімкненого проходження крізь блоки (біт 4) і прапорець вимкненої гравцем невразливості (біт 5) і прапорець вимкненого випадіння душ з убитих ворогів (біт 6, Армія Душ);
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
        int armySlots,
        boolean lord,
        boolean energy,
        int form
) {
    public static final SpiritData DEFAULT =
            new SpiritData(false, false, false, 1, 1, 1, 0, false, false, false, false, false, 0, false, 1, false, false, 0);

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
            Codec.INT.optionalFieldOf("armySlots", 1).forGetter(SpiritData::armySlots),
            // codec RecordCodecBuilder тримає не більше 16 полів, тож lord і energy зберігаються одним числом
            Codec.INT.optionalFieldOf("flags", 0).forGetter(d -> (d.lord() ? 1 : 0) | (d.energy() ? 2 : 0) | (d.form() << 2))
    ).apply(instance, (u, a, i, h, m, d, ab, in, ic, p, g, e, sh, bg, sl, fl) -> new SpiritData(u, a, i, h, m, d, ab, in, ic, p, g, e, sh, bg, sl, (fl & 1) != 0, (fl & 2) != 0, fl >> 2)));

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
                buf.writeBoolean(data.lord);
                buf.writeBoolean(data.energy);
                ByteBufCodecs.VAR_INT.encode(buf, data.form);
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
                    ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readBoolean(),
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
        form = Math.max(0, Math.min(127, form));
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
        return new SpiritData(value, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withActive(boolean value) {
        return new SpiritData(unlocked, value, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withInvisible(boolean value) {
        return new SpiritData(unlocked, active, value, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withInstinct(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, value, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withIcy(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, value, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withPresence(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, value, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withGaze(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, value, echo, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withEcho(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, value, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withShards(int value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, value, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withBlockGrab(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, value, armySlots, lord, energy, form);
    }

    public SpiritData withArmySlots(int value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, value, lord, energy, form);
    }

    public SpiritData withLord(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, value, energy, form);
    }

    public SpiritData withEnergy(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, value, form);
    }

    public static final int FORM_MAX = 4;

    /** Куплений рівень здібності «Форма Привида» (0 до відкриття форми, далі від 1 до 4). */
    public int formLevel() {
        return Math.min(FORM_MAX, form & 7);
    }

    /** Режим Overworld: здібність «Форма Привида» одразу на максимумі, її не потрібно розвивати. */
    public boolean fullForm() {
        return (form & 8) != 0;
    }

    /** Рівень форми, що діє зараз: 0 без відкритої форми, 4 у режимі Overworld, інакше куплений (щонайменше 1). */
    public int effectiveFormLevel() {
        if (!unlocked) {
            return 0;
        }
        return fullForm() ? FORM_MAX : Math.max(1, formLevel());
    }

    /** Проходження крізь блоки зараз увімкнене гравцем (двічі швидко Shift) і дозволене рівнем форми. */
    public boolean isPhasing() {
        return canPhase() && (form & 16) != 0;
    }

    public boolean phasing() {
        return (form & 16) != 0;
    }

    public SpiritData withPhasing(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy,
                value ? (form | 16) : (form & ~16));
    }

    public boolean canFly() {
        return effectiveFormLevel() >= 3;
    }

    public boolean canPhase() {
        return effectiveFormLevel() >= 2;
    }

    /** Невразливість діє: рівень форми 4 і гравець не вимкнув її (біт 5 = вимкнено). */
    public boolean invulnerable() {
        return effectiveFormLevel() >= 4 && (form & 32) == 0;
    }

    /** Невразливість розвинена до максимуму (її можна вмикати й вимикати), незалежно від перемикача. */
    public boolean canBeInvulnerable() {
        return effectiveFormLevel() >= 4;
    }

    public SpiritData withInvulnerabilityOff(boolean off) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy,
                off ? (form | 32) : (form & ~32));
    }

    /** Душі з убитих ворогів випадають (біт 6 = вимкнено гравцем; за замовчуванням увімкнено). */
    public boolean soulDrops() {
        return (form & 64) == 0;
    }

    public SpiritData withSoulDrops(boolean enabled) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy,
                enabled ? (form & ~64) : (form | 64));
    }

    public SpiritData withFormLevel(int level) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy,
                (form & ~7) | Math.max(0, Math.min(FORM_MAX, level)));
    }

    public SpiritData withFullForm(boolean value) {
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy,
                value ? (form | 8) : (form & ~8));
    }

    /** Скільки Уламків Душі може існувати одночасно: з Серцем Смерті удвічі більше. */
    public int effectiveShards() {
        return lord ? shards * 2 : shards;
    }

    /** Скільки душ вміщує Армія: з Серцем Смерті удвічі більше. */
    public int effectiveArmySlots() {
        return lord ? armySlots * 2 : armySlots;
    }

    public SpiritData withAbility(SpiritAbility ability, boolean value) {
        int mask = value ? (abilities | ability.bit()) : (abilities & ~ability.bit());
        return new SpiritData(unlocked, active, invisible, hunger, movement, darkness, mask, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
    }

    public SpiritData withLevel(SpiritBranch branch, int level) {
        return switch (branch) {
            case HUNGER -> new SpiritData(unlocked, active, invisible, level, movement, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
            case MOVEMENT -> new SpiritData(unlocked, active, invisible, hunger, level, darkness, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
            case DARKNESS -> new SpiritData(unlocked, active, invisible, hunger, movement, level, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
        };
    }

    public SpiritData withAllBranchesReset() {
        return new SpiritData(unlocked, active, invisible, 1, 1, 1, abilities, instinct, icy, presence, gaze, echo, shards, blockGrab, armySlots, lord, energy, form);
    }
}
