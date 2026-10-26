package com.spiritform;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Колеса армії: виклик душ (центр викликає всіх), накази та видалення душ.
 * Закривається після вибору або клавішею Esc.
 */
public class ArmyWheelScreen extends Screen {

    private enum Kind {
        SUMMON,
        ORDERS,
        SOULS
    }

    private record Entry(Component name, Component sub, String icon, ItemStack stack, boolean on, Runnable action) {
    }

    private static final int OUTER = 104;
    private static final int INNER = 30;
    private static final int ICON_RADIUS = 70;

    private final Kind kind;
    private int pendingDelete = -1;

    private ArmyWheelScreen(Kind kind, String title) {
        super(Component.translatable(title));
        this.kind = kind;
    }

    public static ArmyWheelScreen summon() {
        return new ArmyWheelScreen(Kind.SUMMON, "screen.spirit_form.army.summon");
    }

    public static ArmyWheelScreen orders() {
        return new ArmyWheelScreen(Kind.ORDERS, "screen.spirit_form.army.orders");
    }

    public static ArmyWheelScreen souls() {
        return new ArmyWheelScreen(Kind.SOULS, "screen.spirit_form.army.souls");
    }

    private static void send(int action, String arg) {
        PacketDistributor.sendToServer(new ArmyActionPayload(action, arg));
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        if (kind == Kind.ORDERS) {
            String[] ids = {"guard", "attack", "hold", "fury"};
            for (int i = 0; i < ids.length; i++) {
                final int index = i;
                list.add(new Entry(
                        Component.translatable("order.spirit_form." + ids[i]),
                        Component.translatable("order.spirit_form." + ids[i] + ".desc"),
                        "order_" + ids[i], ItemStack.EMPTY,
                        i == SpiritArmyClient.order(),
                        () -> send(SpiritArmy.ACT_ORDER, String.valueOf(index))));
            }
            return list;
        }

        for (int i = 0; i < SpiritArmyClient.souls().size(); i++) {
            final int index = i;
            ArmySyncPayload.Entry soul = SpiritArmyClient.souls().get(i);
            Component name = soul.name().isEmpty() ? SpiritArmy.typeName(soul.type()) : Component.literal(soul.name());
            Component sub = Component.literal("♥ " + soul.hp()
                    + (soul.summoned() ? " · " : ""))
                    .append(soul.summoned() ? Component.translatable("screen.spirit_form.army.summoned") : Component.empty());
            ItemStack stack = ItemStack.EMPTY;
            SpawnEggItem egg = EntityType.byString(soul.type()).map(SpawnEggItem::byId).orElse(null);
            if (egg != null) {
                stack = new ItemStack(egg);
            }
            ItemStack custom = SpiritArmyIcons.get(soul.id());
            if (!custom.isEmpty()) {
                stack = custom;
            }
            Runnable action;
            if (kind == Kind.SUMMON) {
                action = () -> send(SpiritArmy.ACT_SUMMON, soul.id().toString());
            } else {
                action = () -> send(SpiritArmy.ACT_DELETE, soul.id().toString());
            }
            list.add(new Entry(name, sub, "soul", stack, soul.summoned(), action));
        }
        return list;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** -2 центр, -1 ніщо, інакше номер сектора. */
    private int hovered(int count, double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < INNER) {
            return -2;
        }
        if (count <= 0 || distance > OUTER + 30) {
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
        graphics.drawCenteredString(font, title, cx, cy - OUTER - 26, 0x8CFFC8);

        int hover = hovered(entries.size(), mouseX, mouseY);

        if (kind == Kind.SUMMON) {
            if (hover == -2) {
                disk(graphics, cx, cy, INNER, 0x6040FFA0);
            }
            graphics.drawCenteredString(font, Component.translatable("screen.spirit_form.army.all"), cx, cy - 4, 0xFFFFFF);
        }

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.spirit_form.army.empty"),
                    cx, cy + OUTER + 8, 0xAAAAAA);
            return;
        }

        double sector = 360.0 / entries.size();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            double angle = Math.toRadians(-90.0 + i * sector);
            int x = cx + (int) Math.round(Math.cos(angle) * ICON_RADIUS);
            int y = cy + (int) Math.round(Math.sin(angle) * ICON_RADIUS);

            if (i == hover) {
                disk(graphics, x, y, 22, 0x80FFFFFF);
            }
            int base = kind == Kind.SOULS
                    ? (i == pendingDelete ? 0xC0D03030 : 0xA0603030)
                    : (entry.on() ? 0xA040C080 : 0xA0404040);
            disk(graphics, x, y, 18, base);
            if (!entry.stack().isEmpty()) {
                graphics.renderItem(entry.stack(), x - 8, y - 8);
            } else {
                SpiritIcons.draw(graphics, entry.icon(), x - 8, y - 8);
            }
        }

        if (hover >= 0) {
            Entry entry = entries.get(hover);
            graphics.drawCenteredString(font, entry.name(), cx, cy + OUTER + 8, 0xFFFFFF);
            Component sub = kind == Kind.SOULS && hover == pendingDelete
                    ? Component.translatable("screen.spirit_form.army.confirm")
                    : entry.sub();
            graphics.drawCenteredString(font, sub, cx, cy + OUTER + 20, kind == Kind.SOULS ? 0xFF8080 : 0xAAFFD0);
            if (kind != Kind.ORDERS) {
                graphics.drawCenteredString(font, Component.translatable("screen.spirit_form.army.icon_hint"),
                        cx, cy + OUTER + 32, 0x888888);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Entry> entries = entries();
        int hover = hovered(entries.size(), mouseX, mouseY);
        if (button == 1 && kind != Kind.ORDERS && hover >= 0 && hover < SpiritArmyClient.souls().size()) {
            // ПКМ по душі: змінити іконку
            if (minecraft != null) {
                minecraft.setScreen(new ArmyIconScreen(this, SpiritArmyClient.souls().get(hover).id()));
            }
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (hover == -2 && kind == Kind.SUMMON) {
            send(SpiritArmy.ACT_SUMMON, "");
            finish();
            return true;
        }
        if (hover >= 0) {
            if (kind == Kind.SOULS && pendingDelete != hover) {
                pendingDelete = hover;
                click();
                return true;
            }
            entries.get(hover).action().run();
            finish();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void finish() {
        click();
        onClose();
    }
}
