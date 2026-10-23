package com.spiritform;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Кнопка з маленькою піксельною іконкою замість тексту (шестерня, стрілка скидання).
 * Іконки малюються самі, тому не залежать від шрифтів і текстур.
 */
public class IconButton extends Button {

    public enum Icon {
        GEAR(new String[] {
                "...###...",
                ".#.###.#.",
                "..#####..",
                "###...###",
                "###...###",
                "###...###",
                "..#####..",
                ".#.###.#.",
                "...###..."
        }),
        RESET(new String[] {
                "..####...",
                ".#....#..",
                "#......#.",
                "#.....###",
                "#......#.",
                "#........",
                ".#.....#.",
                "..#####..",
                "........."
        });

        private final String[] rows;

        Icon(String[] rows) {
            this.rows = rows;
        }
    }

    private final Icon icon;

    public IconButton(int x, int y, int width, int height, Icon icon, OnPress onPress, Component tooltip) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        setTooltip(Tooltip.create(tooltip));
    }

    public void setTooltipText(Component text) {
        setTooltip(Tooltip.create(text));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        int color = active ? 0xFFFFFFFF : 0xFF808080;
        int originX = getX() + (getWidth() - 9) / 2;
        int originY = getY() + (getHeight() - 9) / 2;

        for (int row = 0; row < icon.rows.length; row++) {
            String line = icon.rows[row];
            for (int column = 0; column < line.length(); column++) {
                if (line.charAt(column) == '#') {
                    int x = originX + column;
                    int y = originY + row;
                    graphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }
}
