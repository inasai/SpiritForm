package com.spiritform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * Єдине вікно прокачки (клавіша H): зліва дерево розділів, справа подробиці вибраного.
 * <p>
 * Розділи (Форма Привида, Шлях Голоду, Шлях Руху, Шлях Темряви та Розташування) випадають: клац розгортає розділ
 * і показує його вміст, повторний клац згортає. Коли список не вміщується, справа від нього є повзунок прокрутки.
 * Біля розділу «Розташування» вкладено три панелі (пасивні, активні, основні), редактор вибраної видно справа.
 * Поки форма закрита, у вікні лише кнопка відкриття.
 */
public class SpiritUpgradeScreen extends SpiritScreenBase {

    private static final int W = 380;
    private static final int H = 240;
    private static final int LIST_W = 118;
    private static final int ROW_H = 13;
    private static final int ROW_STEP = 14;
    private static final int HEAD_H = 15;
    private static final int DIV_H = 12;

    private static final int COLOR_HUNGER = 0xFFE05050;
    private static final int COLOR_MOVEMENT = 0xFF40C8E8;
    private static final int COLOR_DARKNESS = 0xFFA070E0;
    private static final int COLOR_COMMON = 0xFF8CFFC8;

    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    private enum Kind { FORM, FORMLEVEL, PATH, PLACEMENT, LEVEL, ABILITY, SHARDS, ARMY, INSTINCT, SOULS, STORAGE, VAULT_ART, VAULT_GEAR, EXTERNAL, BAR, HEADER, DIVIDER }

    private record Row(Kind kind, SpiritBranch branch, SpiritAbility ability, SpiritLoadout.Bar bar, Component label, int color) {
        boolean selectable() {
            return kind != Kind.HEADER && kind != Kind.DIVIDER;
        }

        /** Рядок розділу, що згортається й розгортається. */
        boolean section() {
            return kind == Kind.FORM || kind == Kind.PATH || kind == Kind.PLACEMENT || kind == Kind.STORAGE;
        }

        int height() {
            return switch (kind) {
                case HEADER -> HEAD_H;
                case DIVIDER -> DIV_H;
                default -> ROW_STEP;
            };
        }
    }

    /** Які розділи розгорнуто (ключі: form, placement, id шляху); запам'ятовується між відкриттями вікна. */
    private static final Set<String> OPEN = new HashSet<>();
    private static int selected;

    private boolean builtUnlocked;
    private Button unlockButton;
    private Button primary;
    private IconButton resetButton;
    private Button prevPool;
    private Button nextPool;
    private Button pagePrev;
    private Button pageNext;
    private Button pageAdd;
    private Button pageRemove;
    private final SpiritLoadoutEditor editor = new SpiritLoadoutEditor(this);
    private final SpiritVaultPanel vault = new SpiritVaultPanel();
    private final SpiritExternalPanel external = new SpiritExternalPanel();

    private int scroll;
    private boolean draggingBar;
    private int dragOffset;
    private boolean revealSelected = true;

    public SpiritUpgradeScreen() {
        super(Component.translatable("screen.spirit_form.title"), null);
    }

    @Override
    protected int panelWidth() {
        return W;
    }

    @Override
    protected int panelHeight() {
        return H;
    }

    @Override
    protected boolean showSettings() {
        return data().unlocked();
    }

    // ---------- рядки списку ----------

    private static int branchColor(SpiritBranch branch) {
        return switch (branch) {
            case HUNGER -> COLOR_HUNGER;
            case MOVEMENT -> COLOR_MOVEMENT;
            case DARKNESS -> COLOR_DARKNESS;
        };
    }

    /** Дія перемикання пасивної здібності або -1, якщо здібність не пасивна. */
    private static int toggleAction(SpiritAbility ability) {
        return switch (ability) {
            case ICY_TOUCH -> UpgradeSpiritPayload.TOGGLE_ICY;
            case PRESENCE -> UpgradeSpiritPayload.TOGGLE_PRESENCE;
            case GAZE -> UpgradeSpiritPayload.TOGGLE_GAZE;
            case ECHO -> UpgradeSpiritPayload.TOGGLE_ECHO;
            default -> -1;
        };
    }

    private static boolean toggledOn(SpiritData data, SpiritAbility ability) {
        return switch (ability) {
            case ICY_TOUCH -> data.icy();
            case PRESENCE -> data.presence();
            case GAZE -> data.gaze();
            case ECHO -> data.echo();
            default -> false;
        };
    }

    private static Row header(Component label, int color) {
        return new Row(Kind.HEADER, null, null, null, label, color);
    }

    private static Row abilityRow(SpiritAbility ability) {
        int color = ability.branch == null ? COLOR_COMMON : branchColor(ability.branch);
        return new Row(Kind.ABILITY, ability.branch, ability, null, ability.displayName(), color);
    }

    private static boolean isOpen(String key) {
        return OPEN.contains(key);
    }

    private static String sectionKey(Row row) {
        return switch (row.kind) {
            case FORM -> "form";
            case PLACEMENT -> "placement";
            case STORAGE -> "storage";
            default -> row.branch.id;
        };
    }

    /**
     * Рядки списку. Їх будувати щокадру задорого (переклади, сортування здібностей із читанням конфігу),
     * тому результат живе до кінця тіку, а розгортання розділу чи повторне відкриття вікна скидає його.
     */
    private List<Row> rows() {
        if (rowsCache == null || rowsStamp != tickCounter) {
            rowsCache = buildRows();
            rowsStamp = tickCounter;
        }
        return rowsCache;
    }

    private List<Row> rowsCache;
    private long rowsStamp = -1L;
    private long tickCounter;

    @Override
    public void tick() {
        tickCounter++;
        super.tick();
    }

