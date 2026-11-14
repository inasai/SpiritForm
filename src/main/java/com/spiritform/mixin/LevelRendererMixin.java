package com.spiritform.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.spiritform.client.SpiritHeraldClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Кривавий дощ Вісника Голоду: поки поряд Вісник Голоду, дощ малюється червоним (множник кольору шейдера).
 * require = 0: якщо в іншій збірці метод зміниться, гра запуститься, а дощ просто лишиться звичайним.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), require = 0)
    private void spiritForm$redRainStart(LightTexture lightTexture, float partialTick, double camX, double camY,
                                         double camZ, CallbackInfo ci) {
        if (SpiritHeraldClient.redRain()) {
            RenderSystem.setShaderColor(1.0F, 0.12F, 0.1F, 1.0F);
        }
    }

    @Inject(method = "renderSnowAndRain", at = @At("RETURN"), require = 0)
    private void spiritForm$redRainEnd(LightTexture lightTexture, float partialTick, double camX, double camY,
                                       double camZ, CallbackInfo ci) {
        if (SpiritHeraldClient.redRain()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
