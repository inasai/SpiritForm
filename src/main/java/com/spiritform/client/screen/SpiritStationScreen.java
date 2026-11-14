package com.spiritform.client.screen;

import com.spiritform.client.gui.SpiritUi;
import com.spiritform.client.gui.SpiritWheelUi;
import com.spiritform.data.StationData;
import com.spiritform.network.StationActionPayload;
import com.spiritform.vault.SpiritLoadout;
import com.spiritform.vault.SpiritStations;
import com.spiritform.vault.Station;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Колесо станцій (здібність Самодостатність). Три сторінки по чотири станції; їх розставляють у вікні прокачки.
 * ЛКМ по станції відкриває її ванільне вікно. Shift+ЛКМ по закритій станції відкриває її за рівні досвіду.
 * Центр: ЛКМ - наступна сторінка, ПКМ - попередня, Shift+ЛКМ - відкрити нову сторінку за рівні.
 * ПКМ поза центром закриває колесо. Мітка біля комірки означає, що в станції лежить готовий вміст.
 */
public class SpiritStationScreen extends Screen {

    private static int lastPage;

    private record Entry(Station station, String id, boolean pageOpen, boolean unlocked, boolean ready) {
    }

    private int page;

    public SpiritStationScreen() {
        super(Component.translatable("screen.spirit_form.stations"));
        SpiritLoadout.syncExternal(Station.clientBlocks());
        page = Math.min(lastPage, Station.pages() - 1);
    }

    private static int pageCount() {
        return Station.pages();
    }

    private void turn(int delta) {
        page = Math.floorMod(page + delta, pageCount());
        lastPage = page;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4F));
        }
    }

    private StationData data() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        return player == null ? StationData.EMPTY : SpiritStations.get(player);
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        StationData data = data();
        boolean pageOpen = page < data.pages();
        for (int i = 0; i < SpiritLoadout.Bar.STATION.size; i++) {
            String id = SpiritLoadout.get(SpiritLoadout.Bar.STATION, page, i);
            Station station = Station.byLoadoutId(id);
            if (station != null && station.available()) {
                list.add(new Entry(station, id, pageOpen, data.has(station), data.isReady(station)));
            }
        }
        return list;
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
    public void removed() {
        SpiritWheelUi.release();
    }

    private SpiritWheelUi.Geo geo() {
        return SpiritWheelUi.Geo.of(height);
    }

    private int hovered(int count, double mouseX, double mouseY) {
        return SpiritWheelUi.hit(geo(), count, mouseX - width / 2.0, mouseY - height / 2.0);
    }

    private Component stateInfo(StationData data, Entry entry) {
        if (!entry.pageOpen()) {
            return Component.translatable("screen.spirit_form.station.page_locked");
        }
        if (!entry.unlocked()) {
            return data.unlockedCount() >= SpiritStations.capacity(data)
                    ? Component.translatable("screen.spirit_form.station.need_page")
                    : Component.translatable("screen.spirit_form.station.buy", data.nextStationCost());
        }
        return Component.translatable(entry.ready() ? "screen.spirit_form.station.ready" : "screen.spirit_form.station.open");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        StationData data = data();
        List<Entry> entries = entries();
        List<SpiritWheelUi.Slice> slices = new ArrayList<>();
        for (Entry entry : entries) {
            boolean usable = entry.pageOpen() && entry.unlocked();
            slices.add(new SpiritWheelUi.Slice(entry.station().displayName(), stateInfo(data, entry), null,
                    entry.station().stack(), usable && entry.ready(), !usable, false));
        }

        int raw = hovered(entries.size(), mouseX, mouseY);
        int hover = raw < 0 ? -1 : raw;
        boolean centerHot = raw == -2;
        Component pageLabel = Component.translatable("screen.spirit_form.wheel.page", page + 1, pageCount());

        SpiritWheelUi.Tip tip;
        if (centerHot) {
            Component detail = data.pages() < Station.pages()
                    ? Component.translatable("screen.spirit_form.station.page_buy", SpiritStations.pageCost(data.pages()))
                    : null;
            tip = new SpiritWheelUi.Tip(pageLabel, page < data.pages() ? null
                    : Component.translatable("screen.spirit_form.station.page_locked"),
                    detail, Component.translatable("screen.spirit_form.wheel.page_hint"));
        } else if (hover >= 0) {
            tip = new SpiritWheelUi.Tip(slices.get(hover).name(), slices.get(hover).info(), null,
                    Component.translatable("screen.spirit_form.station.hint"));
        } else if (entries.isEmpty()) {
            tip = new SpiritWheelUi.Tip(title, pageLabel,
                    Component.translatable("screen.spirit_form.station.empty"), null);
        } else {
            tip = new SpiritWheelUi.Tip(title, pageLabel, null, Component.translatable("screen.spirit_form.station.hint"));
        }

        SpiritWheelUi.Center center = new SpiritWheelUi.Center("self_sufficiency", null,
                Component.literal((page + 1) + "/" + pageCount()));
        SpiritWheelUi.draw(graphics, font, width, height, geo(), slices, hover, center, centerHot, tip);

        // Мітка біля комірки, де лежить готовий вміст
        int count = entries.size();
        for (int i = 0; i < count; i++) {
            Entry entry = entries.get(i);
            if (!(entry.pageOpen() && entry.unlocked() && entry.ready())) {
                continue;
            }
            double angle = Math.toRadians(-90.0 + i * (360.0 / count));
            int radius = geo().outer() - 9;
            int x = width / 2 + (int) Math.round(Math.cos(angle) * radius);
            int y = height / 2 + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x - 4, y - 4, x + 4, y + 4, 0xFF04110C);
            graphics.fill(x - 3, y - 3, x + 3, y + 3, SpiritUi.ACCENT);
        }
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Entry> entries = entries();
        int hover = hovered(entries.size(), mouseX, mouseY);
        boolean shift = hasShiftDown();

        if (hover == -2 && (button == 0 || button == 1)) {
            if (button == 0 && shift) {
                PacketDistributor.sendToServer(new StationActionPayload(StationActionPayload.BUY_PAGE, 0));
                click();
            } else {
                turn(button == 0 ? 1 : -1);
            }
            return true;
        }
        if (button == 1) {
            onClose();
            return true;
        }
        if (button == 0 && hover >= 0) {
            Entry entry = entries.get(hover);
            if (!entry.pageOpen()) {
                return true;
            }
            if (!entry.unlocked()) {
                if (shift) {
                    PacketDistributor.sendToServer(
                            new StationActionPayload(StationActionPayload.BUY_STATION, entry.station().ordinal()));
                    click();
                }
                return true;
            }
            PacketDistributor.sendToServer(new StationActionPayload(StationActionPayload.OPEN, entry.station().ordinal()));
            click();
            // Колесо закривається, а сервер відкриває вікно станції
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0) {
            turn(scrollY < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_LEFT) {
            turn(keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