    private List<Row> buildRows() {
        List<Row> list = new ArrayList<>();
        SpiritData data = data();

        list.add(new Row(Kind.FORM, null, null, null,
                Component.translatable("screen.spirit_form.form"), COLOR_COMMON));
        if (isOpen("form")) {
            list.add(new Row(Kind.FORMLEVEL, null, null, null,
                    Component.translatable("screen.spirit_form.formlevel.name"), COLOR_COMMON));
            list.add(new Row(Kind.INSTINCT, null, null, null,
                    Component.translatable("passive.spirit_form.instinct"), COLOR_COMMON));
            list.add(new Row(Kind.SOULS, null, null, null,
                    Component.translatable("screen.spirit_form.souls.name"), COLOR_COMMON));
            for (SpiritAbility ability : SpiritAbility.sorted()) {
                if (ability.branch == null) {
                    list.add(abilityRow(ability));
                }
            }
            list.add(new Row(Kind.SHARDS, null, null, null,
                    Component.translatable("screen.spirit_form.shards.name"), COLOR_COMMON));
            list.add(new Row(Kind.ARMY, null, null, null,
                    Component.translatable("screen.spirit_form.army_slots.name"), COLOR_COMMON));
        }

        for (SpiritBranch branch : SpiritBranch.values()) {
            int color = branchColor(branch);
            list.add(new Row(Kind.PATH, branch, null, null,
                    Component.translatable("path.spirit_form." + branch.id), color));
            if (isOpen(branch.id)) {
                list.add(new Row(Kind.LEVEL, branch, null, null,
                        Component.translatable("screen.spirit_form.level.chip", data.level(branch), SpiritBalance.MAX_LEVEL),
                        color));
                for (SpiritAbility ability : SpiritAbility.sorted()) {
                    if (ability.branch == branch) {
                        list.add(abilityRow(ability));
                    }
                }
            }
        }

        list.add(new Row(Kind.PLACEMENT, null, null, null,
                Component.translatable("screen.spirit_form.placement"), COLOR_COMMON));
        if (isOpen("placement")) {
            for (SpiritLoadout.Bar bar : new SpiritLoadout.Bar[] {
                    SpiritLoadout.Bar.PASSIVE, SpiritLoadout.Bar.AUX, SpiritLoadout.Bar.MAIN, SpiritLoadout.Bar.STATION}) {
                if (bar == SpiritLoadout.Bar.STATION && !data.hasAbility(SpiritAbility.SELF_SUFFICIENCY)) {
                    continue;
                }
                list.add(new Row(Kind.BAR, null, null, bar,
                        Component.translatable("button.spirit_form.bar." + bar.key()), COLOR_COMMON));
            }
        }

        list.add(new Row(Kind.STORAGE, null, null, null,
                Component.translatable("screen.spirit_form.storage"), COLOR_COMMON));
        if (isOpen("storage")) {
            list.add(new Row(Kind.VAULT_ART, null, null, null,
                    Component.translatable("screen.spirit_form.vault.artifacts"), COLOR_COMMON));
            list.add(new Row(Kind.VAULT_GEAR, null, null, null,
                    Component.translatable("screen.spirit_form.vault.gear"), COLOR_COMMON));
            if (data.hasAbility(SpiritAbility.SELF_SUFFICIENCY)) {
                list.add(new Row(Kind.EXTERNAL, null, null, null,
                        Component.translatable("screen.spirit_form.external"), COLOR_COMMON));
            }
        }
        return list;
    }

