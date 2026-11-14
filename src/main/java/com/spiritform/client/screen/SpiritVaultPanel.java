package com.spiritform.client.screen;

import com.spiritform.client.gui.SpiritUi;
import com.spiritform.data.VaultData;
import com.spiritform.network.VaultActionPayload;
import com.spiritform.vault.SpiritVault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Панель сховища в меню розвитку: комірки артефактів або спорядження й нижче інвентар гравця.
 * Керування: клацніть по предмету інвентаря (він виділиться), потім по комірці, щоб покласти; Shift + клацання
 * кладе в першу вільну комірку. Клацання по зайнятій комірці повертає предмет в інвентар. Біля кожного предмета
 * дві іконки: "око" (показувати на персонажі) і "живлення" (діє предмет чи ні). Заблоковану комірку відкриває
 * клацання: N-та коштує N рівнів досвіду.
 */
final class SpiritVaultPanel {

    private static final int CELL = 18;
    private static final int ROW_H = 22;
    private static final int MINI_X = 21;

    private static final String[] EYE = {
            "..#####..",
            ".#.....#.",
            "#...#...#",
            ".#.....#.",
            "..#####.."
    };
    private static final String[] POWER = {
            "...#...",
            ".#.#.#.",
            "#..#..#",
            "#.....#",
            "#.....#",
            ".#...#.",
            "..###.."
    };
    private static final String[] LOCK = {
            "..###..",
            ".#...#.",
            ".#...#.",
            "#######",
            "#######",
            "###.###",
            "#######"
    };

    private static final int BORDER_DARK = 0xFF08080B;
    private static final int BORDER_LIGHT = 0xFF4A4A54;
    private static final int FILL = 0xFF1B1B20;
    private static final int FILL_LOCKED = 0xFF101014;
    private static final int OFF = 0xFF6A6A74;

    private int selectedSlot = -1;

    // Розкладка останнього кадру: потрібна, щоб клацання потрапляло туди, де щойно намалювали
    private int ox;
    private int oy;
    private int ow;
    private int kind;

    private ItemStack tipStack = ItemStack.EMPTY;
    private Component tipText;

    /** Нижня межа панелі (під нею екран малює опис). */
    int bottom() {
        return invY() + 4 * CELL + 3;
    }

    /** Скидає вибраний предмет (при зміні рядка в меню). */
    void drop() {
        selectedSlot = -1;
    }

    // ---------- розкладка ----------

    private int columns() {
        return kind == SpiritVault.KIND_ARTIFACT ? 4 : 3;
    }

    private int cellX(int cell) {
        int columnWidth = ow / columns();
        return ox + (cell % columns()) * columnWidth;
    }

    private int cellY(int cell) {
        return oy + (cell / columns()) * ROW_H;
    }

    private int cellRows() {
        return (SpiritVault.maxCells(kind) + columns() - 1) / columns();
    }

    private int invY() {
        return oy + cellRows() * ROW_H + 4;
    }

    private static int slotX(int ox, int column) {
        return ox + column * CELL;
    }

    private int slotY(int row) {
        return invY() + row * CELL + (row == 3 ? 3 : 0);
    }

