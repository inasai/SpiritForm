package com.spiritform.mixin;

import com.spiritform.SpiritFormState;
import com.spiritform.DisguiseData;
import com.spiritform.SpiritAttachments;
import com.spiritform.SpiritDisguise;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;

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
        if (SpiritFormState.isActive(self) && SpiritFormState.get(self).isPhasing()) {
            self.noPhysics = true;
            // Політ гравець вмикає сам (двічі Пробіл). Без польоту EntityMixin лишає йому тверду підлогу.
            // У польоті з noPhysics гра не перевіряє землю, і прапорець "стоїть на землі" залишався б
            // увімкненим. Тоді рух рахується за формулою ходьби, а не польоту, і привид
            // виявлявся повільним. Так само робить режим спостерігача.
            if (self.getAbilities().flying) {
                self.setOnGround(false);
            }
        }
    }

    /**
     * У формі Привида гравець завжди стоїть у повний зріст. Інакше в проході заввишки один блок (або всередині
     * блока) гра переводила його в позу повзання чи плавання, а це робило привида дуже повільним.
     */
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true, require = 0)
    private void spiritForm$alwaysStanding(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (SpiritFormState.isActive(self) && SpiritFormState.get(self).isPhasing()) {
            self.setPose(Pose.STANDING);
            ci.cancel();
        }
    }

    /** Вигляд, для якого востаннє перераховано розміри (щоб робити це лише при зміні). */
    @Unique
    private String spiritForm$lookType = "";
    @Unique
    private boolean spiritForm$lookBaby;

    /**
     * Вкрадена зовнішність: хітбокс і висота очей беруться з вкраденого моба (для всіх поз), тож гравець
     * справді стає завбільшки з нього. Працює і на клієнті, і на сервері, щоб рух і зіткнення збігалися.
     */
    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true, require = 0)
    private void spiritForm$disguiseSize(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;
        if (!self.hasData(SpiritAttachments.DISGUISE)) {
            return;
        }
        DisguiseData data = self.getData(SpiritAttachments.DISGUISE);
        if (!data.active()) {
            return;
        }
        EntityType<?> type = SpiritDisguise.type(data);
        if (type != null) {
            EntityDimensions size = type.getDimensions();
            cir.setReturnValue(data.baby() ? size.scale(0.5F) : size);
        }
    }

    /** Коли вигляд надягли, зняли чи змінили, розміри гравця перераховуються. */
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void spiritForm$refreshLook(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        String type = "";
        boolean baby = false;
        if (self.hasData(SpiritAttachments.DISGUISE)) {
            DisguiseData data = self.getData(SpiritAttachments.DISGUISE);
            if (data.active()) {
                type = data.type();
                baby = data.baby();
            }
        }
        if (baby != spiritForm$lookBaby || !type.equals(spiritForm$lookType)) {
            spiritForm$lookType = type;
            spiritForm$lookBaby = baby;
            self.refreshDimensions();
        }
    }
}
