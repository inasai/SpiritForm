package com.spiritform.mixin;

import com.spiritform.SpiritFormState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Проходження крізь блоки.
 *
 * Гравець у режимі спостерігача проходить крізь блоки завдяки прапорцю noPhysics, але Player.tick
 * скидає його щотіку (noPhysics = isSpectator()). Тому ми вмикаємо прапорець знову
 * саме перед рухом, тобто перед викликом super.tick().
 *
 * Цей Mixin працює і на клієнті, і на сервері, тому він записаний у список "mixins",
 * а не "client".
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;tick()V"),
            require = 0
    )
    private void spiritForm$noClip(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (SpiritFormState.isActive(self)) {
            self.noPhysics = true;
            // Без гравітації й колізій вихід з польоту означав би падіння крізь світ,
            // тому привид завжди літає
            self.getAbilities().flying = true;
            // Із noPhysics гра не перевіряє землю, і прапорець "стоїть на землі" залишався б
            // увімкненим. Тоді рух рахується за формулою ходьби, а не польоту, і привид
            // виявлявся повільним. Так само робить режим спостерігача.
            self.setOnGround(false);
        }
    }
}
