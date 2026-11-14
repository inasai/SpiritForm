package com.spiritform.client.screen;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.SpiritAnim;
import com.spiritform.client.SpiritKeyMappings;
import com.spiritform.client.gui.IconButton;
import com.spiritform.client.gui.SpiritButton;
import com.spiritform.client.gui.SpiritUi;
import com.spiritform.data.SpiritData;
import com.spiritform.form.SpiritFormState;
import com.spiritform.network.UpgradeSpiritPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Основа всіх вікон прокачки: ванільна панель, заголовок, рівні досвіду, анімація відкриття й закриття,
 * кнопка налаштувань зліва вгорі, кнопка закриття (X) справа вгорі та кнопка "Назад".
 */
public abstract class SpiritScreenBase extends Screen {

    protected static final int PANEL_W = 320;
    protected static final int PANEL_H = 240;
    protected static final int MARGIN = 14;

    protected final Screen parent;
    protected final SpiritAnim anim = new SpiritAnim();
    protected int left;
    protected int top;

    /** Розмір вікна. Підкласи можуть зробити його більшим. */
    protected int panelWidth() {
        return PANEL_W;
    }

    protected int panelHeight() {
        return PANEL_H;
    }

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
        SpiritUi.clearCache();
        anim.reopen();
        left = (width - panelWidth()) / 2;
        top = (height - panelHeight()) / 2;

        addRenderableWidget(
                SpiritButton.of(Component.literal("X").withStyle(ChatFormatting.RED), b -> closeAll())
                        .bounds(left + panelWidth() - 24, top + 3, 20, 20)
                        .build()
        );

        if (showSettings()) {
            addRenderableWidget(new IconButton(
                    left + 4, top + 3, 20, 20,
                    IconButton.Icon.GEAR,
                    b -> openSettings(),
                    Component.translatable("button.spirit_form.settings")
            ));
        }

        if (parent != null) {
            addRenderableWidget(
                    SpiritButton.of(Component.translatable("button.spirit_form.back"), b -> onClose())
                            .bounds(left + MARGIN, top + panelHeight() - 28, 70, 20)
                            .build()
            );
        }

        buildContent();
        refresh();
    }

    @Override
    public void tick() {
        anim.tick();
        refresh();
    }

    /** Вікно відкривається чи закривається: клацання й клавіші тоді ігноруються. */
    protected boolean busy() {
        return anim.busy();
    }

    /** Назад до попереднього вікна (або закрити, якщо воно головне), з анімацією закриття. */
    @Override
    public void onClose() {
        anim.close(() -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        });
    }

    protected void closeAll() {
        anim.close(() -> {
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return busy() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return busy() || super.mouseReleased(mouseX, mouseY, button);
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
        if (anim.closing()) {
            return true;
        }
        // Вікно закривається і клавішею відкриття меню (H), і клавішею інвентарю (E)
        if (SpiritKeyMappings.OPEN_UPGRADES.matches(keyCode, scanCode)
                || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            closeAll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Затемнення малює render (з анімацією), тож тут лише панель. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        SpiritUi.panel(graphics, left, top, panelWidth(), panelHeight());
        // Гравійована лінія під заголовком
        SpiritUi.engraved(graphics, left + 6, top + 26, panelWidth() - 12);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        anim.begin(graphics, width, height, left + panelWidth() / 2, top + panelHeight() / 2);

        super.render(graphics, mouseX, mouseY, partialTick);

        int titleX = left + (showSettings() ? 30 : 10);
        SpiritUi.text(graphics, font, title, titleX, top + 9, SpiritUi.TEXT);

        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player != null) {
            Component xp = Component.translatable("screen.spirit_form.xp", player.experienceLevel);
            SpiritUi.text(graphics, font, xp, left + panelWidth() - 30 - font.width(xp), top + 9, SpiritUi.TEXT_OK);
        }

        renderContent(graphics, mouseX, mouseY);
        anim.end(graphics);
    }

    protected static void send(int action) {
        PacketDistributor.sendToServer(new UpgradeSpiritPayload(action));
    }

    protected static boolean canAfford(LocalPlayer player, int cost) {
        return player.getAbilities().instabuild || player.experienceLevel >= cost;
    }

    /** Шрифт для допоміжних класів вікна (поле font у Screen захищене). */
    net.minecraft.client.gui.Font textFont() {
        return font;
    }

    protected SpiritData data() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        return player == null ? SpiritData.DEFAULT : SpiritFormState.get(player);
    }
}
