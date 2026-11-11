package com.spiritform;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Сховище артефактів і спорядження гравця. Зберігається разом із гравцем і не губиться після смерті.
 *
 * artifacts      - предмети в комірках артефактів (комірки загальні, будь-який артефакт у будь-якій);
 * artifactCells  - скільки комірок артефактів відкрито (відкриваються по порядку);
 * gear           - предмети в комірках спорядження (шолом, нагрудник, штани, чоботи, основна й допоміжна рука);
 * gearUnlocked   - бітова маска відкритих комірок спорядження;
 * hidden         - бітова маска прихованих на персонажі комірок: біт i для артефактів, біт 16 + i для спорядження;
 * disabled       - бітова маска комірок, де дію предмета вимкнено гравцем (такий самий розподіл бітів).
 *
 * Об'єкт незмінний: кожна зміна дає новий екземпляр. Списки можуть бути коротшими за потрібне (тоді решта порожня).
 */
public final class VaultData {

    public static final int GEAR_SIZE = 6;

    public static final VaultData EMPTY = new VaultData(List.of(), 0, List.of(), 0, 0, 0);

    private static final Codec<List<ItemStack>> STACKS = ItemStack.OPTIONAL_CODEC.listOf();

    public static final Codec<VaultData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STACKS.optionalFieldOf("artifacts", List.of()).forGetter(VaultData::artifacts),
            Codec.INT.optionalFieldOf("artifactCells", 0).forGetter(VaultData::artifactCells),
            STACKS.optionalFieldOf("gear", List.of()).forGetter(VaultData::gear),
            Codec.INT.optionalFieldOf("gearUnlocked", 0).forGetter(VaultData::gearUnlockedMask),
            Codec.INT.optionalFieldOf("hidden", 0).forGetter(VaultData::hiddenMask),
            Codec.INT.optionalFieldOf("disabled", 0).forGetter(VaultData::disabledMask)
    ).apply(instance, VaultData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VaultData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                writeStacks(buf, data.artifacts);
                ByteBufCodecs.VAR_INT.encode(buf, data.artifactCells);
                writeStacks(buf, data.gear);
                ByteBufCodecs.VAR_INT.encode(buf, data.gearUnlocked);
                ByteBufCodecs.VAR_INT.encode(buf, data.hidden);
                ByteBufCodecs.VAR_INT.encode(buf, data.disabled);
            },
            buf -> new VaultData(
                    readStacks(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    readStacks(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );

    private static void writeStacks(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        ByteBufCodecs.VAR_INT.encode(buf, stacks.size());
        for (ItemStack stack : stacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static List<ItemStack> readStacks(RegistryFriendlyByteBuf buf) {
        int size = ByteBufCodecs.VAR_INT.decode(buf);
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return list;
    }

    private final List<ItemStack> artifacts;
    private final int artifactCells;
    private final List<ItemStack> gear;
    private final int gearUnlocked;
    private final int hidden;
    private final int disabled;

    public VaultData(List<ItemStack> artifacts, int artifactCells, List<ItemStack> gear, int gearUnlocked, int hidden, int disabled) {
        this.artifacts = copy(artifacts);
        this.artifactCells = Math.max(0, artifactCells);
        this.gear = copy(gear);
        this.gearUnlocked = gearUnlocked & ((1 << GEAR_SIZE) - 1);
        this.hidden = hidden;
        this.disabled = disabled;
    }

    private static List<ItemStack> copy(List<ItemStack> source) {
        List<ItemStack> list = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            list.add(stack.copy());
        }
        return list;
    }

    private static List<ItemStack> with(List<ItemStack> source, int index, ItemStack stack) {
        List<ItemStack> list = copy(source);
        while (list.size() <= index) {
            list.add(ItemStack.EMPTY);
        }
        list.set(index, stack.copy());
        return list;
    }

    // ---------- читання ----------

    public List<ItemStack> artifacts() {
        return artifacts;
    }

    public List<ItemStack> gear() {
        return gear;
    }

    public int artifactCells() {
        return artifactCells;
    }

    public int gearUnlockedMask() {
        return gearUnlocked;
    }

    public int hiddenMask() {
        return hidden;
    }

    public int disabledMask() {
        return disabled;
    }

    public int gearUnlockedCount() {
        return Integer.bitCount(gearUnlocked);
    }

    public ItemStack artifact(int index) {
        return index >= 0 && index < artifacts.size() ? artifacts.get(index) : ItemStack.EMPTY;
    }

    public ItemStack gearStack(int index) {
        return index >= 0 && index < gear.size() ? gear.get(index) : ItemStack.EMPTY;
    }

    public boolean artifactUnlocked(int index) {
        return index >= 0 && index < artifactCells;
    }

    public boolean gearUnlocked(int index) {
        return index >= 0 && index < GEAR_SIZE && (gearUnlocked & (1 << index)) != 0;
    }

    public boolean artifactHidden(int index) {
        return index >= 0 && index < 16 && (hidden & (1 << index)) != 0;
    }

    public boolean gearHidden(int index) {
        return index >= 0 && index < GEAR_SIZE && (hidden & (1 << (16 + index))) != 0;
    }

    /** Дію предмета в комірці артефактів вимкнено гравцем. */
    public boolean artifactDisabled(int index) {
        return index >= 0 && index < 16 && (disabled & (1 << index)) != 0;
    }

    public boolean gearDisabled(int index) {
        return index >= 0 && index < GEAR_SIZE && (disabled & (1 << (16 + index))) != 0;
    }

    // ---------- зміни (повертають новий екземпляр) ----------

    public VaultData withArtifact(int index, ItemStack stack) {
        return new VaultData(with(artifacts, index, stack), artifactCells, gear, gearUnlocked, hidden, disabled);
    }

    public VaultData withGear(int index, ItemStack stack) {
        return new VaultData(artifacts, artifactCells, with(gear, index, stack), gearUnlocked, hidden, disabled);
    }

    public VaultData withArtifactCells(int cells) {
        return new VaultData(artifacts, cells, gear, gearUnlocked, hidden, disabled);
    }

    public VaultData withGearUnlocked(int index) {
        return new VaultData(artifacts, artifactCells, gear, gearUnlocked | (1 << index), hidden, disabled);
    }

    public VaultData withArtifactHidden(int index, boolean value) {
        return new VaultData(artifacts, artifactCells, gear, gearUnlocked, bit(hidden, index, value), disabled);
    }

    public VaultData withGearHidden(int index, boolean value) {
        return new VaultData(artifacts, artifactCells, gear, gearUnlocked, bit(hidden, 16 + index, value), disabled);
    }

    public VaultData withArtifactDisabled(int index, boolean value) {
        return new VaultData(artifacts, artifactCells, gear, gearUnlocked, hidden, bit(disabled, index, value));
    }

    public VaultData withGearDisabled(int index, boolean value) {
        return new VaultData(artifacts, artifactCells, gear, gearUnlocked, hidden, bit(disabled, 16 + index, value));
    }

    private static int bit(int mask, int index, boolean value) {
        return value ? mask | (1 << index) : mask & ~(1 << index);
    }
}
