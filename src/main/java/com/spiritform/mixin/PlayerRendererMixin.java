package com.spiritform.mixin;

import com.spiritform.FentonBracelet;
import com.spiritform.FentonSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Інверсія кольорів скіна для гравця з Браслетом Фентона (модель і рука від першої особи). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void spiritForm$invertSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        if (FentonBracelet.invertsSkin(player)) {
            cir.setReturnValue(FentonSkin.invert(cir.getReturnValue()));
        }
    }

    @Redirect(method = "renderHand",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/PlayerSkin;texture()Lnet/minecraft/resources/ResourceLocation;"),
            require = 0)
    private ResourceLocation spiritForm$invertHand(PlayerSkin skin) {
        ResourceLocation texture = skin.texture();
        var local = Minecraft.getInstance().player;
        return local != null && FentonBracelet.invertsSkin(local) ? FentonSkin.invert(texture) : texture;
    }
}
