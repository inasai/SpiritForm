package com.spiritform;

import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Вікно одного шляху: поточний рівень із параметрами, наступний рівень із параметрами,
 * знизу кнопка прокачування та маленька кнопка скидання.
 */
public class SpiritBranchScreen extends SpiritScreenBase {

    private final SpiritBranch branch;
    private Button upgradeButton;
    private IconButton resetButton;

    public SpiritBranchScreen(Screen parent, SpiritBranch branch) {
        super(Component.translatable("path.spirit_form." + branch.id), parent);
        this.branch = branch;
    }

    @Override
    protected void buildContent() {
        int upgradeW = 180;
        int x = left + (PANEL_W - upgradeW - 4 - 20) / 2;
        int y = top + PANEL_H - 60;

        upgradeButton = addRenderableWidget(
                Button.builder(Component.empty(), b -> send(UpgradeSpiritPayload.actionFor(branch)))
                        .bounds(x, y, upgradeW, 20)
                        .build()
        );

        resetButton = addRenderableWidget(new IconButton(
                x + upgradeW + 4, y, 20, 20,
                IconButton.Icon.RESET,
                b -> send(UpgradeSpiritPayload.resetActionFor(branch)),
                Component.translatable("button.spirit_form.reset_tooltip", 0)
        ));
    }

    @Override
    protected void refresh() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null || upgradeButton == null) {
            return;
        }

        int level = data().level(branch);

        if (level >= SpiritBalance.MAX_LEVEL) {
            upgradeButton.setMessage(Component.translatable("button.spirit_form.max"));
            upgradeButton.active = false;
        } else {
            int cost = SpiritBalance.cost(level + 1);
            upgradeButton.setMessage(Component.translatable("button.spirit_form.levelup", cost));
            upgradeButton.active = canAfford(player, cost);
        }

        resetButton.active = level > 1;
        resetButton.setTooltipText(
                Component.translatable("button.spirit_form.reset_tooltip", SpiritBalance.branchSpent(level))
        );
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int level = data().level(branch);
        int x = left + MARGIN;
        int textWidth = PANEL_W - 2 * MARGIN;

        // Поточний рівень
        int y = top + 48;
        graphics.drawString(
                font,
                Component.translatable("screen.spirit_form.branch.current", level + "/" + SpiritBalance.MAX_LEVEL),
                x, y, 0xFFFFFF
        );
        graphics.drawWordWrap(font, levelInfo(branch, level), x, y + 14, textWidth, 0xAAAAAA);

        // Наступний рівень
        y = top + 108;
        if (level >= SpiritBalance.MAX_LEVEL) {
            graphics.drawString(
                    font, Component.translatable("screen.spirit_form.branch.max"), x, y, 0xA0FF70);
        } else {
            graphics.drawString(
                    font,
                    Component.translatable("screen.spirit_form.branch.next", (level + 1) + "/" + SpiritBalance.MAX_LEVEL),
                    x, y, 0xFFFFFF
            );
            graphics.drawWordWrap(font, levelInfo(branch, level + 1), x, y + 14, textWidth, 0xAAAAAA);
        }
    }

    /** Параметри шляху на заданому рівні. */
    static Component levelInfo(SpiritBranch branch, int level) {
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
