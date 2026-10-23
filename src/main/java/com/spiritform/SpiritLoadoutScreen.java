package com.spiritform;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Редактор однієї з трьох панелей здібностей (пасивні, активні, основні).
 * Клацни здібність у нижньому ряду або в клітинці, щоб узяти її, потім клацни клітинку, щоб покласти
 * (якщо там щось є, вони міняються місцями). Права кнопка очищає клітинку. Shift + клац по здібності
 * відкриває вікно вибору іконки.
 */
public class SpiritLoadoutScreen extends SpiritScreenBase {

    private static final int CELL = 26;
    private static final int GAP = 4;

    private final SpiritLoadout.Bar bar;
    private final List<String> pool;

    /** Здібність, яку тримає курсор. Порожній рядок означає, що нічого. */
    private String held = "";

    public SpiritLoadoutScreen(Screen parent, SpiritLoadout.Bar bar) {
        super(Component.translatable("screen.spirit_form.loadout." + bar.key()), parent);
        this.bar = bar;
        this.pool = SpiritLoadout.candidates(bar);
    }

    private static int rowWidth(int count) {
        return count * CELL + (count - 1) * GAP;
    }

    private int slotX(int index) {
        return left + (PANEL_W - rowWidth(bar.size)) / 2 + index * (CELL + GAP);
    }

    private int slotY() {
        return top + 74;
    }

    private int poolX(int index) {
        return left + (PANEL_W - rowWidth(pool.size())) / 2 + index * (CELL + GAP);
    }

    private int poolY() {
        return top + 150;
    }

    private static boolean inside(double mx, double my, int x, int y) {
        return mx >= x && mx < x + CELL && my >= y && my < y + CELL;
    }

    /** Чи відкрита здібність (інакше її не можна взяти з нижнього ряду). */
    private boolean owned(String id) {
        if (SpiritLoadout.INSTINCT.equals(id)) {
            return data().unlocked();
        }
        SpiritAbility ability = SpiritAbility.byId(id);
        return ability != null && data().hasAbility(ability);
    }

    @Override
    protected void buildContent() {
    }

    @Override
    protected void refresh() {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (int i = 0; i < bar.size; i++) {
            if (!inside(mouseX, mouseY, slotX(i), slotY())) {
                continue;
            }
            String current = SpiritLoadout.get(bar, i);

            if (button == 1) {
                SpiritLoadout.set(bar, i, "");
                return true;
            }
            if (button == 0) {
                if (held.isEmpty()) {
                    held = current; // взяти здібність із клітинки
                    if (!current.isEmpty()) {
                        SpiritLoadout.set(bar, i, "");
                    }
                } else {
                    SpiritLoadout.set(bar, i, held);
                    held = current; // обмін
                }
                return true;
            }
        }

        for (int i = 0; i < pool.size(); i++) {
            if (!inside(mouseX, mouseY, poolX(i), poolY()) || button != 0) {
                continue;
            }
            String id = pool.get(i);
            if (!held.isEmpty()) {
                held = ""; // покласти назад: здібність просто лишається поза панеллю
            } else if (owned(id)) {
                held = id;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.translatable("screen.spirit_form.loadout.hint1"),
                width / 2, top + 34, 0xAAAAAA);
        graphics.drawCenteredString(font, Component.translatable("screen.spirit_form.loadout.hint2"),
                width / 2, top + 46, 0xAAAAAA);

        String hovered = "";

        for (int i = 0; i < bar.size; i++) {
            int x = slotX(i);
            int y = slotY();
            String id = SpiritLoadout.get(bar, i);
            boolean hover = inside(mouseX, mouseY, x, y);
            drawCell(graphics, x, y, id, hover);
            graphics.drawCenteredString(font, slotLabel(i), x + CELL / 2, y + CELL + 3, 0x909090);
            if (hover && !id.isEmpty()) {
                hovered = id;
            }
        }

        graphics.drawCenteredString(font, Component.translatable("screen.spirit_form.loadout.pool"),
                width / 2, poolY() - 14, 0xFFFFFF);
        for (int i = 0; i < pool.size(); i++) {
            int x = poolX(i);
            int y = poolY();
            String id = pool.get(i);
            boolean hover = inside(mouseX, mouseY, x, y);
            drawCell(graphics, x, y, id, hover);
            if (hover) {
                hovered = id;
            }
        }

        if (!hovered.isEmpty()) {
            graphics.drawCenteredString(font, SpiritLoadout.name(hovered), width / 2, top + 112, 0xFFFFFF);
        }

        if (!held.isEmpty()) {
            SpiritIcons.draw(graphics, held, mouseX - 8, mouseY - 8);
        }
    }

    private Component slotLabel(int index) {
        return switch (bar) {
            case AUX -> Component.literal(String.valueOf(index + 1));
            case MAIN -> SpiritKeyMappings.MAIN[index].getTranslatedKeyMessage();
            case PASSIVE -> Component.literal(String.valueOf(index + 1));
        };
    }

    private void drawCell(GuiGraphics graphics, int x, int y, String id, boolean hover) {
        graphics.fill(x, y, x + CELL, y + CELL, 0xB0101018);
        graphics.renderOutline(x, y, CELL, CELL, hover ? 0xFFFFFFFF : 0xFF8888AA);
        if (!id.isEmpty()) {
            SpiritIcons.draw(graphics, id, x + (CELL - 16) / 2, y + (CELL - 16) / 2);
            if (!owned(id)) {
                graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xA0000000);
            }
        }
    }
}
