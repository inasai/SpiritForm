package com.spiritform;

import net.minecraft.world.entity.player.Player;

/** Множник радіуса дії здібностей: з Кайданами Межі удвічі більший. */
public final class SpiritRange {

    private SpiritRange() {
    }

    public static double mul(Player player) {
        return ShacklesItem.isWorn(player) ? 2.0 : 1.0;
    }
}
