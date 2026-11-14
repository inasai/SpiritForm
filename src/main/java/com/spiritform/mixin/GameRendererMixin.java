package com.spiritform.mixin;

import com.spiritform.form.SpiritFormState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Сила нічного бачення у формі Привида без зілля: повна, без мигання наприкінці. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true, require = 0)
    private static void spiritForm$nightVisionScale(LivingEntity entity, float partialTick,
                                                    CallbackInfoReturnable<Float> cir) {
        if (entity instanceof Player player && SpiritFormState.isActive(player)
                && !player.hasEffect(MobEffects.NIGHT_VISION)) {
            cir.setReturnValue(1.0F);
        }
    }
}
