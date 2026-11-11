package com.spiritform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Вкрадена зовнішність моба (див. SpiritDisguise): тип моба, знімок його даних (колір, варіант, спорядження)
 * і чи надягнута вона зараз. Зберігається разом із гравцем, не губиться після смерті й синхронізується з клієнтами.
 */
public record DisguiseData(String type, CompoundTag nbt, boolean worn) {

    public static final DisguiseData EMPTY = new DisguiseData("", new CompoundTag(), false);

    public static final Codec<DisguiseData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(DisguiseData::type),
            CompoundTag.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(DisguiseData::nbt),
            Codec.BOOL.optionalFieldOf("worn", false).forGetter(DisguiseData::worn)
    ).apply(instance, DisguiseData::new));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DisguiseData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DisguiseData::type,
            ByteBufCodecs.COMPOUND_TAG, DisguiseData::nbt,
            ByteBufCodecs.BOOL, DisguiseData::worn,
            DisguiseData::new
    );

    public boolean stored() {
        return !type.isEmpty();
    }

    public boolean active() {
        return worn && stored();
    }

    /** Дитинча (зменшений вигляд): за знімком даних моба. */
    public boolean baby() {
        return nbt.getBoolean("IsBaby") || nbt.contains("Age") && nbt.getInt("Age") < 0;
    }

    public DisguiseData withWorn(boolean value) {
        return new DisguiseData(type, nbt, value);
    }
}
