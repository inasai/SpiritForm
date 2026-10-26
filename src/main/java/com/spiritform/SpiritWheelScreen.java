package com.spiritform;

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
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Колесо пасивних здібностей (клавіша T). Показується, поки утримується клавіша.
 * Клацання мишею по здібності вмикає або вимикає її.
 */
public class SpiritWheelScreen extends Screen {

    private static final int OUTER = 84;
    private static final int INNER = 28;
    private static final int ICON_RADIUS = 56;

    private record Entry(Component name, String icon, boolean on, int action) {
    }

    public SpiritWheelScreen() {
        super(Component.translatable("screen.spirit_form.wheel"));
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return list;
        }

        SpiritData data = SpiritFormState.get(player);
        for (int i = 0; i < SpiritLoadout.Bar.PASSIVE.size; i++) {
            String id = SpiritLoadout.get(SpiritLoadout.Bar.PASSIVE, i);
            if (id.isEmpty()) {
                continue;
            }

            Component name = SpiritLoadout.name(id);
            String icon = id;
            if (SpiritLoadout.INSTINCT.equals(id)) {
                if (data.unlocked()) {
                    list.add(new Entry(name, icon, data.instinct(), UpgradeSpiritPayload.TOGGLE_INSTINCT));
                }
            } else if (SpiritAbility.ICY_TOUCH.id.equals(id)) {
                if (data.hasAbility(SpiritAbility.ICY_TOUCH)) {
                    list.add(new Entry(name, icon, data.icy(), UpgradeSpiritPayload.TOGGLE_ICY));
                }
            } else if (SpiritAbility.PRESENCE.id.equals(id)) {
                if (data.hasAbility(SpiritAbility.PRESENCE)) {
                    list.add(new Entry(name, icon, data.presence(), UpgradeSpiritPayload.TOGGLE_PRESENCE));
                }
            } else if (SpiritAbility.ECHO.id.equals(id)) {
                if (data.hasAbility(SpiritAbility.ECHO)) {
                    list.add(new Entry(name, icon, data.echo(), UpgradeSpiritPayload.TOGGLE_ECHO));
                }
            } else if (SpiritAbility.GAZE.id.equals(id)) {
                if (data.hasAbility(SpiritAbility.GAZE)) {
                    list.add(new Entry(name, icon, data.gaze(), UpgradeSpiritPayload.TOGGLE_GAZE));
                }
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

    /** Номер сектора під курсором або -1. */
    private int hovered(int count, double mouseX, double mouseY) {
        if (count <= 0) {
            return -1;
        }
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < INNER || distance > OUTER + 30) {
            return -1;
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        double sector = 360.0 / count;
        angle = ((angle + sector / 2.0) % 360.0 + 360.0) % 360.0;
        return Math.min(count - 1, (int) (angle / sector));
    }

    private static void disk(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt((double) radius * radius - (double) dy * dy);
            graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<Entry> entries = entries();
        int cx = width / 2;
        int cy = height / 2;

        disk(graphics, cx, cy, OUTER, 0x90101018);
        disk(graphics, cx, cy, INNER, 0xC0000000);

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.spirit_form.wheel.empty"),
                    cx, cy - 4, 0xAAAAAA);
            return;
        }

        int hover = hovered(entries.size(), mouseX, mouseY);
        double sector = 360.0 / entries.size();

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            double angle = Math.toRadians(-90.0 + i * sector);
            int x = cx + (int) Math.round(Math.cos(angle) * ICON_RADIUS);
            int y = cy + (int) Math.round(Math.sin(angle) * ICON_RADIUS);

            if (i == hover) {
                disk(graphics, x, y, 24, 0x80FFFFFF);
            }
            disk(graphics, x, y, 19, entry.on() ? 0xA040C040 : 0xA0404040);
            SpiritIcons.draw(graphics, entry.icon(), x - 8, y - 8);
        }

        if (hover >= 0) {
            Entry entry = entries.get(hover);
            graphics.drawCenteredString(font, entry.name(), cx, cy - 9, 0xFFFFFF);
            graphics.drawCenteredString(
                    font,
                    Component.translatable(entry.on() ? "screen.spirit_form.wheel.on" : "screen.spirit_form.wheel.off"),
                    cx, cy + 2, entry.on() ? 0x80FF80 : 0xFF8080);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Entry> entries = entries();
        int hover = hovered(entries.size(), mouseX, mouseY);
        if (button == 0 && hover >= 0) {
            PacketDistributor.sendToServer(new UpgradeSpiritPayload(entries.get(hover).action()));
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
