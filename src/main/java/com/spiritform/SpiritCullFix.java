package com.spiritform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Зір у формі Привида, як у режимі спостерігача. Ванілла вимикає розумне відсікання секцій (smart cull, "cave culling"),
 * коли спостерігач стоїть усередині суцільного блока, інакше з породи майже нічого не видно. Тут те саме
 * робиться для Привида: поки форма активна, а камера в суцільному блоці, відсікання вимкнене.
 * Поза цим станом значення повертається назад, а не залишається вимкненим.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritCullFix {

    /** Чи вимкнули відсікання ми самі (щоб повертати лише своє). */
    private static boolean overridden;

    private SpiritCullFix() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean want = false;
        if (player != null && mc.level != null && SpiritFormState.isActive(player)) {
            BlockPos pos = BlockPos.containing(player.getEyePosition());
            want = mc.level.getBlockState(pos).isSolidRender(mc.level, pos);
        }

        if (want && !overridden) {
            overridden = true;
            mc.smartCull = false;
            mc.levelRenderer.needsUpdate();
        } else if (!want && overridden) {
            overridden = false;
            mc.smartCull = true;
            mc.levelRenderer.needsUpdate();
        }
    }
}
