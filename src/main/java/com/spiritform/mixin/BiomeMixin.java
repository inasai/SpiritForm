package com.spiritform.mixin;

import com.spiritform.herald.SpiritHeraldWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Штучна зима Вісника Холоду: у її зоні опади в будь-якому біомі показуються як сніг.
 * Сам сніг і лід на землі ставить SpiritHeraldWorld незалежно від цього. require = 0: безпечно, якщо метод інший.
 */
@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Inject(method = "getPrecipitationAt", at = @At("RETURN"), cancellable = true, require = 0)
    private void spiritForm$winter(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (cir.getReturnValue() == Biome.Precipitation.RAIN && SpiritHeraldWorld.isWinterAt(pos)) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        }
    }
}
