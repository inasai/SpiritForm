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
        if (control == null || control.flying()) {
            return original; // літуни рухаються окремо (SpiritPossession.onEntityTick)
        }

        float yaw = control.yaw();
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);

        // Так само, як у ванільного ШІ: швидкість моба береться з його характеристики,
        // а вхідний напрямок руху масштабується тією ж характеристикою (Mob.setSpeed задає і zza).
        float speed = SpiritPossession.speedOf(mob);
        mob.setSpeed(speed);

        float scale = speed * SpiritPossession.speedMultiplier();
        return new Vec3(control.strafe() * scale, original.y, control.forward() * scale);
    }
}
