package com.spiritform;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Головне вікно прокачки (клавіша H).
 * Поки форма закрита, у ньому лише кнопка відкриття. Після відкриття: три шляхи
 * в один ряд і широка кнопка "Здібності Привида".
 */
public class SpiritUpgradeScreen extends SpiritScreenBase {

    private static final int PATH_GAP = 8;
    private static final int PATH_W = (PANEL_W - 2 * MARGIN - 2 * PATH_GAP) / 3;

    private boolean builtUnlocked;
    private Button unlockButton;

    public SpiritUpgradeScreen() {
        super(Component.translatable("screen.spirit_form.title"), null);
    }

    @Override
    protected boolean showSettings() {
        return data().unlocked();
    }

    @Override
    protected void buildContent() {
        builtUnlocked = data().unlocked();
        unlockButton = null;

        if (!builtUnlocked) {
            unlockButton = addRenderableWidget(
                    Button.builder(Component.empty(), b -> send(UpgradeSpiritPayload.UNLOCK))
                            .bounds(left + (PANEL_W - 180) / 2, top + 110, 180, 20)
                            .build()
            );
            return;
        }

        for (SpiritBranch branch : SpiritBranch.values()) {
            int x = left + MARGIN + branch.ordinal() * (PATH_W + PATH_GAP);
            addRenderableWidget(
                    Button.builder(
                                    Component.translatable("path.spirit_form." + branch.id),
                                    b -> minecraft.setScreen(new SpiritBranchScreen(this, branch))
                            )
                            .bounds(x, top + 70, PATH_W, 24)
                            .build()
            );
        }

        addRenderableWidget(
                Button.builder(
                                Component.translatable("button.spirit_form.abilities"),
                                b -> minecraft.setScreen(new SpiritAbilitiesScreen(this))
                        )
                        .bounds(left + MARGIN, top + 130, PANEL_W - 2 * MARGIN, 24)
                        .build()
        );

        // Три панелі здібностей: пасивні (колесо T), активні (цифри) й основні (гарячі клавіші)
        int barGap = 6;
        int barW = (PANEL_W - 2 * MARGIN - 2 * barGap) / 3;
        SpiritLoadout.Bar[] bars = {SpiritLoadout.Bar.PASSIVE, SpiritLoadout.Bar.AUX, SpiritLoadout.Bar.MAIN};
        for (int i = 0; i < bars.length; i++) {
            SpiritLoadout.Bar bar = bars[i];
            addRenderableWidget(
                    Button.builder(
                                    Component.translatable("button.spirit_form.bar." + bar.key()),
                                    b -> minecraft.setScreen(new SpiritLoadoutScreen(this, bar))
                            )
                            .bounds(left + MARGIN + i * (barW + barGap), top + 176, barW, 24)
                            .build()
            );
        }
    }

    @Override
    protected void refresh() {
        if (data().unlocked() != builtUnlocked) {
            rebuildWidgets();
            return;
        }

        if (unlockButton != null) {
            LocalPlayer player = minecraft == null ? null : minecraft.player;
            int cost = SpiritBalance.cost(1);
            unlockButton.setMessage(Component.translatable("button.spirit_form.unlock", cost));
            unlockButton.active = player != null && canAfford(player, cost);
        }
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY) {
        SpiritData data = data();

        if (!data.unlocked()) {
            graphics.drawCenteredString(
                    font, Component.translatable("screen.spirit_form.base"), width / 2, top + 70, 0xFFFFFF);
            graphics.drawCenteredString(
                    font, Component.translatable("screen.spirit_form.base.closed"), width / 2, top + 84, 0xAAAAAA);
            return;
        }

        graphics.drawCenteredString(
                font, Component.translatable("screen.spirit_form.bars"), width / 2, top + 164, 0xAAAAAA);

        graphics.drawCenteredString(
                font, Component.translatable(
                        "screen.spirit_form.base.opened", Component.keybind("key.spirit_form.toggle_spirit")), width / 2, top + 44, 0xAAAAAA);

        for (SpiritBranch branch : SpiritBranch.values()) {
            int x = left + MARGIN + branch.ordinal() * (PATH_W + PATH_GAP);
            graphics.drawCenteredString(
                    font,
                    Component.literal(data.level(branch) + "/" + SpiritBalance.MAX_LEVEL),
                    x + PATH_W / 2, top + 98, 0xAAAAAA
            );
        }
    }
}
