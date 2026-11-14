package com.spiritform.client.screen;

import com.spiritform.client.SpiritAnim;
import com.spiritform.client.gui.SpiritArmyIcons;
import com.spiritform.client.gui.SpiritUi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Вибір іконки душі: пошук серед усіх предметів, клацання вибирає предмет. */
public class ArmyIconScreen extends Screen {

    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int CELL = 20;

    private final Screen parent;
    private final UUID soul;
    private EditBox search;
    private final List<Item> matches = new ArrayList<>();
    private int scroll;
    private final SpiritAnim anim = new SpiritAnim();

    public ArmyIconScreen(Screen parent, UUID soul) {
        super(Component.translatable("screen.spirit_form.army.icon_title"));
        this.parent = parent;
        this.soul = soul;
    }

    private int gridLeft() {
        return width / 2 - COLS * CELL / 2;
    }

    private int gridTop() {
        return height / 2 - ROWS * CELL / 2;
    }

    @Override
    protected void init() {
        anim.reopen();
        search = new EditBox(font, width / 2 - 90, gridTop() - 26, 180, 18,
                Component.translatable("screen.spirit_form.army.icon_search"));
        search.setHint(Component.translatable("screen.spirit_form.army.icon_search"));
        search.setResponder(text -> {
            scroll = 0;
            rebuild(text);
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        addRenderableWidget(Button.builder(Component.translatable("screen.spirit_form.army.icon_reset"), b -> {
            SpiritArmyIcons.set(soul, null);
            onClose();
        }).bounds(width / 2 - 50, gridTop() + ROWS * CELL + 8, 100, 20).build());

        rebuild("");
    }

    private void rebuild(String text) {
        matches.clear();
        String query = text.trim().toLowerCase(Locale.ROOT);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            if (query.isEmpty()
                    || new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                    || BuiltInRegistries.ITEM.getKey(item).getPath().contains(query)) {
                matches.add(item);
            }
        }
    }

    private int maxScroll() {
        int rows = (matches.size() + COLS - 1) / COLS;
        return Math.max(0, rows - ROWS);
    }

    private int panelLeft() {
        return gridLeft() - 14;
    }

    private int panelTop() {
        return gridTop() - 50;
    }

    private int panelWidth() {
        return COLS * CELL + 28;
    }

    private int panelHeight() {
        return ROWS * CELL + 50 + 42;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        SpiritUi.panel(graphics, panelLeft(), panelTop(), panelWidth(), panelHeight());
    }

    @Override
    public void tick() {
        anim.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        anim.begin(graphics, width, height, width / 2, panelTop() + panelHeight() / 2);
        super.render(graphics, mouseX, mouseY, partialTick);
        SpiritUi.text(graphics, font, title, panelLeft() + 8, panelTop() + 6, SpiritUi.TEXT);

        int left = gridLeft();
        int top = gridTop();
        SpiritUi.inset(graphics, left - 1, top - 1, COLS * CELL + 2, ROWS * CELL + 2);

        Item hovered = null;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = (row + scroll) * COLS + col;
                if (index >= matches.size()) {
                    continue;
                }
                int x = left + col * CELL;
                int y = top + row * CELL;
                boolean over = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL && !anim.busy();
                if (over) {
                    graphics.fill(x, y, x + CELL, y + CELL, 0x80FFFFFF);
                    hovered = matches.get(index);
                }
                graphics.renderItem(new ItemStack(matches.get(index)), x + 2, y + 2);
            }
        }
        if (hovered != null) {
            graphics.renderTooltip(font, new ItemStack(hovered).getHoverName(), mouseX, mouseY);
        }
        anim.end(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (anim.busy()) {
            return true;
        }
        if (button == 0) {
            int col = (int) Math.floor((mouseX - gridLeft()) / CELL);
            int row = (int) Math.floor((mouseY - gridTop()) / CELL);
            if (mouseX >= gridLeft() && mouseY >= gridTop() && col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                int index = (row + scroll) * COLS + col;
                if (index < matches.size()) {
                    SpiritArmyIcons.set(soul, matches.get(index));
                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        anim.close(() -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        });
    }
}
