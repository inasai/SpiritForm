package com.spiritform.client.screen;

import com.spiritform.army.ArmySoulStats;
import com.spiritform.army.SpiritArmy;
import com.spiritform.client.SpiritArmyClient;
import com.spiritform.client.gui.SpiritUi;
import com.spiritform.network.ArmyActionPayload;
import com.spiritform.network.ArmySyncPayload;
import com.spiritform.registry.SpiritItems;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Вдосконалення душ Армії (доступне Володарю Потойбіччя): ліворуч список душ із рангами, праворуч ранг і прогрес,
 * здоров'я, шкода та швидкість (кожну можна покращити за Кристалізовані Душі), чотири комірки обладунків
 * та інвентар. Обладунок вибирається клацанням у інвентарі, потім клацанням по комірці (Shift + клацання одягає одразу).
 */
public class ArmySoulsScreen extends Screen {

    private static final int W = 380;
    private static final int H = 272;
    private static final int LIST_W = 132;
    private static final int ROW = 26;
    private static final int CELL = 18;

    private static final int[] STAT_COLORS = {0xFFE46C6C, 0xFFE8B25A, 0xFF6CB8E8};

    private final Screen parent;
    private UUID selected;
    private int scroll;
    private int selectedSlot = -1;
    private int left;
    private int top;

    private ItemStack tipStack = ItemStack.EMPTY;
    private Component tipText;

