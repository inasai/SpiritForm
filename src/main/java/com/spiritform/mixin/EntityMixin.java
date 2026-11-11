package com.spiritform.mixin;

import com.spiritform.SpiritData;
import com.spiritform.SpiritFormState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Проходження крізь блоки без польоту: гравець проходить крізь стіни, але стоїть на поверхні.
 * Рух розбивається на горизонтальну частину (без колізій, тож стіни не заважають) і вертикальну
 * (з колізіями, тож підлога тримає). Якщо затиснути присідання, підлога теж стає прохідною.
 * Блоки, у яких уже стоїть гравець, вертикальні колізії ігнорують, тому з них можна вільно вийти.
 * Рух угору (стрибок) теж іде без колізій, тож стелі й блоки над головою не заважають.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Unique
    private boolean spiritForm$splitting;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void spiritForm$phaseOnSurface(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (spiritForm$splitting || !((Object) this instanceof Player player)) {
            return;
        }
        if (!SpiritFormState.isActive(player)) {
            return;
        }
        SpiritData data = SpiritFormState.get(player);
        if (!data.isPhasing() || player.getAbilities().flying || player.isShiftKeyDown()) {
            return;
        }

        Entity self = (Entity) (Object) this;
        spiritForm$splitting = true;
        try {
            boolean previous = self.noPhysics;
            self.noPhysics = true;
            self.move(type, new Vec3(movement.x, 0.0, movement.z));
            if (movement.y > 0.0) {
                // Угору (стрибок) стелі не заважають: рух без колізій, а на землі гравець уже не стоїть
                self.move(type, new Vec3(0.0, movement.y, 0.0));
                self.setOnGround(false);
            } else {
                // Униз і на місці: підлога тримає
                self.noPhysics = false;
                self.move(type, new Vec3(0.0, movement.y, 0.0));
            }
            self.noPhysics = previous;
        } finally {
            spiritForm$splitting = false;
        }
        ci.cancel();
    }
}