    /** Номер слота інвентаря (0..8 хотбар, 9..35 решта) під точкою або -1. */
    private int slotAt(double mx, double my) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int x = slotX(ox, col);
                int y = slotY(row);
                if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) {
                    return row == 3 ? col : 9 + row * 9 + col;
                }
            }
        }
        return -1;
    }

    private int cellAt(double mx, double my) {
        for (int cell = 0; cell < SpiritVault.maxCells(kind); cell++) {
            int x = cellX(cell);
            int y = cellY(cell);
            if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) {
                return cell;
            }
        }
        return -1;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean eligibleAny(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int cell = 0; cell < SpiritVault.maxCells(kind); cell++) {
            if (SpiritVault.eligible(kind, cell, stack)) {
                return true;
            }
        }
        return false;
    }

    private static void send(int action, int kind, int cell, int slot) {
        PacketDistributor.sendToServer(new VaultActionPayload(action, kind, cell, slot));
    }

    // ---------- малювання ----------

    private static void drawPixels(GuiGraphics g, String[] art, int x, int y, int color) {
        for (int row = 0; row < art.length; row++) {
            for (int col = 0; col < art[row].length(); col++) {
                if (art[row].charAt(col) == '#') {
                    g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    private static void box(GuiGraphics g, int x, int y, int fill, int border) {
        g.fill(x, y, x + CELL, y + CELL, border);
        g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, fill);
    }

    /** "Око": для артефактів завжди, для спорядження лише в комірках броні (руки не малюються). */
    private static boolean hasEye(int kind, int cell, ItemStack stack) {
        return kind == SpiritVault.KIND_ARTIFACT ? SpiritVault.isArtifact(stack) : cell < 4;
    }

    private void drawEye(GuiGraphics g, int x, int y, boolean shown) {
        drawPixels(g, EYE, x, y + 1, shown ? SpiritUi.ACCENT : OFF);
        if (!shown) {
            for (int i = 0; i < 9; i++) {
                g.fill(x + i, y + 5 - i * 5 / 9, x + i + 1, y + 6 - i * 5 / 9, SpiritUi.TEXT_BAD);
            }
        }
    }

    void render(GuiGraphics g, Font font, int mouseX, int mouseY, int x, int y, int w, int kind) {
        this.ox = x;
        this.oy = y;
        this.ow = w;
        this.kind = kind;
        tipStack = ItemStack.EMPTY;
        tipText = null;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        VaultData data = SpiritVault.get(player);
        int max = SpiritVault.maxCells(kind);
        int unlockedCount = SpiritVault.unlockedCount(data, kind);
        int cost = SpiritVault.nextCost(data, kind);
        boolean full = unlockedCount >= max;
        boolean canPay = player.getAbilities().instabuild || player.experienceLevel >= cost;

        // Лічильник комірок і ціна наступної праворуч від заголовка
        Component counter = Component.translatable("screen.spirit_form.vault.cells", unlockedCount, max);
        if (!full) {
            counter = counter.copy().append(" · ").append(Component.translatable("screen.spirit_form.vault.next", cost));
        }
        SpiritUi.text(g, font, counter, x + w - font.width(counter), y - 16, full || canPay ? SpiritUi.TEXT_DIM : SpiritUi.TEXT_BAD);

        ItemStack held = selectedSlot >= 0 ? player.getInventory().getItem(selectedSlot) : ItemStack.EMPTY;
        if (held.isEmpty()) {
            selectedSlot = -1;
        }

        // Комірки
        int nextArtifact = data.artifactCells();
        for (int cell = 0; cell < max; cell++) {
            int cx = cellX(cell);
            int cy = cellY(cell);
            boolean unlocked = SpiritVault.unlocked(data, kind, cell);
            ItemStack stack = SpiritVault.stackAt(data, kind, cell);

            boolean target = !held.isEmpty() && unlocked && SpiritVault.eligible(kind, cell, held);
            box(g, cx, cy, unlocked ? FILL : FILL_LOCKED, target ? SpiritUi.ACCENT : BORDER_DARK);
            if (!target) {
                g.fill(cx + CELL - 1, cy + 1, cx + CELL, cy + CELL, BORDER_LIGHT);
                g.fill(cx + 1, cy + CELL - 1, cx + CELL, cy + CELL, BORDER_LIGHT);
            }

            if (!unlocked) {
                boolean next = kind == SpiritVault.KIND_ARTIFACT ? cell == nextArtifact : !full;
                drawPixels(g, LOCK, cx + 5, cy + 5, next ? SpiritUi.TEXT_DIM : 0xFF3A3A42);
                if (next) {
                    SpiritUi.text(g, font, String.valueOf(cost), cx + CELL - font.width(String.valueOf(cost)) - 1, cy + CELL - 9,
                            canPay ? SpiritUi.ACCENT : SpiritUi.TEXT_BAD);
                }
            } else if (!stack.isEmpty()) {
                g.renderItem(stack, cx + 1, cy + 1);
                g.renderItemDecorations(font, stack, cx + 1, cy + 1);
                if (SpiritVault.disabledAt(data, kind, cell)) {
                    g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x99101014);
                }
            }

            // Назва комірки спорядження поруч
            if (kind == SpiritVault.KIND_GEAR) {
                g.pose().pushPose();
                g.pose().translate(cx + MINI_X + 12, cy + 5, 0.0F);
                g.pose().scale(0.75F, 0.75F, 1.0F);
                SpiritUi.text(g, font, Component.translatable("screen.spirit_form.vault.slot." + cell), 0, 0, SpiritUi.TEXT_DIM);
                g.pose().popPose();
            }

            // Іконки "око" й "живлення" біля зайнятої комірки
            if (unlocked && !stack.isEmpty()) {
                if (hasEye(kind, cell, stack)) {
                    drawEye(g, cx + MINI_X, cy + 1, !SpiritVault.hiddenAt(data, kind, cell));
                }
                drawPixels(g, POWER, cx + MINI_X + 1, cy + 10,
                        SpiritVault.disabledAt(data, kind, cell) ? SpiritUi.TEXT_BAD : SpiritUi.ACCENT);
            }

            // Підказка
            if (inside(mouseX, mouseY, cx, cy, CELL, CELL)) {
                if (!unlocked) {
                    tipText = Component.translatable("screen.spirit_form.vault.locked", cost);
                } else if (!stack.isEmpty()) {
                    tipStack = stack;
                } else {
                    tipText = Component.translatable(kind == SpiritVault.KIND_GEAR
                            ? "screen.spirit_form.vault.empty_gear" : "screen.spirit_form.vault.empty");
                }
            } else if (unlocked && !stack.isEmpty()) {
                if (hasEye(kind, cell, stack) && inside(mouseX, mouseY, cx + MINI_X, cy + 1, 9, 7)) {
                    tipText = Component.translatable(SpiritVault.hiddenAt(data, kind, cell)
                            ? "screen.spirit_form.vault.eye.hidden" : "screen.spirit_form.vault.eye.shown");
                } else if (inside(mouseX, mouseY, cx + MINI_X, cy + 10, 9, 7)) {
                    tipText = Component.translatable(SpiritVault.disabledAt(data, kind, cell)
                            ? "screen.spirit_form.vault.power.off" : "screen.spirit_form.vault.power.on");
                }
            }
        }

        // Інвентар гравця
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = slotX(ox, col);
                int sy = slotY(row);
                int slot = row == 3 ? col : 9 + row * 9 + col;
                ItemStack stack = player.getInventory().getItem(slot);
                boolean storable = eligibleAny(stack);
                boolean chosen = slot == selectedSlot;
                box(g, sx, sy, chosen ? 0xFF2E4A40 : FILL, chosen ? SpiritUi.ACCENT : storable ? 0xFF3C8068 : BORDER_DARK);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, sx + 1, sy + 1);
                    g.renderItemDecorations(font, stack, sx + 1, sy + 1);
                }
                if (!stack.isEmpty() && inside(mouseX, mouseY, sx, sy, CELL, CELL)) {
                    tipStack = stack;
                }
            }
        }
    }

    /** Підказка малюється поза обрізанням області деталей, інакше її зрізало б. */
    void renderTooltip(GuiGraphics g, Font font, int mouseX, int mouseY) {
        if (!tipStack.isEmpty()) {
            g.renderTooltip(font, tipStack, mouseX, mouseY);
        } else if (tipText != null) {
            g.renderTooltip(font, tipText, mouseX, mouseY);
        }
    }

    // ---------- керування ----------

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

        VaultData data = SpiritVault.get(player);
        for (int cell = 0; cell < SpiritVault.maxCells(kind); cell++) {
            int cx = cellX(cell);
            int cy = cellY(cell);
            boolean unlocked = SpiritVault.unlocked(data, kind, cell);
            ItemStack stack = SpiritVault.stackAt(data, kind, cell);

            if (unlocked && !stack.isEmpty()) {
                if (hasEye(kind, cell, stack) && inside(mx, my, cx + MINI_X, cy + 1, 9, 7)) {
                    send(SpiritVault.ACT_TOGGLE_VISIBLE, kind, cell, 0);
                    return true;
                }
                if (inside(mx, my, cx + MINI_X, cy + 10, 9, 7)) {
                    send(SpiritVault.ACT_TOGGLE_ACTIVE, kind, cell, 0);
                    return true;
                }
            }
            if (!inside(mx, my, cx, cy, CELL, CELL)) {
                continue;
            }
            if (!unlocked) {
                send(SpiritVault.ACT_UNLOCK, kind, cell, 0);
            } else if (selectedSlot >= 0) {
                send(SpiritVault.ACT_PUT, kind, cell, selectedSlot);
                selectedSlot = -1;
            } else if (!stack.isEmpty()) {
                send(SpiritVault.ACT_TAKE, kind, cell, 0);
            }
            return true;
        }

        int slot = slotAt(mx, my);
        if (slot >= 0) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (eligibleAny(stack)) {
                if (Screen.hasShiftDown()) {
                    send(SpiritVault.ACT_PUT, kind, -1, slot);
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
