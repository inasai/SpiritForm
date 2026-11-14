package com.spiritform.client;

import com.spiritform.SpiritFormMod;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Перезарядки здібностей, які клієнт показує на іконках (затемнення, що поступово зникає). Рахується в тіках світу. */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritCooldownClient {

    private record Span(long start, long end) {
    }

    private static final Map<String, Span> SPANS = new HashMap<>();

    private SpiritCooldownClient() {
    }

    public static void start(String ability, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ticks <= 0) {
            return;
        }
        long now = minecraft.level.getGameTime();
        SPANS.put(ability, new Span(now, now + ticks));
    }

    /** Скільки перезарядки лишилось: від 1 (щойно почалася) до 0 (готово). */
    public static float remaining(String ability) {
        Span span = SPANS.get(ability);
        Minecraft minecraft = Minecraft.getInstance();
        if (span == null || minecraft.level == null) {
            return 0.0F;
        }
        long now = minecraft.level.getGameTime();
        if (now >= span.end() || now < span.start()) {
            SPANS.remove(ability);
            return 0.0F;
        }
        return (span.end() - now) / (float) (span.end() - span.start());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SPANS.clear();
    }
}