    private static int firstSelectable(List<Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).selectable()) {
                return i;
            }
        }
        return 0;
    }

    /** Рядок, що відповідає вибраному, з виправленням номера, якщо список змінився. */
    private Row current(List<Row> rows) {
        if (selected < 0 || selected >= rows.size() || !rows.get(selected).selectable()) {
            selected = firstSelectable(rows);
        }
        return rows.get(selected);
    }

    private int rowY(List<Row> rows, int index) {
        int y = top + 33 - scroll;
        for (int i = 0; i < index; i++) {
            y += rows.get(i).height();
        }
        return y;
    }

    // ---------- прокрутка ----------

    private int viewTop() {
        return top + 31;
    }

    private int viewBottom() {
        return top + H - 37;
    }

    private int viewH() {
        return viewBottom() - viewTop();
    }

    private int contentH(List<Row> rows) {
        int h = 4;
        for (Row row : rows) {
            h += row.height();
        }
        return h;
    }

    private int maxScroll(List<Row> rows) {
        return Math.max(0, contentH(rows) - viewH());
    }

    private boolean scrollable(List<Row> rows) {
        return maxScroll(rows) > 0;
    }

    private int rowW(List<Row> rows) {
        return scrollable(rows) ? LIST_W - 8 : LIST_W;
    }

    private void clampScroll(List<Row> rows) {
        scroll = Math.max(0, Math.min(maxScroll(rows), scroll));
    }

    /** Прокручує список так, щоб вибраний рядок було видно. */
    private void reveal(List<Row> rows, int index) {
        int rel = 2;
        for (int i = 0; i < index && i < rows.size(); i++) {
            rel += rows.get(i).height();
        }
        if (rel < scroll) {
            scroll = rel - 2;
        } else if (rel + ROW_H > scroll + viewH()) {
            scroll = rel + ROW_H - viewH() + 2;
        }
        clampScroll(rows);
    }

    private int trackX() {
        return left + 8 + LIST_W - 6;
    }

    private int thumbH(List<Row> rows) {
        return Math.max(14, viewH() * viewH() / Math.max(1, contentH(rows)));
    }

    private int thumbY(List<Row> rows) {
        int max = maxScroll(rows);
        return viewTop() + (max == 0 ? 0 : (viewH() - thumbH(rows)) * scroll / max);
    }

    private boolean inList(double mouseX, double mouseY) {
        return mouseX >= left + 5 && mouseX < left + 8 + LIST_W + 3 && mouseY >= viewTop() && mouseY < viewBottom();
    }

    // ---------- побудова ----------

    private int detailsX() {
        return left + LIST_W + 20;
    }

    private int detailsW() {
        return W - LIST_W - 20 - 10;
    }

    @Override
    protected void buildContent() {
        rowsCache = null;
        LEVEL_INFO.clear();
        builtUnlocked = data().unlocked();
        unlockButton = null;
        primary = null;
        resetButton = null;
        prevPool = null;
        nextPool = null;

        if (!builtUnlocked) {
            unlockButton = addRenderableWidget(
                    SpiritButton.of(Component.empty(), b -> send(UpgradeSpiritPayload.UNLOCK))
                            .bounds(left + (W - 180) / 2, top + 130, 180, 20)
                            .primary()
                            .build()
            );
            return;
        }

        primary = addRenderableWidget(
                SpiritButton.of(Component.empty(), b -> activate())
                        .bounds(left + W - 8 - 130, top + H - 30, 130, 20)
                        .primary()
                        .build()
        );
        resetButton = addRenderableWidget(new IconButton(
                left + W - 8 - 130 - 24, top + H - 30, 20, 20,
                IconButton.Icon.RESET,
                b -> resetCurrent(),
                Component.translatable("button.spirit_form.reset_tooltip", 0)
        ));

        // Редактор панелей: ряд клітинок на 100 пікселів нижче заголовка, кнопки "<" і ">" по боках нижнього ряду
        editor.layout(detailsX(), top + 88, detailsW());
        prevPool = addRenderableWidget(
                SpiritButton.of(Component.literal("<"), b -> editor.setPage(editor.page() - 1))
                        .bounds(editor.prevButtonX(), editor.buttonY(), 20, 20)
                        .build());
        nextPool = addRenderableWidget(
                SpiritButton.of(Component.literal(">"), b -> editor.setPage(editor.page() + 1))
                        .bounds(editor.nextButtonX(), editor.buttonY(), 20, 20)
                        .build());

        // Сторінки колеса: "-" видалити, "<" ">" перегорнути, "+" додати
        pageRemove = addRenderableWidget(
                SpiritButton.of(Component.literal("-").withStyle(net.minecraft.ChatFormatting.RED), b -> editor.removeSlotPage())
                        .bounds(editor.slotRemoveX(), editor.slotButtonY(), 20, 20)
                        .build());
        pagePrev = addRenderableWidget(
                SpiritButton.of(Component.literal("<"), b -> editor.setSlotPage(editor.slotPage() - 1))
                        .bounds(editor.slotPrevX(), editor.slotButtonY(), 20, 20)
                        .build());
        pageNext = addRenderableWidget(
                SpiritButton.of(Component.literal(">"), b -> editor.setSlotPage(editor.slotPage() + 1))
                        .bounds(editor.slotNextX(), editor.slotButtonY(), 20, 20)
                        .build());
        pageAdd = addRenderableWidget(
                SpiritButton.of(Component.literal("+").withStyle(net.minecraft.ChatFormatting.GREEN), b -> editor.addSlotPage())
                        .bounds(editor.slotAddX(), editor.slotButtonY(), 20, 20)
                        .build());
    }

    // ---------- дії ----------

    private void toggleSection(int index) {
        List<Row> rows = rows();
        if (index < 0 || index >= rows.size() || !rows.get(index).section()) {
            return;
        }
        String key = sectionKey(rows.get(index));
        if (!OPEN.remove(key)) {
            OPEN.add(key);
        }
        rowsCache = null;
        selected = index;
        editor.drop();
        vault.drop();
        external.drop();
        click();
        refresh();
    }

    private void activate() {
        List<Row> rows = rows();
        Row row = current(rows);
        switch (row.kind) {
            case FORM, PATH, PLACEMENT, STORAGE -> toggleSection(selected);
            case INSTINCT -> send(UpgradeSpiritPayload.TOGGLE_INSTINCT);
            case SOULS -> send(UpgradeSpiritPayload.TOGGLE_SOULS);
            case FORMLEVEL -> send(data().fullForm() || data().formLevel() >= SpiritData.FORM_MAX
                    ? UpgradeSpiritPayload.TOGGLE_INVULNERABLE : UpgradeSpiritPayload.UPGRADE_FORM);
            case LEVEL -> send(UpgradeSpiritPayload.actionFor(row.branch));
            case ABILITY -> {
                if (!data().hasAbility(row.ability)) {
                    send(UpgradeSpiritPayload.abilityActionFor(row.ability));
                } else if (toggleAction(row.ability) >= 0) {
                    send(toggleAction(row.ability));
                }
            }
            case SHARDS -> send(UpgradeSpiritPayload.BUY_SHARD);
            case ARMY -> send(UpgradeSpiritPayload.BUY_ARMY_SLOT);
            default -> { }
        }
    }

    private void resetCurrent() {
        Row row = current(rows());
        if (row.kind == Kind.FORMLEVEL) {
            send(UpgradeSpiritPayload.RESET_FORM);
        } else if (row.kind == Kind.LEVEL) {
            send(UpgradeSpiritPayload.resetActionFor(row.branch));
        } else if (row.kind == Kind.ABILITY && data().hasAbility(row.ability)) {
            send(UpgradeSpiritPayload.resetAbilityActionFor(row.ability));
        }
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void select(int index) {
        List<Row> rows = rows();
        if (index < 0 || index >= rows.size() || !rows.get(index).selectable() || index == selected) {
            return;
        }
        selected = index;
        reveal(rows, index);
        editor.drop();
        vault.drop();
        external.drop();
        click();
        refresh();
    }

    /** Наступний або попередній вибираний рядок (перескакує через заголовки). */
    private void step(int delta) {
        List<Row> rows = rows();
        int index = selected;
        do {
            index += delta;
        } while (index >= 0 && index < rows.size() && !rows.get(index).selectable());
        select(index);
    }

    // ---------- оновлення кнопок ----------

    @Override
    protected void refresh() {
        if (data().unlocked() != builtUnlocked) {
            rebuildWidgets();
            return;
        }

        LocalPlayer player = minecraft == null ? null : minecraft.player;

        if (unlockButton != null) {
            int cost = SpiritBalance.cost(1);
            unlockButton.setMessage(Component.translatable("button.spirit_form.unlock", cost));
            unlockButton.active = player != null && canAfford(player, cost);
            return;
        }
        if (primary == null || player == null) {
            return;
        }

        SpiritData data = data();
        List<Row> rows = rows();
        Row row = current(rows);

        resetButton.visible = false;
        primary.visible = true;
        primary.active = true;

        if (row.kind == Kind.BAR) {
            editor.setBar(row.bar);
        }
        boolean showPool = row.kind == Kind.BAR && editor.hasBar();
        prevPool.visible = showPool && editor.pages() > 1;
        nextPool.visible = prevPool.visible;
        prevPool.active = editor.page() > 0;
        nextPool.active = editor.page() < editor.pages() - 1;

        boolean paged = row.kind == Kind.BAR && editor.hasBar() && editor.hasSlotPages();
        boolean wheel = paged && editor.isWheel();
        pagePrev.visible = paged;
        pageNext.visible = paged;
        pageAdd.visible = wheel;
        pageRemove.visible = wheel;
        if (paged) {
            // Ряд станцій вужчий за ряд колеса, тож кнопки стають біля нього
            pagePrev.setX(editor.slotPrevX());
            pageNext.setX(editor.slotNextX());
            pageRemove.setX(editor.slotRemoveX());
            pageAdd.setX(editor.slotAddX());
            pagePrev.active = editor.slotPage() > 0;
            pageNext.active = editor.slotPage() < editor.slotPages() - 1;
            pageAdd.active = editor.canAddPage();
            pageRemove.active = editor.canRemovePage();
        }

        switch (row.kind) {
            case LEVEL -> {
                int level = data.level(row.branch);
                if (level >= SpiritBalance.MAX_LEVEL) {
                    primary.setMessage(Component.translatable("button.spirit_form.max"));
                    primary.active = false;
                } else {
                    int cost = SpiritBalance.cost(level + 1);
                    primary.setMessage(Component.translatable("button.spirit_form.levelup", cost));
                    primary.active = canAfford(player, cost);
                }
                resetButton.visible = true;
                resetButton.active = level > 1;
                resetButton.setTooltipText(Component.translatable("button.spirit_form.reset"));
            }
            case ABILITY -> {
                SpiritAbility ability = row.ability;
                resetButton.visible = true;
                resetButton.active = data.hasAbility(ability);
                resetButton.setTooltipText(Component.translatable("button.spirit_form.reset"));
                if (data.hasAbility(ability)) {
                    if (toggleAction(ability) >= 0) {
                        primary.setMessage(Component.translatable(
                                toggledOn(data, ability) ? "button.spirit_form.toggle.on" : "button.spirit_form.toggle.off"));
                    } else {
                        primary.setMessage(Component.translatable("button.spirit_form.opened"));
                        primary.active = false;
                    }
                } else if (!ability.isAvailable(data)) {
                    primary.setMessage(Component.translatable("button.spirit_form.locked"));
                    primary.active = false;
                } else {
                    primary.setMessage(Component.translatable("button.spirit_form.ability_unlock", ability.cost()));
                    primary.active = canAfford(player, ability.cost());
                }
            }
            case SHARDS -> {
                int owned = data.shards();
                if (owned >= Config.SOUL_SHARD_LIMIT.get()) {
                    primary.setMessage(Component.translatable("button.spirit_form.shard_max"));
                    primary.active = false;
                } else {
                    primary.setMessage(Component.translatable("button.spirit_form.shard_buy", owned + 1));
                    primary.active = canAfford(player, owned + 1);
                }
            }
            case ARMY -> {
                int slots = data.armySlots();
                if (!data.hasAbility(SpiritAbility.ARMY)) {
                    primary.setMessage(Component.translatable("button.spirit_form.locked"));
                    primary.active = false;
                } else if (slots >= Config.ARMY_MAX_SLOTS.get()) {
                    primary.setMessage(Component.translatable("button.spirit_form.army_slot_max"));
                    primary.active = false;
                } else {
                    primary.setMessage(Component.translatable("button.spirit_form.army_slot_buy", slots));
                    primary.active = canAfford(player, slots);
                }
            }
            case FORMLEVEL -> {
                int level = Math.max(1, data.formLevel());
                if (data.fullForm() || level >= SpiritData.FORM_MAX) {
                    primary.setMessage(Component.translatable(
                            data.invulnerable() ? "button.spirit_form.toggle.on" : "button.spirit_form.toggle.off"));
                    primary.active = data.unlocked();
                } else {
                    int cost = SpiritBalance.formCost(level + 1);
                    primary.setMessage(Component.translatable("button.spirit_form.levelup", cost));
                    primary.active = canAfford(player, cost);
                }
                resetButton.visible = true;
                resetButton.active = !data.fullForm() && level > 1;
                resetButton.setTooltipText(Component.translatable("button.spirit_form.reset"));
            }
            case INSTINCT -> primary.setMessage(Component.translatable(
                    data.instinct() ? "button.spirit_form.toggle.on" : "button.spirit_form.toggle.off"));
            case SOULS -> {
                if (!data.hasAbility(SpiritAbility.ARMY)) {
                    primary.setMessage(Component.translatable("button.spirit_form.locked"));
                    primary.active = false;
                } else {
                    primary.setMessage(Component.translatable(
                            data.soulDrops() ? "button.spirit_form.toggle.on" : "button.spirit_form.toggle.off"));
                }
            }
            case BAR, FORM, PATH, PLACEMENT, STORAGE, VAULT_ART, VAULT_GEAR, EXTERNAL -> primary.visible = false;
            default -> primary.setMessage(Component.translatable("button.spirit_form.open"));
        }
    }

    // ---------- керування ----------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (busy()) {
            return true;
        }
        if (builtUnlocked && button == 0) {
            List<Row> rows = rows();
            clampScroll(rows);
            if (scrollable(rows) && mouseX >= trackX() && mouseX < trackX() + 5 && inList(mouseX, mouseY)) {
                int ty = thumbY(rows);
                int th = thumbH(rows);
                if (mouseY >= ty && mouseY < ty + th) {
                    dragOffset = (int) mouseY - ty;
                } else {
                    dragOffset = th / 2;
                    dragTo(rows, mouseY);
                }
                draggingBar = true;
                return true;
            }
            int x = left + 8;
            int rw = rowW(rows);
            if (inList(mouseX, mouseY)) {
                for (int i = 0; i < rows.size(); i++) {
                    Row row = rows.get(i);
                    int y = rowY(rows, i);
                    int rx = row.section() ? x : x + 6;
                    int w = row.section() ? rw : rw - 6;
                    if (row.selectable() && mouseX >= rx && mouseX < rx + w && mouseY >= y && mouseY < y + ROW_H) {
                        if (row.section()) {
                            toggleSection(i);
                        } else {
                            select(i);
                        }
                        return true;
                    }
                }
            }
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (builtUnlocked && current(rows()).kind == Kind.BAR) {
            return editor.mouseClicked(mouseX, mouseY, button);
        }
        if (builtUnlocked && (current(rows()).kind == Kind.VAULT_ART || current(rows()).kind == Kind.VAULT_GEAR)) {
            return vault.mouseClicked(mouseX, mouseY, button);
        }
        if (builtUnlocked && current(rows()).kind == Kind.EXTERNAL) {
            return external.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    private void dragTo(List<Row> rows, double mouseY) {
        int track = viewH() - thumbH(rows);
        if (track > 0) {
            double fraction = (mouseY - dragOffset - viewTop()) / track;
            scroll = (int) Math.round(fraction * maxScroll(rows));
            clampScroll(rows);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingBar) {
            dragTo(rows(), mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (builtUnlocked && inList(mouseX, mouseY)) {
            List<Row> rows = rows();
            scroll -= (int) Math.round(scrollY * ROW_STEP * 2);
            clampScroll(rows);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (builtUnlocked && !anim.closing()) {
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                step(1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                step(-1);
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && current(rows()).section()) {
                toggleSection(selected);
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && primary != null
                    && primary.visible && primary.active) {
                activate();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---------- малювання ----------

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        SpiritData data = data();

        if (!data.unlocked()) {
            SpiritUi.centered(graphics, font, Component.translatable("screen.spirit_form.base"), width / 2, top + 90, SpiritUi.TEXT);
            SpiritUi.centered(graphics, font, Component.translatable("screen.spirit_form.base.closed"), width / 2, top + 104, SpiritUi.TEXT_DIM);
            return;
        }

        List<Row> rows = rows();
        Row row = current(rows);

        renderList(graphics, mouseX, mouseY, data, rows);
        graphics.enableScissor(detailsX() - 4, top + 28, left + W - 4, top + H - 38);
        renderDetails(graphics, mouseX, mouseY, data, row);
        graphics.disableScissor();
        if (row.kind == Kind.VAULT_ART || row.kind == Kind.VAULT_GEAR) {
            vault.renderTooltip(graphics, font, mouseX, mouseY);
        }
        if (row.kind == Kind.EXTERNAL) {
            external.renderTooltip(graphics, font, mouseX, mouseY);
        }
        if (row.kind == Kind.BAR) {
            editor.renderHeld(graphics, mouseX, mouseY);
        }
        renderFooter(graphics, row);

        // Шкала переслідування: вгорі посередині заголовка, як смуга боса
        if (SpiritHeraldClient.known()) {
            int shown = Math.max(0, Math.min(100, SpiritHeraldClient.meter()));
            boolean ready = SpiritHeraldClient.meter() >= SpiritHeralds.THRESHOLD;
            Component label = Component.translatable("hud.spirit_form.herald.title");
            int cx = left + W / 2;
            SpiritUi.centered(graphics, font, label, cx, top + 5, SpiritUi.TEXT);
            float scale = 0.55F;
            int barW = (int) (182 * scale);
            SpiritUi.bossBar(graphics, cx - barW / 2, top + 16, shown / 100.0F, ready, scale);
        }
    }

    private void renderFooter(GuiGraphics graphics, Row row) {
        SpiritUi.engraved(graphics, left + 8, top + H - 36, W - 16);
        int hx = left + 10;
        int hr = primary != null && primary.visible ? left + W - 8 - 130 - 28 : left + W - 8;
        Component hint = row.kind == Kind.BAR
                ? Component.translatable("screen.spirit_form.loadout.hint1").append(" ")
                        .append(Component.translatable("screen.spirit_form.loadout.hint2"))
                : row.kind == Kind.EXTERNAL
                        ? Component.translatable("screen.spirit_form.external.hint")
                : row.kind == Kind.VAULT_ART || row.kind == Kind.VAULT_GEAR
                        ? Component.translatable("screen.spirit_form.vault.hint")
                        : Component.translatable("screen.spirit_form.hint");
        SpiritUi.paragraph(graphics, font, hint, hx, top + H - 31, Math.max(60, hr - hx), SpiritUi.TEXT_DIM);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY, SpiritData data, List<Row> rows) {
        int x = left + 8;
        SpiritUi.inset(graphics, x - 3, top + 30, LIST_W + 6, H - 66);

        if (revealSelected) {
            revealSelected = false;
            reveal(rows, selected);
        }
        clampScroll(rows);
        int rw = rowW(rows);
        boolean mouseIn = inList(mouseX, mouseY);

        graphics.enableScissor(x - 3, viewTop(), x + LIST_W + 3, viewBottom());
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = rowY(rows, i);
            if (y + ROW_H < viewTop() || y > viewBottom()) {
                continue;
            }

            boolean section = row.section();
            int rx = section ? x : x + 6;
            int w = section ? rw : rw - 6;
            boolean hover = mouseIn && mouseX >= rx && mouseX < rx + w && mouseY >= y && mouseY < y + ROW_H;
            boolean isSelected = i == selected;

            graphics.blitSprite(isSelected || hover ? BUTTON_HIGHLIGHTED : BUTTON, rx, y, w, ROW_H);
            graphics.fill(rx + 3, y + 3, rx + 5, y + ROW_H - 3, row.color);

            int textColor = 0xFFFFFFFF;
            if (isSelected) {
                textColor = 0xFFFFFF55;
            } else if (row.kind == Kind.INSTINCT) {
                textColor = data.instinct() ? 0xFF6FE09A : 0xFFFFFFFF;
            } else if (row.kind == Kind.SOULS) {
                textColor = !data.hasAbility(SpiritAbility.ARMY) ? 0xFF8A8A96
                        : data.soulDrops() ? 0xFF6FE09A : 0xFFFFFFFF;
            } else if (row.kind == Kind.ABILITY) {
                textColor = data.hasAbility(row.ability) ? 0xFF6FE09A
                        : row.ability.isAvailable(data) ? 0xFFFFFFFF : 0xFF8A8A96;
            }
            graphics.drawString(font, row.label, rx + 10, y + 3, textColor, true);

            if (section) {
                String arrow = isOpen(sectionKey(row)) ? "v" : ">";
                graphics.drawString(font, arrow, rx + w - 5 - font.width(arrow), y + 3, 0xFFB0B0B0, false);
            }
        }
        graphics.disableScissor();

        if (scrollable(rows)) {
            int tx = trackX();
            SpiritUi.inset(graphics, tx, viewTop(), 5, viewH());
            int ty = thumbY(rows);
            int th = thumbH(rows);
            boolean hot = draggingBar || (mouseX >= tx && mouseX < tx + 5 && mouseY >= ty && mouseY < ty + th);
            graphics.fill(tx + 1, ty + 1, tx + 4, ty + th - 1, hot ? 0xFFE0E0E8 : 0xFF9A9AA6);
        }

        // Вертикальний розділювач між списком і подробицями
        SpiritUi.engravedV(graphics, x + LIST_W + 8, top + 32, H - 72);
    }

    private void renderDetails(GuiGraphics graphics, int mouseX, int mouseY, SpiritData data, Row row) {
        int x = detailsX();
        int w = detailsW();
        int y = top + 33;

        switch (row.kind) {
            case FORM, PATH, PLACEMENT -> renderRoot(graphics, data, x, y, w);
            case STORAGE -> {
                heading(graphics, row.label, row.color, null, x, y);
                SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.storage.desc"),
                        x, y + 22, w, SpiritUi.TEXT);
            }
            case VAULT_ART, VAULT_GEAR -> {
                int kind = row.kind == Kind.VAULT_ART ? SpiritVault.KIND_ARTIFACT : SpiritVault.KIND_GEAR;
                heading(graphics, row.label, row.color, null, x, y);
                vault.render(graphics, font, mouseX, mouseY, x, y + 20, w, kind);
                SpiritUi.paragraph(graphics, font,
                        Component.translatable(kind == SpiritVault.KIND_ARTIFACT
                                ? "screen.spirit_form.vault.artifacts.desc" : "screen.spirit_form.vault.gear.desc"),
                        x, vault.bottom() + 5, w, SpiritUi.TEXT_DIM);
            }
            case EXTERNAL -> {
                heading(graphics, row.label, row.color, null, x, y);
                external.render(graphics, font, mouseX, mouseY, x, y + 20, w);
                SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.external.desc"),
                        x, external.bottom() + 5, w, SpiritUi.TEXT_DIM);
            }
            case INSTINCT -> renderInstinct(graphics, data, x, y, w);
            case SOULS -> renderSouls(graphics, data, x, y, w);
            case FORMLEVEL -> renderFormLevel(graphics, data, x, y, w);
            case LEVEL -> renderPath(graphics, data, row.branch, row.color, x, y, w);
            case ABILITY -> renderAbility(graphics, data, row.ability, row.color, x, y, w);
            case SHARDS -> renderShards(graphics, data, x, y, w);
            case ARMY -> renderArmy(graphics, data, x, y, w);
            case BAR -> {
                heading(graphics, row.label, row.color, null, x, y);
                SpiritUi.paragraph(graphics, font,
                        Component.translatable("screen.spirit_form.bar." + row.bar.key() + ".desc"),
                        x, y + 19, w, SpiritUi.TEXT);
                editor.render(graphics, mouseX, mouseY);
            }
            default -> { }
        }
    }

    /** Назва великими літерами з кольоровим підкресленням; за потреби іконка зліва. */
    private void heading(GuiGraphics graphics, Component label, int color, String iconId, int x, int y) {
        int offset = 0;
        if (iconId != null) {
            SpiritIcons.draw(graphics, iconId, x, y - 2);
            offset = 20;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x + offset, y, 0);
        graphics.pose().scale(1.5F, 1.5F, 1.0F);
        graphics.drawString(font, label, 0, 0, SpiritUi.TEXT, false);
        graphics.pose().popPose();
        graphics.fill(x, y + 15, x + offset + (int) (font.width(label) * 1.5F), y + 17, color);
    }

    private void renderRoot(GuiGraphics graphics, SpiritData data, int x, int y, int w) {
        heading(graphics, Component.translatable("screen.spirit_form.title"), SpiritUi.ACCENT, null, x, y);
        int sy = y + 22;
        SpiritUi.section(graphics, font, x, sy, w, Component.translatable("screen.spirit_form.section.summary"));
        sy += 17;
        int segW = (w - 14) / SpiritBalance.MAX_LEVEL;
        for (SpiritBranch branch : SpiritBranch.values()) {
            int level = data.level(branch);
            Component name = Component.translatable("path.spirit_form." + branch.id);
            graphics.drawString(font, name, x, sy, SpiritUi.TEXT, false);
            String number = level + "/" + SpiritBalance.MAX_LEVEL;
            graphics.drawString(font, number, x + w - font.width(number), sy, SpiritUi.TEXT_DIM, false);
            int by = sy + 11;
            for (int i = 0; i < SpiritBalance.MAX_LEVEL; i++) {
                int sx = x + i * segW;
                SpiritUi.inset(graphics, sx, by, segW - 1, 5);
                if (i < level) {
                    graphics.fill(sx + 1, by + 1, sx + segW - 2, by + 4, branchColor(branch));
                }
            }
            sy += 24;
        }
        graphics.drawString(font,
                Component.translatable("screen.spirit_form.summary.shards", data.shards(), Config.SOUL_SHARD_LIMIT.get()),
                x, sy, SpiritUi.TEXT_OK, false);
        sy += 11;
        if (data.hasAbility(SpiritAbility.ARMY)) {
            graphics.drawString(font,
                    Component.translatable("screen.spirit_form.summary.army", data.armySlots(), Config.ARMY_MAX_SLOTS.get()),
                    x, sy, SpiritUi.TEXT_OK, false);
            sy += 11;
        }
    }

    private void renderPath(GuiGraphics graphics, SpiritData data, SpiritBranch branch, int color, int x, int y, int w) {
        int level = data.level(branch);
        boolean max = level >= SpiritBalance.MAX_LEVEL;

        heading(graphics, Component.translatable("path.spirit_form." + branch.id), color, null, x, y);
        SpiritUi.chip(graphics, font, x, y + 20,
                Component.translatable("screen.spirit_form.level.chip", level, SpiritBalance.MAX_LEVEL));
        int next = SpiritUi.paragraph(graphics, font,
                Component.translatable("screen.spirit_form.path." + branch.id + ".desc"),
                x, y + 38, w, SpiritUi.TEXT);

        int sy = next + 6;
        SpiritUi.section(graphics, font, x, sy, w, Component.translatable("screen.spirit_form.section.stats"));
        sy += 16;
        sy = SpiritUi.paragraph(graphics, font, levelInfo(branch, level), x, sy, w, SpiritUi.TEXT_OK);
        if (!max) {
            sy = SpiritUi.paragraph(graphics, font,
                    Component.translatable("screen.spirit_form.next.stat", levelInfo(branch, level + 1)),
                    x, sy + 1, w, SpiritUi.ACCENT);
        }

        // Смуга рівня
        int segW = (w - 14) / SpiritBalance.MAX_LEVEL;
        int by = sy + 4;
        for (int i = 0; i < SpiritBalance.MAX_LEVEL; i++) {
            int sx = x + i * segW;
            SpiritUi.inset(graphics, sx, by, segW - 1, 6);
            if (i < level) {
                graphics.fill(sx + 1, by + 1, sx + segW - 2, by + 5, color);
            }
        }

        int cy = by + 14;
        cy += 3;
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (max) {
            cy = SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.branch.max"),
                    x, cy, w, SpiritUi.TEXT_OK);
        } else {
            int cost = SpiritBalance.cost(level + 1);
            boolean ok = player != null && canAfford(player, cost);
            cy = SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.next.cost", cost),
                    x, cy, w, ok ? SpiritUi.TEXT_OK : SpiritUi.TEXT_BAD);
        }
    }

    private void renderAbility(GuiGraphics graphics, SpiritData data, SpiritAbility ability, int color, int x, int y, int w) {
        boolean owned = data.hasAbility(ability);
        boolean met = ability.isAvailable(data);

        heading(graphics, ability.displayName(), color, ability.id, x, y);
        SpiritUi.text(graphics, font, ability.requirement(), x, y + 22, owned || met ? SpiritUi.TEXT_OK : SpiritUi.TEXT_BAD);
        int next = SpiritUi.paragraph(graphics, font, ability.description(), x, y + 38, w, SpiritUi.TEXT);

        int cy = next + 6;
        cy += 3;
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (owned) {
            if (toggleAction(ability) >= 0) {
                boolean on = toggledOn(data, ability);
                SpiritUi.paragraph(graphics, font,
                        Component.translatable(on ? "screen.spirit_form.instinct.state.on" : "screen.spirit_form.instinct.state.off"),
                        x, cy, w, on ? SpiritUi.TEXT_OK : SpiritUi.TEXT_DIM);
            } else {
                SpiritUi.paragraph(graphics, font, Component.translatable("button.spirit_form.opened"),
                        x, cy, w, SpiritUi.TEXT_OK);
            }
        } else {
            boolean ok = met && player != null && canAfford(player, ability.cost());
            SpiritUi.paragraph(graphics, font,
                    Component.translatable("screen.spirit_form.ability.cost", ability.cost()),
                    x, cy, w, !met ? SpiritUi.TEXT_DIM : ok ? SpiritUi.TEXT_OK : SpiritUi.TEXT_BAD);
        }
    }

    private void renderFormLevel(GuiGraphics graphics, SpiritData data, int x, int y, int w) {
        int level = data.effectiveFormLevel();
        heading(graphics, Component.translatable("screen.spirit_form.formlevel.name"), COLOR_COMMON, "form", x, y);
        SpiritUi.chip(graphics, font, x, y + 20,
                Component.translatable("screen.spirit_form.level.chip", level, SpiritData.FORM_MAX));
        int sy = SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.formlevel.desc"),
                x, y + 38, w, SpiritUi.TEXT);
        sy += 4;
        for (int i = 1; i <= SpiritData.FORM_MAX; i++) {
            int color = i <= level ? SpiritUi.TEXT_OK : i == level + 1 ? SpiritUi.ACCENT : SpiritUi.TEXT_DIM;
            sy = SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.formlevel.l" + i),
                    x, sy, w, color);
        }
        sy += 3;
        if (data.canBeInvulnerable()) {
            boolean on = data.invulnerable();
            sy = SpiritUi.paragraph(graphics, font,
                    Component.translatable(on ? "screen.spirit_form.formlevel.invuln.on" : "screen.spirit_form.formlevel.invuln.off"),
                    x, sy, w, on ? SpiritUi.TEXT_OK : SpiritUi.TEXT_DIM) + 3;
        }
        if (data.fullForm()) {
            SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.formlevel.mode_full"),
                    x, sy, w, SpiritUi.TEXT_OK);
        } else if (level < SpiritData.FORM_MAX) {
            LocalPlayer player = minecraft == null ? null : minecraft.player;
            int cost = SpiritBalance.formCost(level + 1);
            SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.next.cost", cost),
                    x, sy, w, player != null && canAfford(player, cost) ? SpiritUi.TEXT_OK : SpiritUi.TEXT_BAD);
        } else {
            SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.branch.max"),
                    x, sy, w, SpiritUi.TEXT_OK);
        }
    }

    private void renderInstinct(GuiGraphics graphics, SpiritData data, int x, int y, int w) {
        heading(graphics, Component.translatable("passive.spirit_form.instinct"), COLOR_COMMON, "instinct", x, y);
        SpiritUi.text(graphics, font,
                Component.translatable(data.instinct() ? "screen.spirit_form.instinct.state.on" : "screen.spirit_form.instinct.state.off"),
                x, y + 22, data.instinct() ? SpiritUi.TEXT_OK : SpiritUi.TEXT_DIM);
        int next = SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.instinct.desc"),
                x, y + 38, w, SpiritUi.TEXT);
        SpiritUi.paragraph(graphics, font,
                Component.translatable("screen.spirit_form.instinct.cost", (Config.INSTINCT_HUNGER_COST.get() + 1) / 2),
                x, next + 6, w, SpiritUi.TEXT_DIM);
    }

    private void renderSouls(GuiGraphics graphics, SpiritData data, int x, int y, int w) {
        heading(graphics, Component.translatable("screen.spirit_form.souls.name"), COLOR_COMMON, "army", x, y);
        if (!data.hasAbility(SpiritAbility.ARMY)) {
            SpiritUi.text(graphics, font, Component.translatable("screen.spirit_form.ability.requirement_reward"),
                    x, y + 22, SpiritUi.TEXT_BAD);
        } else {
            boolean on = data.soulDrops();
            SpiritUi.text(graphics, font,
                    Component.translatable(on ? "screen.spirit_form.instinct.state.on" : "screen.spirit_form.instinct.state.off"),
                    x, y + 22, on ? SpiritUi.TEXT_OK : SpiritUi.TEXT_DIM);
        }
        SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.souls.desc"),
                x, y + 38, w, SpiritUi.TEXT);
    }

    private void renderShards(GuiGraphics graphics, SpiritData data, int x, int y, int w) {
        heading(graphics, Component.translatable("screen.spirit_form.shards.name"), COLOR_COMMON, "shard", x, y);
        SpiritUi.text(graphics, font,
                Component.translatable("screen.spirit_form.shards", data.shards(), Config.SOUL_SHARD_LIMIT.get()),
                x, y + 22, SpiritUi.TEXT_OK);
        int next = SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.shards.desc"),
                x, y + 38, w, SpiritUi.TEXT);

        int cy = next + 6;
        cy += 3;
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (data.shards() >= Config.SOUL_SHARD_LIMIT.get()) {
            SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.branch.max"),
                    x, cy, w, SpiritUi.TEXT_OK);
        } else {
            int cost = data.shards() + 1;
            SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.ability.cost", cost),
                    x, cy, w, player != null && canAfford(player, cost) ? SpiritUi.TEXT_OK : SpiritUi.TEXT_BAD);
        }
    }

    private void renderArmy(GuiGraphics graphics, SpiritData data, int x, int y, int w) {
        heading(graphics, Component.translatable("screen.spirit_form.army_slots.name"), COLOR_COMMON, "army", x, y);
        SpiritUi.text(graphics, font,
                Component.translatable("screen.spirit_form.army_slots", data.armySlots(), Config.ARMY_MAX_SLOTS.get()),
                x, y + 22, SpiritUi.TEXT_OK);
        int next = SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.army_slots.desc"),
                x, y + 38, w, SpiritUi.TEXT);

        int cy = next + 6;
        cy += 3;
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (!data.hasAbility(SpiritAbility.ARMY)) {
            SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.ability.requirement_reward"),
                    x, cy, w, SpiritUi.TEXT_BAD);
        } else if (data.armySlots() >= Config.ARMY_MAX_SLOTS.get()) {
            SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.branch.max"),
                    x, cy, w, SpiritUi.TEXT_OK);
        } else {
            int cost = data.armySlots();
            SpiritUi.paragraph(graphics, font, Component.translatable("screen.spirit_form.ability.cost", cost),
                    x, cy, w, player != null && canAfford(player, cost) ? SpiritUi.TEXT_OK : SpiritUi.TEXT_BAD);
        }
    }

    // ---------- показники шляхів ----------

    /** Параметри шляху на заданому рівні. */
    static Component levelInfo(SpiritBranch branch, int level) {
        // Текст залежить лише від шляху й рівня, а String.format щокадру зайвий: запам'ятовуємо до закриття вікна
        return LEVEL_INFO.computeIfAbsent(branch.ordinal() * 1000 + level, key -> buildLevelInfo(branch, level));
    }

    private static final java.util.Map<Integer, Component> LEVEL_INFO = new java.util.HashMap<>();

    private static Component buildLevelInfo(SpiritBranch branch, int level) {
        return switch (branch) {
            case MOVEMENT -> Component.translatable(
                    "stat.spirit_form.speed",
                    String.format(Locale.ROOT, "%.1f", SpiritBalance.speedBlocksPerSecond(level))
            );
            case HUNGER -> Component.translatable(
                    "stat.spirit_form.hunger",
                    String.format(Locale.ROOT, "%.3f", SpiritBalance.exhaustionPerBlock(level))
            );
            case DARKNESS -> darknessInfo(level);
        };
    }

    private static Component darknessInfo(int level) {
        if (SpiritBalance.darknessConstantInForm(level)) {
            int after = SpiritBalance.darknessAfterSeconds(level);
            return after > 0
                    ? Component.translatable("stat.spirit_form.darkness.constant", after)
                    : Component.translatable("stat.spirit_form.darkness.constant_none");
        }
        int seconds = SpiritBalance.darknessInFormSeconds(level);
        return seconds > 0
                ? Component.translatable("stat.spirit_form.darkness.temp", seconds)
                : Component.translatable("stat.spirit_form.darkness.none");
    }
}
