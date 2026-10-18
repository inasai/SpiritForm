package com.spiritform.mixin;

import com.spiritform.SpiritClientHighlight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Підсвічування мобів лише для себе (рівень 15 гілки Темряви).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true, require = 0)
    private void spiritForm$highlightMobs(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && SpiritClientHighlight.shouldHighlight(entity)) {
            cir.setReturnValue(true);
        }
    }
}
