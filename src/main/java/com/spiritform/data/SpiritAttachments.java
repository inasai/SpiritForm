package com.spiritform.data;

import com.mojang.serialization.Codec;
import com.spiritform.SpiritFormMod;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class SpiritAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SpiritFormMod.MODID);

    /**
     * Дані форми Привида: зберігаються разом із гравцем, самі їдуть на клієнт
     * (власнику та іншим гравцям поруч) і копіюються при смерті.
     */
    public static final Supplier<AttachmentType<SpiritData>> SPIRIT = ATTACHMENT_TYPES.register(
            "spirit",
            () -> AttachmentType.builder(() -> SpiritData.DEFAULT)
                    .serialize(SpiritData.CODEC)
                    .sync(SpiritData.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    /**
     * Сховище артефактів і спорядження: зберігається разом із гравцем, їде на клієнт (власнику та іншим гравцям
     * поруч, щоб було видно артефакти на персонажі) і копіюється при смерті, тож предмети не губляться.
     */
    public static final Supplier<AttachmentType<VaultData>> VAULT = ATTACHMENT_TYPES.register(
            "vault",
            () -> AttachmentType.builder(() -> VaultData.EMPTY)
                    .serialize(VaultData.CODEC)
                    .sync(VaultData.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    /** Вкрадена зовнішність моба (Одержимість -> Марення): що за моб і чи надягнута вона зараз. */
    public static final Supplier<AttachmentType<DisguiseData>> DISGUISE = ATTACHMENT_TYPES.register(
            "disguise",
            () -> AttachmentType.builder(() -> DisguiseData.EMPTY)
                    .serialize(DisguiseData.CODEC)
                    .sync(DisguiseData.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    /**
     * Станції Самодостатності: відкриті станції й сторінки, готовий вміст (на клієнт) та стан печей і варильної стійки
     * (лише на сервері). Копіюється при смерті, тож станції й їхні предмети не губляться.
     */
    public static final Supplier<AttachmentType<StationData>> STATIONS = ATTACHMENT_TYPES.register(
            "stations",
            () -> AttachmentType.builder(() -> StationData.EMPTY)
                    .serialize(StationData.CODEC)
                    .sync(StationData.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    /** Місця Уламків Душі гравця (виводяться зі світового обліку, на клієнт їдуть для сліду диму Чуття). */
    public static final Supplier<AttachmentType<ShardMarks>> SHARD_MARKS = ATTACHMENT_TYPES.register(
            "shard_marks",
            () -> AttachmentType.builder(() -> ShardMarks.EMPTY)
                    .serialize(ShardMarks.CODEC)
                    .sync(ShardMarks.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    /**
     * Виміри, де гравець уже бував (ідентифікатори): з них складається колесо Відлуння Смерті.
     * Їде на клієнт власнику; копіюється при смерті.
     */
    public static final Supplier<AttachmentType<List<String>>> VISITED_DIMENSIONS = ATTACHMENT_TYPES.register(
            "visited_dimensions",
            () -> AttachmentType.<List<String>>builder(() -> List.of())
                    .serialize(Codec.STRING.listOf())
                    .sync(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)))
                    .copyOnDeath()
                    .build()
    );

    /** Остання безпечна позиція гравця в кожному вимірі (лише на сервері): сюди Відлуння повертає гравця. */
    public static final Supplier<AttachmentType<Map<String, BlockPos>>> DIMENSION_SPOTS = ATTACHMENT_TYPES.register(
            "dimension_spots",
            () -> AttachmentType.<Map<String, BlockPos>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, BlockPos.CODEC))
                    .copyOnDeath()
                    .build()
    );

    private SpiritAttachments() {
    }
}
