package com.spiritform.mixin;

import com.spiritform.SpiritFormState;
import com.spiritform.SpiritTransparency;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.spiritform.SpiritData;

/**
 * Напівпрозорий вигляд привида.
 *
 * Ванільний рендер уже вміє малювати "прозорі" сутності (так видно невидимих союзників).
 * Для цього він бере інший тип рендеру й колір з прозорістю, але лише якщо тіло "невидиме".
 * Тому ми:
 *  1. кажемо рендеру, що тіло привида невидиме (isBodyVisible = false);
 *  2. замінюємо ванільну дуже слабку прозорість на свою (SpiritTransparency.GHOST_COLOR).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "isBodyVisible", at = @At("HEAD"), cancellable = true, require = 0)
    private void spiritForm$ghostBody(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        boolean spirit = false;

        if (entity instanceof Player player) {
            SpiritData data = SpiritFormState.get(player);
            spirit = data.active() || data.invisible();
        }

        SpiritTransparency.RENDERING_SPIRIT_PLAYER = spirit;

        if (spirit) {
            cir.setReturnValue(false);
        }
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            ),
            index = 4,
            require = 0
    )
    private int spiritForm$ghostColor(int color) {
        // -1 означає непрозорий колір; змінюємо лише тоді, коли рендер вибрав прозорий режим
        if (SpiritTransparency.RENDERING_SPIRIT_PLAYER && color != -1) {
            return SpiritTransparency.GHOST_COLOR;
        }
        return color;
    }
}
