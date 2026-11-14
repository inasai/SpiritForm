package com.spiritform.client.screen;

import com.spiritform.client.SpiritAbilityUse;
import com.spiritform.client.SpiritKeyMappings;
import com.spiritform.client.gui.SpiritWheelUi;
import com.spiritform.data.SpiritData;
import com.spiritform.form.SpiritFormState;
import com.spiritform.vault.SpiritLoadout;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * Колесо здібностей (клавіша T). Показується, поки утримується клавіша. Має кілька сторінок (їх додають у вікні прокачки).
 * ЛКМ по здібності вмикає чи вимикає її (активну запускає), ПКМ поза центром закриває колесо.
 * Центр - кнопка сторінок: ЛКМ перегортає на наступну, ПКМ повертає на попередню (також колесо миші та стрілки).
 * Вигляд спільний з колесами Армії (SpiritWheelUi).
 */
public class SpiritWheelScreen extends Screen {

    /** Остання відкрита сторінка запам'ятовується до наступного відкриття колеса. */
    private static int lastPage;

    /** state: true/false для перемикачів, null для разової дії. */
    private record Entry(String id, Component name, String icon, Boolean state) {
    }

    private int page;

    public SpiritWheelScreen() {
        super(Component.translatable("screen.spirit_form.wheel"));
        page = lastPage;
    }

    private int pages() {
        return SpiritLoadout.passivePages();
    }

    private void clampPage() {
        page = Math.floorMod(page, pages());
        lastPage = page;
    }

    private void turn(int delta) {
        page = Math.floorMod(page + delta, pages());
        lastPage = page;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4F));
        }
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return list;
        }
        clampPage();

        SpiritData data = SpiritFormState.get(player);
        for (int i = 0; i < SpiritLoadout.Bar.PASSIVE.size; i++) {
            String id = SpiritLoadout.get(SpiritLoadout.Bar.PASSIVE, page, i);
            if (id.isEmpty() || !SpiritAbilityUse.owned(data, id)) {
                continue;
            }
            list.add(new Entry(id, SpiritLoadout.name(id), id, SpiritAbilityUse.state(minecraft, data, id)));
        }
        return list;
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

    @Override
    public void tick() {
        // Колесо живе, поки утримується клавіша
        if (!isWheelKeyDown()) {
            onClose();
        }
    }

    private static boolean isWheelKeyDown() {
        Minecraft minecraft = Minecraft.getInstance();
        KeyMapping key = SpiritKeyMappings.PASSIVE_WHEEL;
        InputConstants.Key bound = key.getKey();
        long window = minecraft.getWindow().getWindow();
        if (bound.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, bound.getValue());
    }

    private SpiritWheelUi.Geo geo() {
        return SpiritWheelUi.Geo.of(height);
    }

    private int hovered(int count, double mouseX, double mouseY) {
        return SpiritWheelUi.hit(geo(), count, mouseX - width / 2.0, mouseY - height / 2.0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<Entry> entries = entries();
        List<SpiritWheelUi.Slice> slices = new ArrayList<>();
        int toggles = 0;
        int active = 0;
        for (Entry entry : entries) {
            Component info = entry.state() == null
                    ? Component.translatable("screen.spirit_form.wheel.use")
                    : Component.translatable(entry.state() ? "screen.spirit_form.wheel.on" : "screen.spirit_form.wheel.off");
            slices.add(SpiritWheelUi.Slice.of(entry.name(), info, entry.icon(), Boolean.TRUE.equals(entry.state())));
            if (entry.state() != null) {
                toggles++;
                if (entry.state()) {
                    active++;
                }
            }
        }

        int raw = hovered(entries.size(), mouseX, mouseY);
        boolean multi = pages() > 1;
        boolean centerHot = raw == -2 && multi;
        int hover = raw < 0 ? -1 : raw;

        Component hint = Component.translatable("screen.spirit_form.wheel.hint");
        Component count = multi
                ? Component.translatable("screen.spirit_form.wheel.page", page + 1, pages())
                        .append(" · ").append(Component.translatable("screen.spirit_form.wheel.count", active, toggles))
                : Component.translatable("screen.spirit_form.wheel.count", active, toggles);

        SpiritWheelUi.Tip tip;
        if (centerHot) {
            tip = new SpiritWheelUi.Tip(
                    Component.translatable("screen.spirit_form.wheel.page", page + 1, pages()), null,
                    null, Component.translatable("screen.spirit_form.wheel.page_hint"));
        } else if (entries.isEmpty()) {
            tip = new SpiritWheelUi.Tip(title, multi ? Component.translatable("screen.spirit_form.wheel.page", page + 1, pages()) : null,
                    Component.translatable("screen.spirit_form.wheel.empty"),
                    multi ? Component.translatable("screen.spirit_form.wheel.page_hint") : null);
        } else if (hover >= 0) {
            tip = new SpiritWheelUi.Tip(slices.get(hover).name(), slices.get(hover).info(), null, hint);
        } else {
            tip = new SpiritWheelUi.Tip(title, count, null, hint);
        }
        SpiritWheelUi.Center center = multi
                ? new SpiritWheelUi.Center("form", null, Component.literal((page + 1) + "/" + pages()))
                : new SpiritWheelUi.Center("form", null, Component.literal(active + "/" + toggles));
        SpiritWheelUi.draw(graphics, font, width, height, geo(), slices, hover, center, centerHot, tip);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Entry> entries = entries();
        int hover = hovered(entries.size(), mouseX, mouseY);

        // Центр: ЛКМ далі, ПКМ назад
        if (hover == -2 && pages() > 1 && (button == 0 || button == 1)) {
            turn(button == 0 ? 1 : -1);
            return true;
        }
        if (button == 1) {
            onClose();
            return true;
        }
        if (button == 0 && hover >= 0) {
            Entry entry = entries.get(hover);
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            if (entry.state() == null) {
                // Разова дія: колесо закривається першим, бо дія може відкрити власне вікно (Армія)
                onClose();
            }
            SpiritAbilityUse.use(entry.id());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pages() > 1 && scrollY != 0.0) {
            turn(scrollY < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pages() > 1 && (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_LEFT)) {
            turn(keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
