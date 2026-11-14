package com.spiritform.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Ванільна кнопка вікон прокачки (ті самі спрайти, що й у меню гри).
 * Має той самий інтерфейс побудови, що й Button.builder, плюс primary() для головної дії (жовтий текст).
 */
public class SpiritButton extends Button {

    private boolean primary;

    private SpiritButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public static Builder of(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    private static final ResourceLocation NORMAL = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation DISABLED = ResourceLocation.withDefaultNamespace("widget/button_disabled");

    /** Малює ванільну кнопку (її використовує й IconButton). */
    public static void paint(GuiGraphics graphics, AbstractWidget widget, boolean primary) {
        ResourceLocation sprite = !widget.active ? DISABLED : widget.isHoveredOrFocused() ? HIGHLIGHTED : NORMAL;
        graphics.blitSprite(sprite, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        paint(graphics, this, primary);
        int color = !active ? 0xFFA0A0A0 : primary ? 0xFFFFFF55 : 0xFFFFFFFF;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
    }

    public static final class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private boolean primary;

        private Builder(Component message, Button.OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder primary() {
            this.primary = true;
            return this;
        }

        public SpiritButton build() {
            SpiritButton button = new SpiritButton(x, y, width, height, message, onPress);
            button.primary = primary;
            return button;
        }
    }
}
