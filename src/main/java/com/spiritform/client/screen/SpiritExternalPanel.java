package com.spiritform.client.screen;

import com.spiritform.client.gui.SpiritUi;
import com.spiritform.data.StationData;
import com.spiritform.network.ExternalStationPayload;
import com.spiritform.vault.AkashicCompat;
import com.spiritform.vault.SpiritStations;
import com.spiritform.vault.Station;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Панель "Сторонні станції" в меню розвитку: вісім комірок і нижче інвентар гравця. Клацніть по блоку в інвентарі
 * (він виділиться), потім по комірці; Shift + клацання кладе в першу вільну. Клацання по зайнятій комірці
 * повертає блок (разом із вмістом станції) й забирає станцію з колеса.
 */
final class SpiritExternalPanel {

    private static final int CELL = 18;
    private static final int ROW_H = 22;
    private static final int COLUMNS = 4;

    private static final int BORDER_DARK = 0xFF08080B;
    private static final int BORDER_LIGHT = 0xFF4A4A54;
    private static final int FILL = 0xFF1B1B20;

    private int selectedSlot = -1;
    private int ox;
    private int oy;
    private int ow;

    private ItemStack tipStack = ItemStack.EMPTY;
    private Component tipText;

    int bottom() {
        return invY() + 4 * CELL + 3;
    }

    void drop() {
        selectedSlot = -1;
    }

    private int cellX(int cell) {
        return ox + (cell % COLUMNS) * (ow / COLUMNS);
    }

    private int cellY(int cell) {
        return oy + (cell / COLUMNS) * ROW_H;
    }

    private int invY() {
        return oy + ((StationData.EXTERNAL_MAX + COLUMNS - 1) / COLUMNS) * ROW_H + 4;
    }

    private int slotY(int row) {
        return invY() + row * CELL + (row == 3 ? 3 : 0);
    }

    private int slotAt(double mx, double my) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int x = ox + col * CELL;
                int y = slotY(row);
                if (inside(mx, my, x, y, CELL, CELL)) {
                    return row == 3 ? col : 9 + row * 9 + col;
                }
            }
        }
        return -1;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void box(GuiGraphics g, int x, int y, int fill, int border) {
        g.fill(x, y, x + CELL, y + CELL, border);
        g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, fill);
    }

    private static boolean eligible(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof BlockItem || AkashicCompat.isBook(stack.getItem()));
    }

    private static void send(int action, int cell, int slot) {
        PacketDistributor.sendToServer(new ExternalStationPayload(action, cell, slot));
    }

    void render(GuiGraphics g, Font font, int mouseX, int mouseY, int x, int y, int w) {
        this.ox = x;
        this.oy = y;
        this.ow = w;
        tipStack = ItemStack.EMPTY;
        tipText = null;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        StationData data = SpiritStations.get(player);
        ItemStack held = selectedSlot >= 0 ? player.getInventory().getItem(selectedSlot) : ItemStack.EMPTY;
        if (!eligible(held)) {
            selectedSlot = -1;
            held = ItemStack.EMPTY;
        }

        for (int cell = 0; cell < StationData.EXTERNAL_MAX; cell++) {
            int cx = cellX(cell);
            int cy = cellY(cell);
            Station station = Station.byIndex(Station.BUILTIN + cell);
            boolean filled = station != null && station.available(data.blocks());
            boolean target = !held.isEmpty() && !filled;
            box(g, cx, cy, FILL, target ? SpiritUi.ACCENT : BORDER_DARK);
            if (!target) {
                g.fill(cx + CELL - 1, cy + 1, cx + CELL, cy + CELL, BORDER_LIGHT);
                g.fill(cx + 1, cy + CELL - 1, cx + CELL, cy + CELL, BORDER_LIGHT);
            }
            if (filled) {
                ItemStack icon = station.stack(data.blocks());
                g.renderItem(icon, cx + 1, cy + 1);
                if (inside(mouseX, mouseY, cx, cy, CELL, CELL)) {
                    tipStack = icon;
                }
            } else if (inside(mouseX, mouseY, cx, cy, CELL, CELL)) {
                tipText = Component.translatable("screen.spirit_form.external.empty");
            }
        }

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = ox + col * CELL;
                int sy = slotY(row);
                int slot = row == 3 ? col : 9 + row * 9 + col;
                ItemStack stack = player.getInventory().getItem(slot);
                boolean chosen = slot == selectedSlot;
                box(g, sx, sy, chosen ? 0xFF2E4A40 : FILL,
                        chosen ? SpiritUi.ACCENT : eligible(stack) ? 0xFF3C8068 : BORDER_DARK);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, sx + 1, sy + 1);
                    g.renderItemDecorations(font, stack, sx + 1, sy + 1);
                    if (inside(mouseX, mouseY, sx, sy, CELL, CELL)) {
                        tipStack = stack;
                    }
                }
            }
        }
    }

    void renderTooltip(GuiGraphics g, Font font, int mouseX, int mouseY) {
        if (!tipStack.isEmpty()) {
            g.renderTooltip(font, tipStack, mouseX, mouseY);
        } else if (tipText != null) {
            g.renderTooltip(font, tipText, mouseX, mouseY);
        }
    }

    boolean mouseClicked(double mx, double my, int button) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || ow == 0) {
            return false;
        }
        if (button == 1) {
            boolean had = selectedSlot >= 0;
            selectedSlot = -1;
            return had;
        }
        if (button != 0) {
            return false;
        }
        StationData data = SpiritStations.get(player);
        for (int cell = 0; cell < StationData.EXTERNAL_MAX; cell++) {
            if (!inside(mx, my, cellX(cell), cellY(cell), CELL, CELL)) {
                continue;
            }
            if (!data.blockAt(cell).isEmpty()) {
                send(SpiritStations.EXT_REMOVE, cell, 0);
            } else if (selectedSlot >= 0) {
                send(SpiritStations.EXT_ADD, cell, selectedSlot);
                selectedSlot = -1;
            }
            return true;
        }
        int slot = slotAt(mx, my);
        if (slot >= 0) {
            if (eligible(player.getInventory().getItem(slot))) {
                if (Screen.hasShiftDown()) {
                    send(SpiritStations.EXT_ADD, -1, slot);
                    selectedSlot = -1;
                } else {
                    selectedSlot = slot == selectedSlot ? -1 : slot;
                }
            }
            return true;
        }
        return false;
    }
}
