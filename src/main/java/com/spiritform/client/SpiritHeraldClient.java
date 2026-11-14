package com.spiritform.client;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.client.gui.SpiritUi;
import com.spiritform.herald.HeraldBase;
import com.spiritform.herald.HeraldType;
import com.spiritform.herald.SpiritHeraldWorld;
import com.spiritform.herald.SpiritHeralds;
import com.spiritform.network.HeraldSyncPayload;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Клієнтська частина Вісників: копія шкали (лише для показу), панель шкали, червоний дощ біля Вісника Голоду,
 * сніг і відтінок туману. Нічого тут не змінює стан гри: усе вирішує сервер.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritHeraldClient {

    private static int meter;
    private static int next;
    private static int active = -1;
    private static boolean deathActive;
    private static boolean received;

    /** До якого моменту (мс) показувати панель шкали: 5 секунд після змін або зміни виміру. */
    private static long showUntil;
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lastDimension;

    private static void flash() {
        showUntil = System.currentTimeMillis() + 5000L;
    }

    public static boolean known() {
        return received;
    }

    public static int meter() {
        return meter;
    }

    /** Яка стихія зараз навколо камери: 0 нічого, 1 буря, 2 голод, 3 холод. */
    private static int nearKind;
    private static double nearStrength;

    private SpiritHeraldClient() {
    }

    public static void update(HeraldSyncPayload payload) {
        if (received && (payload.meter() != meter || payload.active() != active || payload.deathActive() != deathActive)) {
            flash();
        }
        meter = payload.meter();
        next = payload.next();
        active = payload.active();
        deathActive = payload.deathActive();
        received = true;
    }

    /** Чи треба зараз фарбувати дощ у червоний (читає міксин відображення дощу). */
    public static boolean redRain() {
        return nearKind == 2;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        meter = 0;
        next = 0;
        active = -1;
        deathActive = false;
        received = false;
        showUntil = 0L;
        lastDimension = null;
        nearKind = 0;
        nearStrength = 0.0;
        SpiritHeraldWorld.clearZones();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            nearKind = 0;
            return;
        }
        if (lastDimension != null && !lastDimension.equals(minecraft.level.dimension())) {
            flash();
        }
        lastDimension = minecraft.level.dimension();

        double radius = Config.HERALD_RADIUS.get();
        List<HeraldBase> heralds = minecraft.level.getEntitiesOfClass(HeraldBase.class,
                player.getBoundingBox().inflate(radius), h -> h.isAlive());
        int kind = 0;
        double strength = 0.0;
        for (HeraldBase herald : heralds) {
            double dist = Math.sqrt(player.distanceToSqr(herald));
            double s = Math.max(0.0, 1.0 - dist / radius);
            int k = switch (herald.kind()) {
                case STORM -> 1;
                case HUNGER -> 2;
                case COLD -> 3;
                default -> 0;
            };
            if (herald.kind() == HeraldType.COLD) {
                SpiritHeraldWorld.markWinter(herald.getUUID(), herald.getX(), herald.getZ(), radius);
            }
            if (k != 0 && s >= strength) {
                kind = k;
                strength = s;
            }
        }
        nearKind = kind;
        nearStrength = strength;

        // Сніжинки навколо гравця, поки в цьому місці йде штучна зима
        if (SpiritHeraldWorld.isWinterAt(player.blockPosition()) && player.tickCount % 2 == 0) {
            for (int i = 0; i < 4; i++) {
                minecraft.level.addParticle(ParticleTypes.SNOWFLAKE,
                        player.getX() + (minecraft.level.random.nextDouble() - 0.5) * 24.0,
                        player.getY() + minecraft.level.random.nextDouble() * 8.0,
                        player.getZ() + (minecraft.level.random.nextDouble() - 0.5) * 24.0, 0.0, -0.1, 0.0);
            }
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (nearKind == 0) {
            return;
        }
        float f = (float) Math.min(0.6, 0.15 + nearStrength * 0.6);
        float r;
        float g;
        float b;
        switch (nearKind) {
            case 2 -> {
                r = 0.45F;
                g = 0.03F;
                b = 0.03F;
            }
            case 3 -> {
                r = 0.72F;
                g = 0.85F;
                b = 0.95F;
            }
            default -> {
                r = 0.35F;
                g = 0.38F;
                b = 0.42F;
            }
        }
        event.setRed(event.getRed() + (r - event.getRed()) * f);
        event.setGreen(event.getGreen() + (g - event.getGreen()) * f);
        event.setBlue(event.getBlue() + (b - event.getBlue()) * f);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null || !received
                || System.currentTimeMillis() > showUntil) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        var font = minecraft.font;
        int x = 6;
        int y = 6;
        int barW = 182;

        // На панелі видно від 0 до 100%, а справжнє значення (від'ємне чи понад 100) лише в дужках
        int shown = Math.max(0, Math.min(100, meter));
        boolean ready = meter >= SpiritHeralds.THRESHOLD;
        int color = ready ? 0xFFFF7060 : 0xFFFFFFFF;

        // Смуга, як у боса: назва зверху, смуга, нижче рядок про наступного Вісника
        graphics.drawString(font, Component.translatable("hud.spirit_form.herald.title"), x, y, color, true);
        String percent = shown + "%";
        graphics.drawString(font, percent, x + barW - font.width(percent), y, color, true);
        SpiritUi.bossBar(graphics, x, y + 11, shown / 100.0F, ready, 1.0F);

        Component line;
        if (active >= 0) {
            line = Component.translatable("hud.spirit_form.herald.active", HeraldType.byIndex(active).displayName());
        } else if (deathActive) {
            line = Component.translatable("hud.spirit_form.herald.death");
        } else {
            line = Component.translatable("hud.spirit_form.herald.next", HeraldType.byIndex(next).displayName());
        }
        graphics.drawString(font, line, x, y + 20, 0xFFE0E0E0, true);

        if (meter < 0 || meter > 100) {
            String real = String.format(Locale.ROOT, "(%d)", meter);
            graphics.drawString(font, real, x + barW - font.width(real), y + 20, 0xFFB0B0B0, true);
        }
    }
}
