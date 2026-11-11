package com.spiritform;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Редактор однієї з трьох панелей здібностей (пасивні, активні, основні), вбудований у головне вікно прокачки.
 * Клацни здібність у нижньому ряду або в клітинці, щоб узяти її, потім клацни клітинку, щоб покласти
 * (якщо там щось є, вони міняються місцями). Права кнопка очищає клітинку.
 * Редактор нічого не знає про вікно: хазяїн задає йому місце через {@link #layout} і передає клацання.
 */
final class SpiritLoadoutEditor {

    static final int CELL = 21;
    static final int GAP = 2;
    /** Скільки здібностей показано в нижньому ряду; решта на інших сторінках. */
    static final int POOL_PAGE = 6;

    private final SpiritScreenBase host;

    private SpiritLoadout.Bar bar;
    private List<String> pool = List.of();
    /** Здібність, яку тримає курсор. Порожній рядок означає, що нічого. */
    private String held = "";
    private int poolPage;
    /** Відкрита сторінка колеса (для цифр і основних клавіш завжди 0). */
    private int slotPage;

    private int x;
    private int y;
    private int w;

    SpiritLoadoutEditor(SpiritScreenBase host) {
        this.host = host;
    }

    // ---------- стан ----------

    boolean hasBar() {
        return bar != null;
    }

    void setBar(SpiritLoadout.Bar value) {
        if (value == bar) {
            return;
        }
        bar = value;
        SpiritLoadout.syncExternal(Station.clientBlocks());
        pool = SpiritLoadout.candidates(value);
        poolPage = 0;
        slotPage = 0;
        held = "";
    }

    void drop() {
        held = "";
    }

    /** Місце редактора: x, y лівого верхнього кута ряду клітинок панелі та ширина області. */
    void layout(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;
    }

    int pages() {
        return Math.max(1, (pool.size() + POOL_PAGE - 1) / POOL_PAGE);
    }

    int page() {
        return poolPage;
    }

    void setPage(int value) {
        poolPage = Math.max(0, Math.min(pages() - 1, value));
    }

    // ---------- сторінки колеса ----------

    /** Колесо здібностей: сторінки можна додавати й видаляти. */
    boolean isWheel() {
        return bar == SpiritLoadout.Bar.PASSIVE;
    }

    /** Чи має панель сторінки (колесо здібностей і колесо станцій). */
    boolean hasSlotPages() {
        return bar == SpiritLoadout.Bar.PASSIVE || bar == SpiritLoadout.Bar.STATION;
    }

    int slotPages() {
        return isWheel() ? SpiritLoadout.passivePages() : bar == SpiritLoadout.Bar.STATION ? Station.pages() : 1;
    }

    int slotPage() {
        return Math.min(slotPage, slotPages() - 1);
    }

    void setSlotPage(int value) {
        slotPage = Math.max(0, Math.min(slotPages() - 1, value));
        held = "";
    }

    /** Додає порожню сторінку й переходить на неї. */
    void addSlotPage() {
        int index = SpiritLoadout.addPassivePage();
        if (index >= 0) {
            slotPage = index;
        }
        held = "";
    }

    /** Видаляє відкриту сторінку; її здібності повертаються до нижнього ряду. */
    void removeSlotPage() {
        SpiritLoadout.removePassivePage(slotPage());
        slotPage = Math.min(slotPage, slotPages() - 1);
        held = "";
    }

    boolean canAddPage() {
        return slotPages() < SpiritLoadout.MAX_PAGES;
    }

    boolean canRemovePage() {
        return slotPages() > 1;
    }

    /** Кнопки сторінок стоять біля ряду колеса; панель ще може бути не вибрана, тому розмір беремо з PASSIVE. */
    private int rowCells() {
        return bar == null ? SpiritLoadout.Bar.PASSIVE.size : bar.size;
    }

    private int wheelRowX() {
        return x + (w - rowWidth(rowCells())) / 2;
    }

    int slotPrevX() {
        return wheelRowX() - 24;
    }

    int slotNextX() {
        return wheelRowX() + rowWidth(rowCells()) + 4;
    }

    int slotRemoveX() {
        return slotPrevX() - 22;
    }

    int slotAddX() {
        return slotNextX() + 22;
    }

    int slotButtonY() {
        return slotY() + (CELL - 20) / 2;
    }

    // ---------- геометрія ----------

    private static int rowWidth(int count) {
        return count * CELL + (count - 1) * GAP;
    }

    private int slotX(int index) {
        return x + (w - rowWidth(bar.size)) / 2 + index * (CELL + GAP);
    }

    private int slotY() {
        return y;
    }

    private int poolX(int index) {
        return x + (w - rowWidth(POOL_PAGE)) / 2 + index * (CELL + GAP);
    }

    private int poolY() {
        return y + CELL + 40;
    }

    int prevButtonX() {
        return poolX(0) - 24;
    }

    int nextButtonX() {
        return poolX(POOL_PAGE - 1) + CELL + 4;
    }

    int buttonY() {
        return poolY() + (CELL - 20) / 2;
    }

    private int poolFirst() {
        return poolPage * POOL_PAGE;
    }

    private int poolCountOnPage() {
        return Math.max(0, Math.min(POOL_PAGE, pool.size() - poolFirst()));
    }

    private static boolean inside(double mx, double my, int cx, int cy) {
        return mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL;
    }

    /** Чи відкрита здібність (інакше її не можна взяти з нижнього ряду). */
    private boolean owned(String id) {
        if (Station.byLoadoutId(id) != null) {
            return true; // станції можна розставляти, навіть поки вони не куплені
        }
        if (SpiritLoadout.INSTINCT.equals(id)) {
            return host.data().unlocked();
        }
        SpiritAbility ability = SpiritAbility.byId(id);
        return ability != null && host.data().hasAbility(ability);
    }

    // ---------- клацання ----------

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bar == null) {
            return false;
        }

        for (int i = 0; i < bar.size; i++) {
            if (!inside(mouseX, mouseY, slotX(i), slotY())) {
                continue;
            }
            int page = slotPage();
            String current = SpiritLoadout.get(bar, page, i);

            if (button == 1) {
                SpiritLoadout.set(bar, page, i, "");
                return true;
            }
            if (button == 0) {
                if (held.isEmpty()) {
                    held = current; // взяти здібність із клітинки
                    if (!current.isEmpty()) {
                        SpiritLoadout.set(bar, page, i, "");
                    }
                } else if (SpiritLoadout.allowed(bar, held)) {
                    SpiritLoadout.set(bar, page, i, held);
                    held = current; // обмін
                    if (!held.isEmpty() && !SpiritLoadout.allowed(bar, held)) {
                        held = "";
                    }
                }
                return true;
            }
        }

        for (int i = 0; i < poolCountOnPage(); i++) {
            if (!inside(mouseX, mouseY, poolX(i), poolY()) || button != 0) {
                continue;
            }
            String id = pool.get(poolFirst() + i);
            if (!held.isEmpty()) {
                held = ""; // покласти назад: здібність просто лишається поза панеллю
            } else if (owned(id)) {
                held = id;
            }
            return true;
        }
        return false;
    }

    // ---------- малювання ----------

    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (bar == null) {
            return;
        }
        net.minecraft.client.gui.Font font = host.textFont();
        String hovered = "";

        for (int i = 0; i < bar.size; i++) {
            int cx = slotX(i);
            int cy = slotY();
            String id = SpiritLoadout.get(bar, slotPage(), i);
            boolean hover = inside(mouseX, mouseY, cx, cy);
            drawCell(graphics, cx, cy, id, hover);
            SpiritUi.centered(graphics, font, slotLabel(i), cx + CELL / 2, cy + CELL + 3, SpiritUi.TEXT_DIM);
            if (hover && !id.isEmpty()) {
                hovered = id;
            }
        }

        if (hasSlotPages()) {
            SpiritUi.centered(graphics, font,
                    Component.translatable("screen.spirit_form.loadout.page", slotPage() + 1, slotPages()),
                    slotPrevX() + 10, slotY() + CELL + 3, SpiritUi.TEXT_DIM);
        }

        SpiritUi.centered(graphics, font, Component.translatable("screen.spirit_form.loadout.pool"),
                x + w / 2, poolY() - 14, SpiritUi.TEXT);
        if (pages() > 1) {
            SpiritUi.centered(graphics, font, Component.literal((poolPage + 1) + "/" + pages()),
                    x + w / 2, poolY() + CELL + 4, SpiritUi.TEXT_DIM);
        }
        for (int i = 0; i < poolCountOnPage(); i++) {
            int cx = poolX(i);
            int cy = poolY();
            String id = pool.get(poolFirst() + i);
            boolean hover = inside(mouseX, mouseY, cx, cy);
            drawCell(graphics, cx, cy, id, hover);
            if (hover) {
                hovered = id;
            }
        }

        if (!hovered.isEmpty()) {
            boolean ok = SpiritLoadout.allowed(bar, hovered);
            Component name = ok
                    ? SpiritLoadout.name(hovered)
                    : SpiritLoadout.name(hovered).copy().append(" - ")
                            .append(Component.translatable("screen.spirit_form.loadout.no_wheel"));
            SpiritUi.centered(graphics, font, name, x + w / 2, slotY() + CELL + 15, ok ? SpiritUi.ACCENT : SpiritUi.TEXT_BAD);
        }
    }

    /** Іконка, яку тримає курсор, малюється окремо, поверх усього. */
    void renderHeld(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!held.isEmpty()) {
            SpiritIcons.draw(graphics, held, mouseX - 8, mouseY - 8);
        }
    }

    private Component slotLabel(int index) {
        return switch (bar) {
            case AUX, PASSIVE, STATION -> Component.literal(String.valueOf(index + 1));
            case MAIN -> SpiritKeyMappings.MAIN[index].getTranslatedKeyMessage();
        };
    }

    private void drawCell(GuiGraphics graphics, int cx, int cy, String id, boolean hover) {
        SpiritUi.inset(graphics, cx, cy, CELL, CELL);
        if (!id.isEmpty()) {
            SpiritIcons.draw(graphics, id, cx + (CELL - 16) / 2, cy + (CELL - 16) / 2);
            if (!owned(id)) {
                graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0xA0000000);
            } else if (bar != null && !SpiritLoadout.allowed(bar, id)) {
                // Цю здібність не можна покласти на поточну панель
                graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x90600000);
            }
        }
        if (hover) {
            graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x80FFFFFF);
        }
    }
}
