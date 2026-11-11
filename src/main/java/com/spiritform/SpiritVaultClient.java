package com.spiritform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Клієнтська частина сховища.
 *
 * 1. Показ броні з комірок на персонажі. Перед малюванням гравця у справжні слоти броні на мить підставляються
 *    предмети з комірок (шолом, нагрудник, штани, чоботи, крім прихованих "оком"), а після малювання
 *    повертаються. Тож усі звичайні шари (ванільна броня, оздоблення, блиск, моделі Обладунків Потойбіччя)
 *    малюють саме їх, з пріоритетом над одягненою бронею. Запис іде прямо в список броні інвентаря, тому
 *    ні звуків, ні подій, ні пакетів така підміна не викликає.
 *
 * 2. Затискання ПКМ порожньою рукою для Коси й Ловця Душ у сховищі: кнопка підтверджується серверу кожні 4 тіки.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritVaultClient {

    private static final int KEEPALIVE_TICKS = 4;

    /** Гравець, у якого зараз підставлено броню, і його справжні предмети (індекс як в Inventory.armor: 0 = чоботи). */
    private static Player swapped;
    private static final ItemStack[] SAVED = new ItemStack[4];
    private static int keepalive;

    private SpiritVaultClient() {
    }

    // ---------- показ броні ----------

    private static void restore() {
        if (swapped == null) {
            return;
        }
        for (int i = 0; i < SAVED.length; i++) {
            if (SAVED[i] != null) {
                swapped.getInventory().armor.set(i, SAVED[i]);
                SAVED[i] = null;
            }
        }
        swapped = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        restore();
        Player player = event.getEntity();
        VaultData data = SpiritVault.get(player);
        for (int cell = 0; cell < 4; cell++) {
            ItemStack stored = data.gearStack(cell);
            if (!data.gearUnlocked(cell) || stored.isEmpty() || data.gearHidden(cell)) {
                continue;
            }
            int index = 3 - cell;
            SAVED[index] = player.getInventory().armor.get(index);
            player.getInventory().armor.set(index, stored);
            swapped = player;
        }
    }

    @SubscribeEvent
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        restore();
    }

    // ---------- затискання ПКМ ----------

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || mc.isPaused()) {
            keepalive = 0;
            return;
        }
        if (!mc.options.keyUse.isDown() || !player.getMainHandItem().isEmpty() || player.isUsingItem()) {
            keepalive = 0;
            return;
        }

        int kind;
        if (player.isShiftKeyDown()) {
            ItemStack catcher = SpiritVault.handStack(player, SpiritVault.CELL_OFF_HAND, SpiritItems.SOUL_CATCHER.get());
            if (catcher.isEmpty() || !SoulCatcherItem.isFull(catcher)) {
                return;
            }
            kind = SpiritVaultHands.KIND_CATCHER;
        } else {
            if (SpiritReaper.vaultScythe(player).isEmpty()) {
                return;
            }
            kind = SpiritVaultHands.KIND_SCYTHE;
        }

        if (keepalive-- <= 0) {
            keepalive = KEEPALIVE_TICKS;
            PacketDistributor.sendToServer(new VaultHandPayload(kind));
        }
    }
}
