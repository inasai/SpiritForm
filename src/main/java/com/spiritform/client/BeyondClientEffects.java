package com.spiritform.client;

import com.spiritform.SpiritFormMod;
import com.spiritform.registry.SpiritBiomes;
import com.spiritform.registry.SpiritEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Клієнтські ефекти лісів Потойбіччя: туман у туманному лісі, приглушена далекість у лісі ночі
 * та Інверсія керування (вправо-вліво, камера вгору-вниз).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class BeyondClientEffects {

    private static float mist;
    private static float night;
    private static float lastPitch;
    private static boolean hadPitch;

    private BeyondClientEffects() {
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        var biome = mc.level.getBiome(BlockPos.containing(event.getCamera().getPosition()));
        mist += ((biome.is(SpiritBiomes.MIST_FOREST) ? 1.0F : 0.0F) - mist) * 0.03F;
        night += ((biome.is(SpiritBiomes.NIGHT_FOREST) ? 1.0F : 0.0F) - night) * 0.03F;
        if (mist < 0.01F && night < 0.01F) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        far = Mth.lerp(night, far, Math.min(far, 72.0F));
        far = Mth.lerp(mist, far, Math.min(far, 26.0F));
        near = Mth.lerp(mist, near, 0.0F);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (event.getEntity().hasEffect(SpiritEffects.INVERSION)) {
            Input input = event.getInput();
            boolean left = input.left;
            input.left = input.right;
            input.right = left;
            input.leftImpulse = -input.leftImpulse;
        }
    }

    /** Інвертує вертикальну зміну погляду: кожен кадр відкидає зміну нахилу в протилежний бік. */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(SpiritEffects.INVERSION)) {
            hadPitch = false;
            return;
        }
        if (hadPitch) {
            float delta = player.getXRot() - lastPitch;
            if (delta != 0.0F) {
                player.setXRot(Mth.clamp(lastPitch - delta, -90.0F, 90.0F));
            }
        }
        lastPitch = player.getXRot();
        hadPitch = true;
    }
}
