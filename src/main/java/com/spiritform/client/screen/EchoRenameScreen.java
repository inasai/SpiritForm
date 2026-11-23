package com.spiritform.client.screen;

import com.spiritform.data.EchoSpots;
import com.spiritform.network.EchoSpotPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Введення нової назви Відлуння (Shift+ПКМ у колесі Відлунь). Enter зберігає, Esc скасовує; порожня назва повертає стандартну. */
public class EchoRenameScreen extends Screen {

    private final int number;
    private final String current;
    private EditBox box;

    public EchoRenameScreen(int number, String current) {
        super(Component.translatable("screen.spirit_form.echo_spots.rename"));
        this.number = number;
        this.current = current;
    }

    @Override
    protected void init() {
        box = new EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, title);
        box.setMaxLength(EchoSpots.NAME_MAX);
        box.setValue(current);
        addRenderableWidget(box);
        setInitialFocus(box);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 28, 0xFF8CFFC8);
        graphics.drawCenteredString(font, Component.translatable("screen.spirit_form.echo_spots.rename_hint"),
                width / 2, height / 2 + 18, 0xFF8FB5A5);
    }

    private void back() {
        if (minecraft != null) {
            minecraft.setScreen(new EchoSpotScreen());
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            PacketDistributor.sendToServer(new EchoSpotPayload(EchoSpotPayload.RENAME, number, box.getValue()));
            back();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            back();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
