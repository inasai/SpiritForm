package com.spiritform.client;

import com.spiritform.SpiritFormMod;
import com.spiritform.ability.SpiritAbility;
import com.spiritform.form.SpiritFormState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Вигляд екрана під час Чуття Привида: світ приглушується, а силуети предметів, блоків і частинок отримують
 * приглушений м'ятний контур. Це постобробка (шейдери shaders/post/sense.json): контур шукається за буфером глибини.
 * Сила ефекту плавно наростає й спадає, а коли вона дійшла до нуля, ланцюжок шейдерів закривається.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritSenseVision {

    private static final ResourceLocation CHAIN =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "shaders/post/sense.json");
    private static final float STEP = 0.12F;

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SpiritSenseVision.class);

    private static PostChain chain;
    private static boolean failed;
    private static int lastWidth;
    private static int lastHeight;
    private static float strength;
    private static float prevStrength;

    private SpiritSenseVision() {
    }

    /** Закриває ланцюжок (вихід зі світу, перезавантаження ресурсів). */
    public static void close() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        failed = false;
        lastWidth = 0;
        lastHeight = 0;
    }

    private static boolean wanted(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && mc.level != null && SpiritClientHighlight.isEnabled()
                && SpiritFormState.get(player).hasAbility(SpiritAbility.SENSE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        prevStrength = strength;
        if (mc.player == null || mc.level == null) {
            strength = 0.0F;
            prevStrength = 0.0F;
            close();
            return;
        }
        strength = wanted(mc) ? Math.min(1.0F, strength + STEP) : Math.max(0.0F, strength - STEP);
    }

    /** Раніше за рамки сховищ (вони малюються на цьому ж етапі), щоб рамки не приглушувалися. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float value = prevStrength + (strength - prevStrength) * partial;
        if (value <= 0.001F) {
            if (strength <= 0.0F && chain != null) {
                close();
            }
            return;
        }
        if (failed) {
            return;
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        try {
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CHAIN);
                lastWidth = 0;
            }
            if (width != lastWidth || height != lastHeight) {
                chain.resize(width, height);
                lastWidth = width;
                lastHeight = height;
            }
            chain.setUniform("Strength", value);

            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(event.getPartialTick().getGameTimeDeltaPartialTick(false));
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.enableDepthTest();
        } catch (Exception e) {
            failed = true;
            LOGGER.error("Spirit Sense screen effect failed to load", e);
            close();
            failed = true;
        }
    }
}
