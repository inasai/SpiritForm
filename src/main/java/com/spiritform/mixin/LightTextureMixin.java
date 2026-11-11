package com.spiritform.mixin;

import com.spiritform.SpiritFormState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Нічне бачення у формі Привида без зілля: освітлення вважає, що ефект є, тож іконки й частинок немає взагалі.
 * Працює лише на клієнті гравця; перший виклик hasEffect у updateLightTexture питає саме про нічне бачення.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Redirect(method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z", ordinal = 0),
            require = 0)
    private boolean spiritForm$nightVision(LocalPlayer player, Holder<MobEffect> effect) {
        return player.hasEffect(effect) || SpiritFormState.isActive(player);
    }
}
