package com.spiritform;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.FlowerBlock;

/** Полудниця: квітка, схожа на сонце. Росте колами в Рівнинах Потойбіччя й веде до Польовика. */
public class PoludnytsiaBlock extends FlowerBlock {

    public PoludnytsiaBlock(Properties properties) {
        super(MobEffects.SATURATION, 4.0F, properties);
    }
}
