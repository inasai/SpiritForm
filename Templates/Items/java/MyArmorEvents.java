package com.spiritform;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Бонус за повний комплект MyArmor. Необов'язковий файл: якщо бонус не потрібен, його можна не копіювати. */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class MyArmorEvents {

    private MyArmorEvents() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) {
            return;
        }
        boolean full = player.getItemBySlot(EquipmentSlot.HEAD).is(SpiritItems.MY_ARMOR_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(SpiritItems.MY_ARMOR_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(SpiritItems.MY_ARMOR_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(SpiritItems.MY_ARMOR_BOOTS.get());
        if (full) {
            // Приклад бонусу: нічне бачення. Замініть на свій.
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }
    }
}
