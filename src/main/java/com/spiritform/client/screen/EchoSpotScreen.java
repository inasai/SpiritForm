package com.spiritform.client.screen;

import com.spiritform.Config;
import com.spiritform.ability.EchoTravel;
import com.spiritform.client.gui.SpiritWheelUi;
import com.spiritform.data.EchoSpots;
import com.spiritform.data.SpiritAttachments;
import com.spiritform.network.EchoSpotPayload;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Колесо Відлунь Смерті (відкривається, коли гравець лишив кілька Відлунь). Кожен сектор показує назву й координати.
 * ЛКМ по Відлунню переносить до нього, ЛКМ по секторові «Нове» лишає ще одне Відлуння там, де стоїть гравець,
 * Shift+ПКМ перейменовує, ПКМ чи E закриває колесо. Вигляд спільний з іншими колесами (SpiritWheelUi).
 */
public class EchoSpotScreen extends Screen {

    private static final int NEW = -1;

    public EchoSpotScreen() {
        super(Component.translatable("screen.spirit_form.echo_spots.title"));
    }

    private EchoSpots spots() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        return player == null ? EchoSpots.EMPTY : player.getData(SpiritAttachments.ECHO_SPOTS.get());
    }

    private int limit() {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        return player == null ? 1 : Math.min(player.getData(SpiritAttachments.ECHO_LIMIT.get()), Config.ECHO_LIMIT.get());
    }

    private static Component nameOf(EchoSpots.Spot spot, int number) {
        return spot.name().isEmpty()
                ? Component.translatable("screen.spirit_form.echo_spots.default", number + 1)
                : Component.literal(spot.name());
    }

    private static Component coords(BlockPos pos) {
        return Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    private static Component dimension(EchoSpots.Spot spot) {
        ResourceLocation id = ResourceLocation.tryParse(spot.dimension());
        return id == null ? Component.literal(spot.dimension()) : EchoTravel.dimensionName(id);
    }

    private boolean canAdd(EchoSpots spots) {
        return spots.spots().size() < limit();
    }

    /** Сектори: усі Відлуння, а в кінці «Нове», якщо ще є вільне місце. */
    private int count(EchoSpots spots) {
        return spots.spots().size() + (canAdd(spots) ? 1 : 0);
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

    private int hovered(int count, double mouseX, double mouseY) {
        return SpiritWheelUi.hit(geo(), count, mouseX - width / 2.0, mouseY - height / 2.0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        EchoSpots spots = spots();
        List<SpiritWheelUi.Slice> slices = new ArrayList<>();
        for (int i = 0; i < spots.spots().size(); i++) {
            EchoSpots.Spot spot = spots.spots().get(i);
            slices.add(new SpiritWheelUi.Slice(nameOf(spot, i), coords(spot.pos()), null,
                    new ItemStack(Items.ECHO_SHARD), false, false, false));
        }
        if (canAdd(spots)) {
            slices.add(new SpiritWheelUi.Slice(Component.translatable("screen.spirit_form.echo_spots.new"),
                    Component.translatable("screen.spirit_form.echo_spots.new_info"), null,
                    new ItemStack(Items.AMETHYST_SHARD), false, false, false));
        }

        int raw = hovered(slices.size(), mouseX, mouseY);
        int hover = raw < 0 ? -1 : raw;
        Component count = Component.translatable("screen.spirit_form.echo_spots.count", spots.spots().size(), limit());

        SpiritWheelUi.Tip tip;
        if (slices.isEmpty()) {
            tip = new SpiritWheelUi.Tip(title, null, Component.translatable("screen.spirit_form.echo_spots.empty"), null);
        } else if (hover >= 0 && hover < spots.spots().size()) {
            EchoSpots.Spot spot = spots.spots().get(hover);
            tip = new SpiritWheelUi.Tip(nameOf(spot, hover), coords(spot.pos()), dimension(spot),
                    Component.translatable("screen.spirit_form.echo_spots.hint"));
        } else if (hover >= 0) {
            tip = new SpiritWheelUi.Tip(slices.get(hover).name(), slices.get(hover).info(), null,
                    Component.translatable("screen.spirit_form.echo_spots.new_hint"));
        } else {
            tip = new SpiritWheelUi.Tip(title, count, null, Component.translatable("screen.spirit_form.echo_spots.hint"));
        }
        SpiritWheelUi.Center center = new SpiritWheelUi.Center("echo", null,
                Component.literal(spots.spots().size() + "/" + limit()));
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

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        EchoSpots spots = spots();
        int hover = hovered(count(spots), mouseX, mouseY);
        boolean onSpot = hover >= 0 && hover < spots.spots().size();

        if (button == 1) {
            if (onSpot && hasShiftDown() && minecraft != null) {
                EchoSpots.Spot spot = spots.spots().get(hover);
                click();
                minecraft.setScreen(new EchoRenameScreen(hover, spot.name()));
                return true;
            }
            onClose();
            return true;
        }
        if (button == 0 && hover >= 0) {
            click();
            if (onSpot) {
                PacketDistributor.sendToServer(new EchoSpotPayload(EchoSpotPayload.GO, hover, ""));
            } else {
                PacketDistributor.sendToServer(new EchoSpotPayload(EchoSpotPayload.PLACE, NEW, ""));
            }
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
