package com.spiritform;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Меню армії у режимі здібностей: хотбар знову змінюється. На цифрах 1-3: виклик, накази, видалення душ.
 * Вийти назад до панелі здібностей можна клавішею режиму (R).
 */
public final class SpiritArmyMode {

    private static final String[] ICONS = {"army_summon", "army_orders", "order_retreat", "army_souls"};

    private static boolean open;

    /** Скільки мілісекунд після входу показується підказка про клавіші. */
    private static final long HINT_MILLIS = 3000L;
    private static long openedAt;

    private SpiritArmyMode() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static void open() {
        open = true;
        openedAt = System.currentTimeMillis();
    }

    public static void close() {
        open = false;
    }

    /** Цифри 1-3 відкривають колеса, решту цифр і основні клавіші гасимо. */
    public static void tick(Minecraft minecraft, boolean noScreen) {
        KeyMapping[] keys = minecraft.options.keyHotbarSlots;
        for (int i = 0; i < keys.length; i++) {
            while (keys[i].consumeClick()) {
                if (!noScreen) {
                    continue;
                }
                if (i == 0) {
                    minecraft.setScreen(ArmyWheelScreen.summon());
                } else if (i == 1) {
                    minecraft.setScreen(ArmyWheelScreen.orders());
                } else if (i == 2) {
                    PacketDistributor.sendToServer(new ArmyActionPayload(SpiritArmy.ACT_RECALL, ""));
                } else if (i == 3) {
                    minecraft.setScreen(ArmyWheelScreen.souls());
                }
            }
        }
        for (KeyMapping key : SpiritKeyMappings.MAIN) {
            while (key.consumeClick()) {
                // у меню армії основні клавіші не працюють
            }
        }
    }

    public static void draw(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int size = 22;
        int baseX = width / 2 - (4 * size) / 2;
        int y = height - 22;

        for (int i = 0; i < 4; i++) {
            int x = baseX + i * size;
            graphics.fill(x, y, x + size, y + size, 0xB0101018);
            graphics.renderOutline(x, y, size, size, 0xFF8CFFC8);
            SpiritIcons.draw(graphics, ICONS[i], x + 3, y + 3);
            Component label = minecraft.options.keyHotbarSlots[i].getTranslatedKeyMessage();
            graphics.drawString(minecraft.font, label, x + size - 2 - minecraft.font.width(label), y + size - 9, 0xFFFFFF, true);
        }

        Component storage = Component.translatable("screen.spirit_form.army.storage",
                SpiritArmyClient.souls().size(), data.armySlots());
        graphics.drawCenteredString(minecraft.font, storage, width / 2, y - 24, 0x8CFFC8);
        Component order = Component.translatable("screen.spirit_form.army.current_order",
                Component.translatable("order.spirit_form."
                        + SpiritArmy.Order.values()[Math.max(0, Math.min(SpiritArmy.Order.values().length - 1,
                        SpiritArmyClient.order()))].name().toLowerCase()));
        graphics.drawCenteredString(minecraft.font, order, width / 2, y - 14, 0xFFFFFF);
        if (System.currentTimeMillis() - openedAt < HINT_MILLIS) {
            graphics.drawCenteredString(minecraft.font,
                    Component.translatable("screen.spirit_form.army.hint", Component.keybind("key.spirit_form.ability_mode")),
                    width / 2, y - 34, 0xAAAAAA);
        }
    }
}
