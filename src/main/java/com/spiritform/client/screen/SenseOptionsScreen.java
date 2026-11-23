package com.spiritform.client.screen;

import com.spiritform.client.SpiritClientHighlight;
import com.spiritform.client.gui.SpiritWheelUi;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Налаштування Чуття Привида (ПКМ по Чуттю в колесі пасивних): три підсвічування вмикаються окремо - контури
 * (м'ятне обведення об'єктів), сховища крізь стіни й моби. ЛКМ перемикає, ПКМ чи E закриває колесо.
 * Вибір зберігається в клієнтському конфігу.
 */
public class SenseOptionsScreen extends Screen {

    private static final int OUTLINES = 0;
    private static final int CHESTS = 1;
    private static final int MOBS = 2;
    private static final int COUNT = 3;

    public SenseOptionsScreen() {
        super(Component.translatable("screen.spirit_form.sense.title"));
    }

    private static boolean on(int option) {
        return switch (option) {
            case OUTLINES -> SpiritClientHighlight.outlines();
            case CHESTS -> SpiritClientHighlight.chests();
            default -> SpiritClientHighlight.mobs();
        };
    }

    private static String key(int option) {
        return switch (option) {
            case OUTLINES -> "outlines";
            case CHESTS -> "chests";
            default -> "mobs";
        };
    }

    private static ItemStack icon(int option) {
        return new ItemStack(switch (option) {
            case OUTLINES -> Items.ENDER_EYE;
            case CHESTS -> Items.CHEST;
            default -> Items.ZOMBIE_HEAD;
        });
    }

    @Override
    public void removed() {
        SpiritWheelUi.release();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Світ лишається видимим, тло не затемнюємо. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private SpiritWheelUi.Geo geo() {
        return SpiritWheelUi.Geo.of(height);
    }

    private int hovered(double mouseX, double mouseY) {
        return SpiritWheelUi.hit(geo(), COUNT, mouseX - width / 2.0, mouseY - height / 2.0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<SpiritWheelUi.Slice> slices = new ArrayList<>();
        int active = 0;
        for (int i = 0; i < COUNT; i++) {
            boolean enabled = on(i);
            if (enabled) {
                active++;
            }
            slices.add(new SpiritWheelUi.Slice(
                    Component.translatable("screen.spirit_form.sense." + key(i)),
                    Component.translatable(enabled ? "screen.spirit_form.wheel.on" : "screen.spirit_form.wheel.off"),
                    null, icon(i), enabled, false, false));
        }
        int raw = hovered(mouseX, mouseY);
        int hover = raw < 0 ? -1 : raw;
        Component hint = Component.translatable("screen.spirit_form.sense.hint");

        SpiritWheelUi.Tip tip = hover >= 0
                ? new SpiritWheelUi.Tip(slices.get(hover).name(), slices.get(hover).info(),
                        Component.translatable("screen.spirit_form.sense." + key(hover) + ".desc"), hint)
                : new SpiritWheelUi.Tip(title, Component.translatable("screen.spirit_form.wheel.count", active, COUNT),
                        null, hint);
        SpiritWheelUi.Center center = new SpiritWheelUi.Center("sense", null, Component.literal(active + "/" + COUNT));
        SpiritWheelUi.draw(graphics, font, width, height, geo(), slices, hover, center, false, tip);
    }

    /** Колесо закривається й клавішею інвентарю (за замовчуванням E). */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SpiritWheelUi.isInventoryKey(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int hover = hovered(mouseX, mouseY);
        if (button == 1) {
            onClose();
            return true;
        }
        if (button == 0 && hover >= 0) {
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            SpiritClientHighlight.toggleOption(hover);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
