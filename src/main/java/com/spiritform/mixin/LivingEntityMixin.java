package com.spiritform.mixin;

import com.spiritform.SpiritPossession;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Керування мобом, у якого вселився привид.
 *
 * ШІ моба щотіку задає свій напрямок руху прямо перед travel(). Тому ми підміняємо
 * вхідний вектор руху, поворот і швидкість у самому travel(), коли моб під контролем гравця.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 spiritForm$possessedInput(Vec3 original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Mob mob)) {
            return original;
        }

        SpiritPossession.Control control = SpiritPossession.controlFor(mob);
        if (control == null) {
            return original;
        }

        float yaw = control.yaw();
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setSpeed(SpiritPossession.speedOf(mob));

        return new Vec3(control.strafe(), original.y, control.forward());
    }
}
