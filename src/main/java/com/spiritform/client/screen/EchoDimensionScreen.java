package com.spiritform.client.screen;

import com.spiritform.ability.EchoTravel;
import com.spiritform.client.gui.SpiritWheelUi;
import com.spiritform.data.SpiritAttachments;
import com.spiritform.form.SpiritBeyond;
import com.spiritform.network.EchoTravelPayload;
import com.spiritform.registry.SpiritItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Колесо вибору виміру (ПКМ по Відлунню Смерті в колесі пасивних здібностей). Показує лише виміри, де гравець
 * уже бував. ЛКМ по виміру переносить туди (сервер перевіряє все сам), ПКМ чи Esc закриває колесо.
 * Поточний вимір підсвічений і недоступний. Вигляд спільний з іншими колесами (SpiritWheelUi).
 */
public class EchoDimensionScreen extends Screen {

    private record Entry(ResourceLocation dimension, SpiritWheelUi.Slice slice, boolean current) {
    }

    public EchoDimensionScreen() {
        super(Component.translatable("screen.spirit_form.echo.title"));
    }

    private static int order(ResourceLocation id) {
        if (Level.OVERWORLD.location().equals(id)) {
            return 0;
        }
        if (Level.NETHER.location().equals(id)) {
            return 1;
        }
        if (Level.END.location().equals(id)) {
            return 2;
        }
        return SpiritBeyond.KEY.location().equals(id) ? 3 : 4;
    }

    private static ItemStack icon(ResourceLocation id) {
        if (Level.OVERWORLD.location().equals(id)) {
            return new ItemStack(Items.GRASS_BLOCK);
        }
        if (Level.NETHER.location().equals(id)) {
            return new ItemStack(Items.NETHERRACK);
        }
        if (Level.END.location().equals(id)) {
            return new ItemStack(Items.END_STONE);
        }
        if (SpiritBeyond.KEY.location().equals(id)) {
            return new ItemStack(SpiritItems.CRYSTAL_SOUL.get());
        }
        return new ItemStack(Items.ENDER_EYE);
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return list;
        }
        Set<ResourceKey<Level>> known = player.connection.levels();
        ResourceLocation current = player.level().dimension().location();
        List<ResourceLocation> dimensions = new ArrayList<>();
        for (String text : player.getData(SpiritAttachments.VISITED_DIMENSIONS.get())) {
            ResourceLocation id = ResourceLocation.tryParse(text);
            if (id != null && (known.contains(ResourceKey.create(Registries.DIMENSION, id)))) {
                dimensions.add(id);
            }
        }
        dimensions.sort(Comparator.comparingInt(EchoDimensionScreen::order).thenComparing(ResourceLocation::toString));
        for (ResourceLocation id : dimensions) {
            boolean here = id.equals(current);
            Component info = Component.translatable(here ? "screen.spirit_form.echo.here" : "screen.spirit_form.echo.go");
            list.add(new Entry(id,
                    new SpiritWheelUi.Slice(EchoTravel.dimensionName(id), info, null, icon(id), here, false, false), here));
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
        for (Entry entry : entries) {
            slices.add(entry.slice());
        }
        int raw = hovered(entries.size(), mouseX, mouseY);
        int hover = raw < 0 ? -1 : raw;
        Component hint = Component.translatable("screen.spirit_form.echo.hint");

        SpiritWheelUi.Tip tip;
        if (entries.isEmpty()) {
            tip = new SpiritWheelUi.Tip(title, null, Component.translatable("screen.spirit_form.echo.empty"), null);
        } else if (hover >= 0) {
            tip = new SpiritWheelUi.Tip(slices.get(hover).name(), slices.get(hover).info(), null, hint);
        } else {
            tip = new SpiritWheelUi.Tip(title, Component.literal(String.valueOf(entries.size())), null, hint);
        }
        SpiritWheelUi.Center center = new SpiritWheelUi.Center("echo", null, Component.literal(String.valueOf(entries.size())));
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
        List<Entry> entries = entries();
        int hover = hovered(entries.size(), mouseX, mouseY);
        if (button == 1) {
            onClose();
            return true;
        }
        if (button == 0 && hover >= 0) {
            Entry entry = entries.get(hover);
            if (entry.current()) {
                return true;
            }
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            PacketDistributor.sendToServer(new EchoTravelPayload(entry.dimension().toString()));
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
