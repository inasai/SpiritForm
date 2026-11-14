package com.spiritform.client.screen;

import com.spiritform.army.SpiritArmy;
import com.spiritform.client.SpiritArmyClient;
import com.spiritform.client.gui.SpiritArmyIcons;
import com.spiritform.client.gui.SpiritWheelUi;
import com.spiritform.form.SpiritFormState;
import com.spiritform.network.ArmyActionPayload;
import com.spiritform.network.ArmySyncPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Меню Армії Потойбіччя у вигляді колеса (відкривається здібністю Армії).
 * Головне колесо: у центрі кнопка «Викликати всіх» або, коли хоч одну душу викликано, «Відкликати всіх»;
 * по колу три пункти: Виклик, Накази, Видалення. Кожен пункт відкриває своє колесо.
 * ПКМ повертає до попереднього колеса, а з головного закриває меню. Shift + ПКМ по душі змінює її іконку.
 */
public class ArmyWheelScreen extends Screen {

    private enum Kind {
        MAIN,
        SUMMON,
        ORDERS,
        DELETE
    }

    private static final java.util.Map<String, ItemStack> EGGS = new java.util.HashMap<>();
    private static final String[] ORDER_IDS = {"guard", "attack", "hold", "fury"};

    /** Сектор колеса: вигляд, душа (для коліс душ) і номер (для наказів і головного колеса). */
    private record Entry(SpiritWheelUi.Slice slice, ArmySyncPayload.Entry soul, int index) {
    }

    private final Kind kind;
    private final Screen parent;
    private UUID pendingDelete;

    private ArmyWheelScreen(Kind kind, Screen parent, String title) {
        super(Component.translatable(title));
        this.kind = kind;
        this.parent = parent;
    }

    public static ArmyWheelScreen main() {
        return new ArmyWheelScreen(Kind.MAIN, null, "screen.spirit_form.army.title");
    }

    private ArmyWheelScreen child(Kind next) {
        return new ArmyWheelScreen(next, this, switch (next) {
            case SUMMON -> "screen.spirit_form.army.summon";
            case ORDERS -> "screen.spirit_form.army.orders";
            default -> "screen.spirit_form.army.souls";
        });
    }

    private static Kind subKind(int index) {
        return switch (index) {
            case 0 -> Kind.SUMMON;
            case 1 -> Kind.ORDERS;
            default -> Kind.DELETE;
        };
    }

    private static String mainId(int index) {
        return switch (index) {
            case 0 -> "summon";
            case 1 -> "orders";
            default -> "delete";
        };
    }

    private static void send(int action, String arg) {
        PacketDistributor.sendToServer(new ArmyActionPayload(action, arg));
    }

    // ---------- дані ----------

    private static int summonedCount() {
        int count = 0;
        for (ArmySyncPayload.Entry soul : SpiritArmyClient.souls()) {
            if (soul.summoned()) {
                count++;
            }
        }
        return count;
    }

