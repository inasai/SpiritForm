package com.spiritform;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Основа всіх вікон прокачки: рамка, заголовок, рівні досвіду,
 * кнопка налаштувань зліва вгорі, кнопка закриття (X) справа вгорі та кнопка "Назад".
 */
public abstract class SpiritScreenBase extends Screen {

    protected static final int PANEL_W = 320;
    protected static final int PANEL_H = 240;
    protected static final int MARGIN = 14;

    protected final Screen parent;
    protected int left;
    protected int top;

    protected SpiritScreenBase(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** Чи показувати кнопку налаштувань (поки форма закрита, її немає). */
    protected boolean showSettings() {
        return true;
    }

    protected abstract void buildContent();

    protected abstract void refresh();

    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY);

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;

        addRenderableWidget(
                Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> closeAll())
                        .bounds(left + PANEL_W - 24, top + 4, 20, 20)
                        .build()
        );

        if (showSettings()) {
            addRenderableWidget(new IconButton(
                    left + 4, top + 4, 20, 20,
                    IconButton.Icon.GEAR,
                    b -> openSettings(),
                    Component.translatable("button.spirit_form.settings")
            ));
        }

        if (parent != null) {
            addRenderableWidget(
                    Button.builder(Component.translatable("button.spirit_form.back"), b -> onClose())
                            .bounds(left + MARGIN, top + PANEL_H - 28, 70, 20)
                            .build()
            );
        }

        buildContent();
        refresh();
    }

    @Override
    public void tick() {
        refresh();
    }

    /** Назад до попереднього вікна (або закрити, якщо воно головне). */
    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    protected void closeAll() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void openSettings() {
        ModContainer container = ModList.get().getModContainerById(SpiritFormMod.MODID).orElse(null);
        if (container != null && minecraft != null) {
            minecraft.setScreen(new ConfigurationScreen(container, this));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SpiritKeyMappings.OPEN_UPGRADES.matches(keyCode, scanCode)) {
            closeAll();
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

        graphics.drawCenteredString(font, title, width / 2, top + 8, 0xFFFFFF);

        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player != null) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.spirit_form.xp", player.experienceLevel),
                    width / 2, top + 20, 0xA0FF70
            );
        }

        renderContent(graphics, mouseX, mouseY);
    }

    protected static void send(int action) {
        PacketDistributor.sendToServer(new UpgradeSpiritPayload(action));
    }

    protected static boolean canAfford(LocalPlayer player, int cost) {
        return player.getAbilities().instabuild || player.experienceLevel >= cost;
    }

    protected SpiritData data() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        return player == null ? SpiritData.DEFAULT : SpiritFormState.get(player);
    }
}
