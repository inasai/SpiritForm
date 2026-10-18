package com.spiritform;

import java.util.function.Supplier;

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

    private SpiritAttachments() {
    }
}