    private static int storageSize() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0 : SpiritFormState.get(player).effectiveArmySlots();
    }

    private static Component orderName(int index) {
        SpiritArmy.Order[] orders = SpiritArmy.Order.values();
        int i = Math.max(0, Math.min(orders.length - 1, index));
        return Component.translatable("order.spirit_form." + orders[i].name().toLowerCase());
    }

    static Component soulName(ArmySyncPayload.Entry soul) {
        return soul.name().isEmpty() ? SpiritArmy.typeName(soul.type()) : Component.literal(soul.name());
    }

    static ItemStack soulStack(ArmySyncPayload.Entry soul) {
        ItemStack custom = SpiritArmyIcons.get(soul.id());
        if (!custom.isEmpty()) {
            return custom;
        }
        // Яйце виклику шукаємо один раз для кожного виду, а не щокадру
        return EGGS.computeIfAbsent(soul.type(), type -> {
            SpawnEggItem egg = EntityType.byString(type).map(SpawnEggItem::byId).orElse(null);
            return egg == null ? ItemStack.EMPTY : new ItemStack(egg);
        });
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        switch (kind) {
            case MAIN -> {
                int total = SpiritArmyClient.souls().size();
                list.add(new Entry(SpiritWheelUi.Slice.of(
                        Component.translatable("screen.spirit_form.army.sector.summon"),
                        Component.translatable("screen.spirit_form.army.summoned_count", summonedCount(), total),
                        "army_summon", false), null, 0));
                list.add(new Entry(SpiritWheelUi.Slice.of(
                        Component.translatable("screen.spirit_form.army.sector.orders"),
                        Component.translatable("screen.spirit_form.army.current_order", orderName(SpiritArmyClient.order())),
                        "army_orders", false), null, 1));
                list.add(new Entry(SpiritWheelUi.Slice.of(
                        Component.translatable("screen.spirit_form.army.sector.delete"),
                        Component.translatable("screen.spirit_form.army.in_storage", total),
                        "army_souls", false), null, 2));
                list.add(new Entry(SpiritWheelUi.Slice.of(
                        Component.translatable("screen.spirit_form.army.sector.upgrade"),
                        Component.translatable("screen.spirit_form.army.upgrade_info"),
                        "army_upgrade", false), null, 3));
            }
            case ORDERS -> {
                for (int i = 0; i < ORDER_IDS.length; i++) {
                    list.add(new Entry(SpiritWheelUi.Slice.of(
                            Component.translatable("order.spirit_form." + ORDER_IDS[i]),
                            Component.translatable(i == SpiritArmyClient.order()
                                    ? "screen.spirit_form.wheel.on" : "screen.spirit_form.army.order_pick"),
                            "order_" + ORDER_IDS[i], i == SpiritArmyClient.order()), null, i));
                }
            }
            default -> {
                boolean delete = kind == Kind.DELETE;
                for (ArmySyncPayload.Entry soul : SpiritArmyClient.souls()) {
                    Component info = Component.literal("♥ " + soul.hp())
                            .append(soul.summoned() && !delete
                                    ? Component.literal(" · ").append(Component.translatable("screen.spirit_form.army.summoned"))
                                    : Component.empty());
                    boolean pending = delete && soul.id().equals(pendingDelete);
                    list.add(new Entry(new SpiritWheelUi.Slice(soulName(soul), info, "soul", soulStack(soul),
                            soul.summoned() && !delete, delete, pending), soul, list.size()));
                }
            }
        }
        return list;
    }

    /** Назва дії центральної кнопки (для підказки внизу). */
    private Component centerName() {
        if (kind == Kind.MAIN) {
            return Component.translatable(summonedCount() > 0
                    ? "screen.spirit_form.army.recall_all" : "screen.spirit_form.army.summon_all");
        }
        return Component.translatable("screen.spirit_form.army.back");
    }

    /** Центр головного колеса: лише іконка й «душі/місця»; назва дії з'являється в підказці. */
    private SpiritWheelUi.Center center() {
        if (kind == Kind.MAIN) {
            return new SpiritWheelUi.Center("army", null,
                    Component.literal(SpiritArmyClient.souls().size() + "/" + storageSize()));
        }
        return new SpiritWheelUi.Center(null, centerName(), null);
    }

    // ---------- екран ----------

    @Override
    public void removed() {
        SpiritWheelUi.release();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private SpiritWheelUi.Geo geo() {
        return SpiritWheelUi.Geo.of(height);
    }

    private int hovered(int count, double mouseX, double mouseY) {
        return SpiritWheelUi.hit(geo(), count, mouseX - width / 2.0, mouseY - height / 2.0);
    }

    private Component hint() {
        return Component.translatable(switch (kind) {
            case MAIN -> "screen.spirit_form.army.hint.main";
            case SUMMON -> "screen.spirit_form.army.hint.summon";
            case ORDERS -> "screen.spirit_form.army.hint.orders";
            case DELETE -> "screen.spirit_form.army.hint.delete";
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<Entry> entries = entries();
        List<SpiritWheelUi.Slice> slices = new ArrayList<>();
        for (Entry entry : entries) {
            slices.add(entry.slice());
        }

        int raw = hovered(entries.size(), mouseX, mouseY);
        int hover = Math.max(-1, raw);
        boolean centerHot = raw == -2;

        Component hint = hint();
        SpiritWheelUi.Tip tip;
        if (centerHot) {
            Component storage = kind == Kind.MAIN
                    ? Component.translatable("screen.spirit_form.army.storage", SpiritArmyClient.souls().size(), storageSize())
                    : null;
            tip = new SpiritWheelUi.Tip(centerName(), storage, null, hint);
        } else if (hover >= 0) {
            Entry entry = entries.get(hover);
            Component detail = null;
            if (kind == Kind.ORDERS) {
                detail = Component.translatable("order.spirit_form." + ORDER_IDS[entry.index()] + ".desc");
            } else if (kind == Kind.DELETE && entry.slice().pending()) {
                detail = Component.translatable("screen.spirit_form.army.confirm", entry.slice().name());
            } else if (kind == Kind.MAIN) {
                detail = Component.translatable("screen.spirit_form.army.sector." + mainId(entry.index()) + ".desc");
            }
            tip = new SpiritWheelUi.Tip(entry.slice().name(), entry.slice().info(), detail, hint);
        } else if (entries.isEmpty()) {
            tip = new SpiritWheelUi.Tip(title, null, Component.translatable("screen.spirit_form.army.empty"), hint);
        } else {
            tip = new SpiritWheelUi.Tip(title, kind == Kind.MAIN
                    ? Component.translatable("screen.spirit_form.army.storage", SpiritArmyClient.souls().size(), storageSize())
                    : null, null, hint);
        }
        SpiritWheelUi.draw(graphics, font, width, height, geo(), slices, hover, center(), centerHot, tip);
    }

    // ---------- керування ----------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Entry> entries = entries();
        int raw = hovered(entries.size(), mouseX, mouseY);

        if (button == 1) {
            if (hasShiftDown() && raw >= 0 && entries.get(raw).soul() != null && minecraft != null) {
                // Shift + ПКМ по душі: змінити іконку
                minecraft.setScreen(new ArmyIconScreen(this, entries.get(raw).soul().id()));
                return true;
            }
            back();
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (raw == -2) {
            if (kind == Kind.MAIN) {
                send(summonedCount() > 0 ? SpiritArmy.ACT_RECALL : SpiritArmy.ACT_SUMMON, "");
                click();
                onClose();
            } else {
                click();
                back();
            }
            return true;
        }
        if (raw < 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        Entry entry = entries.get(raw);
        switch (kind) {
            case MAIN -> {
                click();
                if (minecraft != null) {
                    minecraft.setScreen(entry.index() == 3 ? new ArmySoulsScreen(this) : child(subKind(entry.index())));
                }
            }
            case SUMMON -> {
                // Повторне клацання по викликаній душі відкликає її (це робить сервер)
                send(SpiritArmy.ACT_SUMMON, entry.soul().id().toString());
                click();
            }
            case ORDERS -> {
                send(SpiritArmy.ACT_ORDER, String.valueOf(entry.index()));
                click();
                onClose();
            }
            case DELETE -> {
                click();
                if (!entry.soul().id().equals(pendingDelete)) {
                    pendingDelete = entry.soul().id();
                } else {
                    pendingDelete = null;
                    send(SpiritArmy.ACT_DELETE, entry.soul().id().toString());
                }
            }
        }
        return true;
    }

    /** Повернення до попереднього колеса; з головного закриває меню. */
    private void back() {
        if (parent != null && minecraft != null) {
            minecraft.setScreen(parent);
        } else {
            onClose();
        }
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
