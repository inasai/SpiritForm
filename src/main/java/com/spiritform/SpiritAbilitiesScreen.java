package com.spiritform;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Вікно здібностей Привида. У списку лише назва й вимога до рівня шляху, а опис з'являється
 * при наведенні курсора на здібність. Здібностей багато, тому вони розбиті на сторінки.
 * Вартість і потрібні рівні шляхів задаються в конфігу.
 */
public class SpiritAbilitiesScreen extends SpiritScreenBase {

    private static final int ROW_START = 40;
    private static final int ROW_STEP = 28;
    private static final int BUTTON_W = 104;
    private static final int PAGE_SIZE = 6;

    private final Button[] buttons = new Button[SpiritAbility.values().length];
    private Button shardButton;
    private Button armyButton;
    private Button previousPage;
    private Button nextPage;
    private int page;

    public SpiritAbilitiesScreen(Screen parent) {
        super(Component.translatable("screen.spirit_form.abilities"), parent);
    }

    /** Здібності за зростанням потрібного рівня. */
    private static final java.util.List<SpiritAbility> ORDER = SpiritAbility.sorted();

    private static int pageCount() {
        return (ORDER.size() + 2 + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private int rowY(SpiritAbility ability) {
        return top + ROW_START + (ORDER.indexOf(ability) % PAGE_SIZE) * ROW_STEP;
    }

    /** Рядок "Уламки Душі" стоїть одразу після всіх здібностей. */
    private int shardRowY() {
        return top + ROW_START + (ORDER.size() % PAGE_SIZE) * ROW_STEP;
    }

    /** Рядок "Армія Потойбіччя" (місця у сховищі) стоїть після рядка уламків. */
    private int armyRowY() {
        return top + ROW_START + ((ORDER.size() + 1) % PAGE_SIZE) * ROW_STEP;
    }

    private boolean armyOnPage() {
        return (ORDER.size() + 1) / PAGE_SIZE == page;
    }

    private boolean shardOnPage() {
        return ORDER.size() / PAGE_SIZE == page;
    }

    private boolean onPage(SpiritAbility ability) {
        return ORDER.indexOf(ability) / PAGE_SIZE == page;
    }

    @Override
    protected void buildContent() {
        for (SpiritAbility ability : SpiritAbility.values()) {
            if (ability.rewardOnly()) {
                continue;
            }
            buttons[ability.ordinal()] = addRenderableWidget(
                    Button.builder(Component.empty(), b -> send(data().hasAbility(ability)
                            ? UpgradeSpiritPayload.resetAbilityActionFor(ability)
                            : UpgradeSpiritPayload.abilityActionFor(ability)))
                            .bounds(left + PANEL_W - MARGIN - BUTTON_W, rowY(ability) + 1, BUTTON_W, 20)
                            .build()
            );
        }

        shardButton = addRenderableWidget(
                Button.builder(Component.empty(), b -> send(UpgradeSpiritPayload.BUY_SHARD))
                        .bounds(left + PANEL_W - MARGIN - BUTTON_W, shardRowY() + 1, BUTTON_W, 20)
                        .build()
        );

        armyButton = addRenderableWidget(
                Button.builder(Component.empty(), b -> send(UpgradeSpiritPayload.BUY_ARMY_SLOT))
                        .bounds(left + PANEL_W - MARGIN - BUTTON_W, armyRowY() + 1, BUTTON_W, 20)
                        .build()
        );

        previousPage = addRenderableWidget(
                Button.builder(Component.literal("<"), b -> setPage(page - 1))
                        .bounds(left + PANEL_W / 2 - 46, top + PANEL_H - 28, 20, 20)
                        .build()
        );
        nextPage = addRenderableWidget(
                Button.builder(Component.literal(">"), b -> setPage(page + 1))
                        .bounds(left + PANEL_W / 2 + 26, top + PANEL_H - 28, 20, 20)
                        .build()
        );
    }

    private void setPage(int value) {
        page = Math.max(0, Math.min(pageCount() - 1, value));
        refresh();
    }

    @Override
    protected void refresh() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null || buttons[0] == null || shardButton == null || armyButton == null) {
            return;
        }

        SpiritData data = data();

        previousPage.active = page > 0;
        nextPage.active = page < pageCount() - 1;

        for (SpiritAbility ability : SpiritAbility.values()) {
            if (ability.rewardOnly()) {
                continue;
            }
            Button button = buttons[ability.ordinal()];
            button.visible = onPage(ability);

            if (data.hasAbility(ability)) {
                button.setMessage(Component.translatable("button.spirit_form.ability_reset", ability.cost()));
                button.active = true;
            } else if (!ability.isAvailable(data)) {
                button.setMessage(Component.translatable("button.spirit_form.locked"));
                button.active = false;
            } else {
                int cost = ability.cost();
                button.setMessage(Component.translatable("button.spirit_form.ability_unlock", cost));
                button.active = canAfford(player, cost);
            }
        }

        shardButton.visible = shardOnPage();
        int owned = data.shards();
        if (owned >= Config.SOUL_SHARD_LIMIT.get()) {
            shardButton.setMessage(Component.translatable("button.spirit_form.shard_max"));
            shardButton.active = false;
        } else {
            int shardCost = owned + 1;
            shardButton.setMessage(Component.translatable("button.spirit_form.shard_buy", shardCost));
            shardButton.active = data.unlocked() && canAfford(player, shardCost);
        }

        armyButton.visible = armyOnPage();
        int slots = data.armySlots();
        if (!data.hasAbility(SpiritAbility.ARMY)) {
            armyButton.setMessage(Component.translatable("button.spirit_form.locked"));
            armyButton.active = false;
        } else if (slots >= Config.ARMY_MAX_SLOTS.get()) {
            armyButton.setMessage(Component.translatable("button.spirit_form.army_slot_max"));
            armyButton.active = false;
        } else {
            armyButton.setMessage(Component.translatable("button.spirit_form.army_slot_buy", slots));
            armyButton.active = canAfford(player, slots);
        }
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        SpiritData data = data();
        int x = left + MARGIN;
        int rowWidth = PANEL_W - 2 * MARGIN - BUTTON_W - 8;
        SpiritAbility hovered = null;

        for (SpiritAbility ability : ORDER) {
            if (!onPage(ability)) {
                continue;
            }
            int y = rowY(ability);

            SpiritIcons.draw(graphics, ability.id, x, y + 2);
            graphics.drawString(font, ability.displayName(), x + 22, y, 0xFFFFFF);

            boolean met = ability.isAvailable(data);
            graphics.drawString(font, ability.requirement(), x + 22, y + 11, met ? 0x80FF80 : 0xFF6060);

            if (mouseX >= x && mouseX < x + rowWidth && mouseY >= y - 1 && mouseY < y + ROW_STEP - 4) {
                hovered = ability;
            }
        }

        boolean hoverShard = false;
        if (shardOnPage()) {
            int y = shardRowY();
            SpiritIcons.draw(graphics, "shard", x, y + 2);
            graphics.drawString(font,
                    Component.translatable("screen.spirit_form.shards", data.shards(), Config.SOUL_SHARD_LIMIT.get()),
                    x + 22, y, 0xFFFFFF);
            graphics.drawString(font, Component.translatable("screen.spirit_form.shards.hint"), x + 22, y + 11, 0xA0A0C0);
            hoverShard = mouseX >= x && mouseX < x + rowWidth && mouseY >= y - 1 && mouseY < y + ROW_STEP - 4;
        }

        boolean hoverArmy = false;
        if (armyOnPage()) {
            int y = armyRowY();
            SpiritIcons.draw(graphics, "army", x, y + 2);
            graphics.drawString(font,
                    Component.translatable("screen.spirit_form.army_slots", data.armySlots(), Config.ARMY_MAX_SLOTS.get()),
                    x + 22, y, 0xFFFFFF);
            graphics.drawString(font, Component.translatable("screen.spirit_form.army_slots.hint"), x + 22, y + 11, 0xA0A0C0);
            hoverArmy = mouseX >= x && mouseX < x + rowWidth && mouseY >= y - 1 && mouseY < y + ROW_STEP - 4;
        }

        graphics.drawCenteredString(
                font, Component.literal((page + 1) + "/" + pageCount()),
                left + PANEL_W / 2, top + PANEL_H - 22, 0xAAAAAA);

        if (hoverArmy) {
            graphics.renderTooltip(font,
                    font.split(Component.translatable("screen.spirit_form.army_slots.desc"), 190), mouseX, mouseY);
        } else if (hoverShard) {
            graphics.renderTooltip(font,
                    font.split(Component.translatable("screen.spirit_form.shards.desc"), 190), mouseX, mouseY);
        } else if (hovered != null) {
            List<FormattedCharSequence> lines = font.split(hovered.description(), 190);
            graphics.renderTooltip(font, lines, mouseX, mouseY);
        }
    }
}