    public ArmySoulsScreen(Screen parent) {
        super(Component.translatable("screen.spirit_form.souls.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(2, (height - H) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    // ---------- розкладка ----------

    private int listX() {
        return left + 10;
    }

    private int listY() {
        return top + 28;
    }

    private int listH() {
        return H - 28 - 24;
    }

    private int visibleRows() {
        return listH() / ROW;
    }

    private int rx() {
        return left + 152;
    }

    private int rw() {
        return W - 152 - 10;
    }

    private int statY(int stat) {
        return top + 28 + 48 + stat * 20;
    }

    private int armorY() {
        return top + 28 + 48 + 66;
    }

    private int invY() {
        return armorY() + 22;
    }

    private int slotY(int row) {
        return invY() + row * CELL + (row == 3 ? 3 : 0);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int buttonX() {
        return rx() + rw() - 42;
    }

    // ---------- дані ----------

    private List<ArmySyncPayload.Entry> souls() {
        return SpiritArmyClient.souls();
    }

    private ArmySyncPayload.Entry current() {
        List<ArmySyncPayload.Entry> list = souls();
        if (list.isEmpty()) {
            return null;
        }
        for (ArmySyncPayload.Entry entry : list) {
            if (entry.id().equals(selected)) {
                return entry;
            }
        }
        selected = list.get(0).id();
        return list.get(0);
    }

    private static int crystals(LocalPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(SpiritItems.CRYSTAL_SOUL.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static String fmt(double value, int digits) {
        return String.format(Locale.ROOT, "%." + digits + "f", value);
    }

    private static void send(int action, String arg) {
        PacketDistributor.sendToServer(new ArmyActionPayload(action, arg));
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    // ---------- малювання ----------

    private static void slotBox(GuiGraphics g, int x, int y, int border) {
        g.fill(x, y, x + CELL, y + CELL, border);
        g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, SpiritUi.INSET);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        tipStack = ItemStack.EMPTY;
        tipText = null;

        SpiritUi.panel(g, left, top, W, H);
        SpiritUi.section(g, font, left + 10, top + 8, W - 20, title);
        Component crystalText = Component.translatable("screen.spirit_form.souls.crystals", crystals(player));
        int textW = font.width(crystalText);
        SpiritUi.text(g, font, crystalText, left + W - 10 - textW - 18, top + 9, SpiritUi.TEXT_DIM);
        g.renderItem(new ItemStack(SpiritItems.CRYSTAL_SOUL.get()), left + W - 10 - 16, top + 5);

        renderList(g, mouseX, mouseY);
        SpiritUi.engravedV(g, left + 10 + LIST_W + 4, top + 28, listH());

        ArmySyncPayload.Entry entry = current();
        if (entry == null) {
            SpiritUi.paragraph(g, font, Component.translatable("screen.spirit_form.souls.empty"),
                    rx(), top + 36, rw(), SpiritUi.TEXT_DIM);
        } else {
            renderDetails(g, mouseX, mouseY, player, entry);
            renderInventory(g, mouseX, mouseY, player, entry);
        }

        SpiritUi.engraved(g, left + 10, top + H - 22, W - 20);
        SpiritUi.paragraph(g, font, Component.translatable("screen.spirit_form.souls.hint"),
                left + 10, top + H - 17, W - 20, SpiritUi.TEXT_DIM);

        if (!tipStack.isEmpty()) {
            g.renderTooltip(font, tipStack, mouseX, mouseY);
        } else if (tipText != null) {
            g.renderTooltip(font, tipText, mouseX, mouseY);
        }
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int x = listX();
        int y = listY();
        SpiritUi.inset(g, x - 1, y - 1, LIST_W + 2, listH() + 2);
        List<ArmySyncPayload.Entry> list = souls();
        int maxScroll = Math.max(0, list.size() - visibleRows());
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        g.enableScissor(x, y, x + LIST_W, y + listH());
        for (int i = 0; i < visibleRows() && scroll + i < list.size(); i++) {
            ArmySyncPayload.Entry soul = list.get(scroll + i);
            int ry = y + i * ROW;
            boolean chosen = soul.id().equals(selected);
            boolean hover = inside(mouseX, mouseY, x, ry, LIST_W, ROW);
            if (chosen || hover) {
                g.fill(x, ry, x + LIST_W, ry + ROW - 1, chosen ? 0xFF2E4A40 : 0xFF26262C);
            }
            if (chosen) {
                g.fill(x, ry, x + 2, ry + ROW - 1, SpiritUi.ACCENT);
            }
            g.renderItem(ArmyWheelScreen.soulStack(soul), x + 5, ry + 5);
            Component name = ArmyWheelScreen.soulName(soul);
            SpiritUi.text(g, font, font.plainSubstrByWidth(name.getString(), LIST_W - 30), x + 26, ry + 4, SpiritUi.TEXT);
            SpiritUi.text(g, font, Component.translatable("rank.spirit_form.army." + soul.rank()),
                    x + 26, ry + 14, SpiritUi.TEXT_DIM);
        }
        g.disableScissor();
        if (list.size() > visibleRows()) {
            int barH = Math.max(10, listH() * visibleRows() / list.size());
            int barY = y + (listH() - barH) * scroll / Math.max(1, maxScroll);
            g.fill(x + LIST_W - 2, barY, x + LIST_W, barY + barH, SpiritUi.WHITE);
        }
    }

    private void renderDetails(GuiGraphics g, int mouseX, int mouseY, LocalPlayer player, ArmySyncPayload.Entry e) {
        int x = rx();
        int w = rw();
        int y = top + 28;
        int rank = e.rank();

        g.renderItem(ArmyWheelScreen.soulStack(e), x, y);
        SpiritUi.text(g, font, ArmyWheelScreen.soulName(e), x + 22, y + 1, SpiritUi.ACCENT);
        SpiritUi.text(g, font, Component.translatable("screen.spirit_form.souls.rank",
                Component.translatable("rank.spirit_form.army." + rank), rank + 1, ArmySoulStats.MAX_RANK + 1),
                x + 22, y + 11, SpiritUi.TEXT_DIM);

        // Прогрес до наступного рангу
        int next = ArmySoulStats.nextKills(rank);
        SpiritUi.inset(g, x, y + 24, w, 7);
        float progress = next < 0 ? 1.0F : (float) (e.kills() - ArmySoulStats.RANK_KILLS[rank])
                / (next - ArmySoulStats.RANK_KILLS[rank]);
        g.fill(x + 1, y + 25, x + 1 + Math.round((w - 3) * Math.max(0.0F, Math.min(1.0F, progress))), y + 30,
                SpiritUi.ACCENT);
        SpiritUi.text(g, font, next < 0
                        ? Component.translatable("screen.spirit_form.souls.kills_max", e.kills())
                        : Component.translatable("screen.spirit_form.souls.kills", e.kills(), next),
                x, y + 34, SpiritUi.TEXT_DIM);
        if (inside(mouseX, mouseY, x, y + 24, w, 18)) {
            tipText = Component.translatable("screen.spirit_form.souls.rank_tip");
        }

        // Три характеристики
        int crystals = crystals(player);
        for (int stat = 0; stat < 3; stat++) {
            int sy = statY(stat);
            int level = stat == 0 ? e.healthLevel() : stat == 1 ? e.damageLevel() : e.speedLevel();
            g.fill(x, sy + 1, x + 3, sy + 11, STAT_COLORS[stat]);
            String key = stat == 0 ? "health" : stat == 1 ? "attack" : "speed";
            SpiritUi.text(g, font, Component.translatable("screen.spirit_form." + "souls." + key), x + 7, sy + 2, SpiritUi.TEXT);
            SpiritUi.text(g, font, statValue(e, stat), x + 70, sy + 2, SpiritUi.TEXT);
            SpiritUi.text(g, font, Component.translatable("screen.spirit_form.souls.level", level, ArmySoulStats.MAX_LEVEL),
                    x + 122, sy + 2, SpiritUi.TEXT_DIM);

            int bx = buttonX();
            boolean maxed = level >= ArmySoulStats.MAX_LEVEL;
            int cost = ArmySoulStats.cost(level);
            boolean can = !maxed && (player.getAbilities().instabuild || crystals >= cost);
            SpiritUi.raised(g, bx, sy - 1, 42, 14);
            Component label = maxed ? Component.translatable("screen.spirit_form.souls.max") : Component.literal("+" + cost);
            SpiritUi.centered(g, font, label, bx + 21, sy + 2, maxed ? SpiritUi.TEXT_DIM : can ? SpiritUi.ACCENT : SpiritUi.TEXT_BAD);
            if (inside(mouseX, mouseY, bx, sy - 1, 42, 14)) {
                tipText = maxed ? Component.translatable("screen.spirit_form.souls.maxed")
                        : Component.translatable("screen.spirit_form.souls.upgrade", cost);
            }
        }

        // Обладунки
        int ay = armorY();
        SpiritUi.text(g, font, Component.translatable("screen.spirit_form.souls.armor"), x, ay + 5, SpiritUi.TEXT);
        double[] totals = ArmySoulStats.armorTotals(e.armor());
        for (int i = 0; i < 4; i++) {
            int ax = x + 64 + i * (CELL + 2);
            ItemStack stack = e.armor().get(i);
            boolean target = selectedSlot >= 0 && ArmySoulStats.armorIndex(player.getInventory().getItem(selectedSlot)) == i;
            slotBox(g, ax, ay, target ? SpiritUi.ACCENT : SpiritUi.INSET_DARK);
            if (!stack.isEmpty()) {
                g.renderItem(stack, ax + 1, ay + 1);
                if (inside(mouseX, mouseY, ax, ay, CELL, CELL)) {
                    tipStack = stack;
                }
            } else if (inside(mouseX, mouseY, ax, ay, CELL, CELL)) {
                tipText = Component.translatable("screen.spirit_form.souls.armor.empty." + i);
            }
        }
        SpiritUi.text(g, font, Component.translatable("screen.spirit_form.souls.defense",
                fmt(totals[0], 0)), x + 64 + 4 * (CELL + 2) + 4, ay + 5, SpiritUi.TEXT_DIM);
    }

    private Component statValue(ArmySyncPayload.Entry e, int stat) {
        int rank = e.rank();
        if (stat == 0) {
            return Component.literal(e.hp() + " (×" + fmt(ArmySoulStats.healthMult(rank, e.healthLevel()), 1) + ")");
        }
        if (stat == 1) {
            double mult = ArmySoulStats.damageMult(rank, e.damageLevel());
            return Component.literal(e.attack() > 0.0F
                    ? fmt(e.attack() * mult, 1) + " (×" + fmt(mult, 1) + ")" : "×" + fmt(mult, 1));
        }
        return Component.literal("×" + fmt(ArmySoulStats.speedMult(rank, e.speedLevel()), 2));
    }

    private void renderInventory(GuiGraphics g, int mouseX, int mouseY, LocalPlayer player, ArmySyncPayload.Entry e) {
        ItemStack held = selectedSlot >= 0 ? player.getInventory().getItem(selectedSlot) : ItemStack.EMPTY;
        if (ArmySoulStats.armorIndex(held) < 0) {
            selectedSlot = -1;
        }
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = rx() + col * CELL;
                int sy = slotY(row);
                int slot = row == 3 ? col : 9 + row * 9 + col;
                ItemStack stack = player.getInventory().getItem(slot);
                boolean chosen = slot == selectedSlot;
                boolean armor = ArmySoulStats.armorIndex(stack) >= 0;
                slotBox(g, sx, sy, chosen ? SpiritUi.ACCENT : armor ? 0xFF3C8068 : SpiritUi.INSET_DARK);
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

    // ---------- керування ----------

    private int slotAt(double mx, double my) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                if (inside(mx, my, rx() + col * CELL, slotY(row), CELL, CELL)) {
                    return row == 3 ? col : 9 + row * 9 + col;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, listX(), listY(), LIST_W, listH())) {
            scroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (button == 1) {
            if (selectedSlot >= 0) {
                selectedSlot = -1;
            } else {
                click();
                onClose();
            }
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }

        List<ArmySyncPayload.Entry> list = souls();
        if (inside(mx, my, listX(), listY(), LIST_W, listH())) {
            int index = scroll + (int) ((my - listY()) / ROW);
            if (index >= 0 && index < list.size()) {
                selected = list.get(index).id();
                selectedSlot = -1;
                click();
            }
            return true;
        }

        ArmySyncPayload.Entry entry = current();
        if (entry == null) {
            return super.mouseClicked(mx, my, button);
        }
        String id = entry.id().toString();

        for (int stat = 0; stat < 3; stat++) {
            if (inside(mx, my, buttonX(), statY(stat) - 1, 42, 14)) {
                send(SpiritArmy.ACT_UPGRADE, id + ":" + stat);
                return true;
            }
        }
        for (int i = 0; i < 4; i++) {
            int ax = rx() + 64 + i * (CELL + 2);
            if (inside(mx, my, ax, armorY(), CELL, CELL)) {
                if (selectedSlot >= 0 && ArmySoulStats.armorIndex(player.getInventory().getItem(selectedSlot)) == i) {
                    send(SpiritArmy.ACT_EQUIP, id + ":" + selectedSlot);
                    selectedSlot = -1;
                } else if (!entry.armor().get(i).isEmpty()) {
                    send(SpiritArmy.ACT_UNEQUIP, id + ":" + i);
                }
                click();
                return true;
            }
        }
        int slot = slotAt(mx, my);
        if (slot >= 0) {
            if (ArmySoulStats.armorIndex(player.getInventory().getItem(slot)) >= 0) {
                if (hasShiftDown()) {
                    send(SpiritArmy.ACT_EQUIP, id + ":" + slot);
                    selectedSlot = -1;
                } else {
                    selectedSlot = slot == selectedSlot ? -1 : slot;
                }
                click();
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
