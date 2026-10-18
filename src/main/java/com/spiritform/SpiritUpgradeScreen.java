package com.spiritform;

import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Вікно прокачки форми Привида (клавіша H).
 */
public class SpiritUpgradeScreen extends Screen {

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 214;
    private static final int ROW_START = 46;
    private static final int ROW_STEP = 34;
    private static final int MARGIN = 10;
    private static final int GAP = 4;
    private static final int UPGRADE_W = 100;
    private static final int RESET_W = 72;

    private int left;
    private int top;

    private Button unlockButton;
    private final Button[] branchButtons = new Button[SpiritBranch.values().length];
    private final Button[] resetButtons = new Button[SpiritBranch.values().length];
    private Button closeButton;

    public SpiritUpgradeScreen() {
        super(Component.translatable("screen.spirit_form.title"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;

        int resetX = left + PANEL_W - MARGIN - RESET_W;
        int upgradeX = resetX - GAP - UPGRADE_W;

        unlockButton = addRenderableWidget(
                Button.builder(Component.empty(), b -> send(UpgradeSpiritPayload.UNLOCK))
                        .bounds(upgradeX, rowY(0), UPGRADE_W, 20)
                        .build()
        );

        for (SpiritBranch branch : SpiritBranch.values()) {
            int upgradeAction = UpgradeSpiritPayload.actionFor(branch);
            int resetAction = UpgradeSpiritPayload.resetActionFor(branch);
            int row = rowY(branch.ordinal() + 1);

            branchButtons[branch.ordinal()] = addRenderableWidget(
                    Button.builder(Component.empty(), b -> send(upgradeAction))
                            .bounds(upgradeX, row, UPGRADE_W, 20)
                            .build()
            );

            resetButtons[branch.ordinal()] = addRenderableWidget(
                    Button.builder(Component.empty(), b -> send(resetAction))
                            .bounds(resetX, row, RESET_W, 20)
                            .build()
            );
        }

        closeButton = addRenderableWidget(
                Button.builder(Component.translatable("button.spirit_form.close"), b -> onClose())
                        .bounds(left + (PANEL_W - 140) / 2, top + PANEL_H - 28, 140, 20)
                        .build()
        );

        refreshButtons();
    }

    @Override
    public void tick() {
        refreshButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SpiritKeyMappings.OPEN_UPGRADES.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xD0101018);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF8888AA);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return;
        }
        SpiritData data = SpiritFormState.get(player);

        int textX = left + MARGIN;
        int textWidth = left + PANEL_W - MARGIN - RESET_W - GAP - UPGRADE_W - GAP - textX;

        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.spirit_form.xp", player.experienceLevel),
                width / 2, top + 24, 0xA0FF70
        );

        // Спільний рівень 1
        int y = rowY(0);
        graphics.drawString(font, Component.translatable("screen.spirit_form.base"), textX, y, 0xFFFFFF);
        graphics.drawWordWrap(
                font,
                Component.translatable(data.unlocked()
                        ? "screen.spirit_form.base.opened"
                        : "screen.spirit_form.base.closed"),
                textX, y + 11, textWidth, 0xAAAAAA
        );

        // Гілки
        for (SpiritBranch branch : SpiritBranch.values()) {
            int level = data.level(branch);
            int rowY = rowY(branch.ordinal() + 1);

            Component name = Component.translatable("branch.spirit_form." + branch.id)
                    .append(Component.literal("  " + level + "/" + SpiritBalance.MAX_LEVEL));
            graphics.drawString(font, name, textX, rowY, 0xFFFFFF);
            graphics.drawWordWrap(font, statLine(branch, level), textX, rowY + 11, textWidth, 0xAAAAAA);
        }
    }

    private void refreshButtons() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null || unlockButton == null) {
            return;
        }
        SpiritData data = SpiritFormState.get(player);

        // Спільний рівень 1
        if (data.unlocked()) {
            unlockButton.setMessage(Component.translatable("button.spirit_form.opened"));
            unlockButton.active = false;
        } else {
            unlockButton.setMessage(Component.translatable("button.spirit_form.unlock", SpiritBalance.cost(1)));
            unlockButton.active = canAfford(player, SpiritBalance.cost(1));
        }

        // Гілки
        for (SpiritBranch branch : SpiritBranch.values()) {
            Button upgradeButton = branchButtons[branch.ordinal()];
            Button resetButton = resetButtons[branch.ordinal()];
            int level = data.level(branch);

            if (!data.unlocked()) {
                upgradeButton.setMessage(Component.translatable("button.spirit_form.locked"));
                upgradeButton.active = false;
            } else if (level >= SpiritBalance.MAX_LEVEL) {
                upgradeButton.setMessage(Component.translatable("button.spirit_form.max"));
                upgradeButton.active = false;
            } else {
                int next = level + 1;
                int cost = SpiritBalance.cost(next);
                upgradeButton.setMessage(Component.translatable("button.spirit_form.upgrade", next, cost));
                upgradeButton.active = canAfford(player, cost);
            }

            resetButton.setMessage(
                    Component.translatable("button.spirit_form.reset_branch", SpiritBalance.branchSpent(level))
            );
            resetButton.active = level > 1;
        }
    }

    private static boolean canAfford(LocalPlayer player, int cost) {
        return player.getAbilities().instabuild || player.experienceLevel >= cost;
    }

    private Component statLine(SpiritBranch branch, int level) {
        return switch (branch) {
            case MOVEMENT -> Component.translatable(
                    "stat.spirit_form.speed",
                    String.format(Locale.ROOT, "%.1f", SpiritBalance.speedBlocksPerSecond(level))
            );
            case HUNGER -> Component.translatable(
                    "stat.spirit_form.hunger",
                    String.format(Locale.ROOT, "%.3f", SpiritBalance.exhaustionPerBlock(level))
            );
            case DARKNESS -> darknessLine(level);
        };
    }

    private Component darknessLine(int level) {
        if (SpiritBalance.highlightsMobs(level)) {
            return Component.translatable("stat.spirit_form.darkness.highlight", Config.HIGHLIGHT_RADIUS.get().intValue());
        }
        if (SpiritBalance.darknessConstantInForm(level)) {
            int after = SpiritBalance.darknessAfterSeconds(level);
            return after > 0
                    ? Component.translatable("stat.spirit_form.darkness.constant", after)
                    : Component.translatable("stat.spirit_form.darkness.constant_none");
        }
        return Component.translatable("stat.spirit_form.darkness.temp", SpiritBalance.darknessInFormSeconds(level));
    }

    private int rowY(int index) {
        return top + ROW_START + index * ROW_STEP;
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(new UpgradeSpiritPayload(action));
    }
}
