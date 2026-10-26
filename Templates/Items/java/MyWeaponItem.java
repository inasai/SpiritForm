package com.spiritform;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;

/**
 * Зброя MyWeapon. Шкода й швидкість атаки задаються тут у createAttributes(tier, шкода, швидкість).
 * Для інструментів замініть SwordItem на PickaxeItem, AxeItem, ShovelItem чи HoeItem.
 * Особливі дії: перевизначте hurtEnemy (удар), use (ПКМ) або inventoryTick (щотіку в інвентарі).
 */
public class MyWeaponItem extends SwordItem {

    public MyWeaponItem(Item.Properties properties) {
        super(MyTier.MY_TIER, properties.attributes(SwordItem.createAttributes(MyTier.MY_TIER, 3, -2.4F)));
    }
}
