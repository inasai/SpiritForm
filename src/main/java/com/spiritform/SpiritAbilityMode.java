package com.spiritform;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Режим активних здібностей (клавіша R).
 *
 * Поки режим увімкнено, звичайний хотбар заміняє панель здібностей: активні здібності з панелі
 * запускаються цифрами 1-9, а основні мають свої гарячі клавіші справа внизу. Що де стоїть,
 * гравець налаштовує у вікні прокачки (SpiritLoadout).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritAbilityMode {

    private static final int AUX = SpiritLoadout.Bar.AUX.size;
    private static final int MAIN = SpiritLoadout.Bar.MAIN.size;

    private static boolean active;

    /** Здібності, що утримуються зараз: індекси 0..8 цифри, далі основні клавіші. */
    private static final SpiritAbility[] HELD = new SpiritAbility[AUX + MAIN];

    private SpiritAbilityMode() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isHolding(SpiritAbility ability) {
        for (SpiritAbility held : HELD) {
            if (held == ability) {
                return true;
            }
        }
        return false;
    }

    // ---------- клавіші ----------

    private static boolean controllingOther(Minecraft minecraft) {
        Entity camera = minecraft.getCameraEntity();
        return minecraft.player != null && camera != null && camera != minecraft.player;
    }

    private static void drain(KeyMapping key) {
        while (key.consumeClick()) {
            // скидаємо накопичені натискання
        }
    }

    private static void send(SpiritAbility ability, boolean pressed) {
        PacketDistributor.sendToServer(new AbilityPayload(ability.ordinal(), pressed));
    }

    private static void releaseAll() {
        for (int i = 0; i < HELD.length; i++) {
            if (HELD[i] != null) {
                send(HELD[i], false);
                HELD[i] = null;
            }
        }
    }

    private static SpiritAbility activeAt(SpiritLoadout.Bar bar, int index) {
        // На цифрах і основних клавішах може стояти будь-яка здібність: пасивна перемикається, активна запускається
        return SpiritLoadout.ability(SpiritLoadout.get(bar, index));
    }

    static void trigger(SpiritAbility ability) {
        int toggle = SpiritAbilityUse.toggleAction(ability);
        if (toggle >= 0) {
            LocalPlayer self = Minecraft.getInstance().player;
            if (self != null && SpiritFormState.get(self).hasAbility(ability)) {
                PacketDistributor.sendToServer(new UpgradeSpiritPayload(toggle));
            }
            return;
        }
        if (ability == SpiritAbility.SELF_SUFFICIENCY) {
            LocalPlayer self = Minecraft.getInstance().player;
            if (self != null && SpiritFormState.get(self).hasAbility(SpiritAbility.SELF_SUFFICIENCY)) {
                Minecraft.getInstance().setScreen(new SpiritStationScreen());
            }
            return;
        }
        if (ability == SpiritAbility.ARMY) {
            LocalPlayer self = Minecraft.getInstance().player;
            if (self != null && SpiritFormState.get(self).hasAbility(SpiritAbility.ARMY)) {
                Minecraft.getInstance().setScreen(ArmyWheelScreen.main());
            } else if (self != null) {
                self.displayClientMessage(Component.translatable("message.spirit_form.army.locked"), true);
            }
        } else if (ability == SpiritAbility.SENSE) {
            SpiritClientHighlight.toggle();
        } else {
            send(ability, true);
        }
    }

    /** Одна клавіша панелі: або разове натискання, або утримування. */
    private static void handleKey(KeyMapping key, SpiritAbility ability, int heldIndex, boolean noScreen) {
        if (ability == null) {
            if (HELD[heldIndex] != null) {
                send(HELD[heldIndex], false);
                HELD[heldIndex] = null;
            }
            drain(key);
            return;
        }

        if (ability.hold) {
            drain(key);
            boolean want = noScreen && key.isDown();
            if (HELD[heldIndex] != null && (HELD[heldIndex] != ability || !want)) {
                send(HELD[heldIndex], false);
                HELD[heldIndex] = null;
            }
            if (want && HELD[heldIndex] == null) {
                send(ability, true);
                HELD[heldIndex] = ability;
            }
            return;
        }

        while (key.consumeClick()) {
            if (noScreen) {
                trigger(ability);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            active = false;
            for (int i = 0; i < HELD.length; i++) {
                HELD[i] = null;
            }
            return;
        }
        Options options = minecraft.options;
        boolean noScreen = minecraft.screen == null;

        // R: режим активних здібностей
        while (SpiritKeyMappings.ABILITY_MODE.consumeClick()) {
            if (noScreen) {
                toggleMode(player);
            }
        }

        // T: колесо пасивних здібностей (поки утримується)
        while (SpiritKeyMappings.PASSIVE_WHEEL.consumeClick()) {
            if (noScreen && minecraft.screen == null) {
                if (SpiritKeyMappings.PASSIVE_WHEEL.getKey().equals(options.keyChat.getKey())) {
                    drain(options.keyChat); // T за замовчуванням відкриває чат, тут вона відкриває колесо
                }
                minecraft.setScreen(new SpiritWheelScreen());
            }
        }

        if (!active || !SpiritFormState.get(player).unlocked()) {
            releaseAll();
            for (KeyMapping key : SpiritKeyMappings.MAIN) {
                drain(key);
            }
            if (!SpiritFormState.get(player).unlocked()) {
                active = false;
            }
            return;
        }

        // Цифри: панель активних здібностей (клавіші хотбару не доходять до гри)
        for (int i = 0; i < AUX; i++) {
            handleKey(options.keyHotbarSlots[i], activeAt(SpiritLoadout.Bar.AUX, i), i, noScreen);
        }
        // Основні гарячі клавіші
        for (int i = 0; i < MAIN; i++) {
            handleKey(SpiritKeyMappings.MAIN[i], activeAt(SpiritLoadout.Bar.MAIN, i), AUX + i, noScreen);
        }

    }

    /** Чи стоїть на гравцеві наш показовий ефект Висушування (невидимий, коротший за 5 тіків). */
    private static boolean isFakeWither(MobEffectInstance effect) {
        return effect != null && !effect.isVisible() && !effect.showIcon() && effect.getDuration() <= 5;
    }

    /**
     * Поки заряджаються Дарування Життя чи Терези, сердечка виглядають як при Висушуванні.
     * Клієнт сам не прибирає завершені ефекти, тому після відпускання клавіші знімаємо його вручну.
     */
    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        boolean want = active && (isHolding(SpiritAbility.LIFE_GIFT) || isHolding(SpiritAbility.SCALES));
        if (want) {
            player.forceAddEffect(new MobEffectInstance(MobEffects.WITHER, 4, 0, false, false, false), null);
        } else if (isFakeWither(player.getEffect(MobEffects.WITHER))) {
            player.removeEffectNoUpdate(MobEffects.WITHER);
        }
    }

    /** Поки тримається Доторк (моб чи блок), колесико миші відсуває й наближає ціль (хотбар не гортається). */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.screen != null || minecraft.player == null) {
            return;
        }
        // Колесо двигає ціль (моба чи блок), поки її тримає Доторк
        if (!SpiritFormState.get(minecraft.player).blockGrab()) {
            return;
        }
        double delta = event.getScrollDeltaY();
        if (delta == 0.0) {
            return;
        }
        event.setCanceled(true);
        PacketDistributor.sendToServer(new TouchDistancePayload(delta > 0 ? 1 : -1));
    }

    private static void toggleMode(LocalPlayer player) {
        if (!SpiritFormState.get(player).unlocked()) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.mode.locked",
                            Component.keybind("key.spirit_form.open_upgrades")),
                    true);
            return;
        }

        active = !active;
        if (!active) {
            releaseAll();
        }
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, active ? 1.2F : 0.9F));
        player.displayClientMessage(
                Component.translatable(
                        active ? "message.spirit_form.mode.on" : "message.spirit_form.mode.off",
                        Component.keybind("key.spirit_form.ability_mode")),
                true);
    }

    // ---------- відображення ----------

    /** Панель здібностей замінює хотбар, поки режим увімкнено. */
    @SubscribeEvent
    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (!active || !VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }

        event.setCanceled(true);
        draw(event.getGuiGraphics(), minecraft, player);
    }

    private static boolean isToggledOn(SpiritAbility ability, Minecraft minecraft, SpiritData data) {
        return switch (ability) {
            case VANISH -> data.invisible();
            case SENSE -> SpiritClientHighlight.isEnabled();
            case POSSESSION -> controllingOther(minecraft);
            case ICY_TOUCH -> data.icy();
            case PRESENCE -> data.presence();
            case GAZE -> data.gaze();
            case ECHO -> data.echo();
            case ENERGY -> data.energy();
            default -> ability.hold && isHolding(ability);
        };
    }

    // Палітра збігається з колесами (SpiritWheelUi): темна м'ятна панель, світла м'ятна підсвітка.
    private static final int PLATE = 0xE0070B09;
    private static final int PLATE_EDGE = 0xFF1B3A30;
    private static final int PLATE_TOP = 0xFF3F7A64;
    private static final int CELL = 0xD00E1613;
    private static final int CELL_EMPTY = 0x70080D0B;
    private static final int CELL_ON = 0xE0183A2F;
    private static final int ACCENT = 0xFF8CFFC8;
    private static final int KEY_TEXT = 0xFFE8FFF4;

    /** Прямокутник зі зрізаними кутами й однопіксельною рамкою. */
    private static void plate(GuiGraphics graphics, int x, int y, int w, int h, int fill, int edge) {
        graphics.fill(x + 1, y, x + w - 1, y + h, fill);
        graphics.fill(x, y + 1, x + w, y + h - 1, fill);
        graphics.fill(x + 1, y, x + w - 1, y + 1, edge);
        graphics.fill(x + 1, y + h - 1, x + w - 1, y + h, edge);
        graphics.fill(x, y + 1, x + 1, y + h - 1, edge);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
    }

    private static void drawSlot(GuiGraphics graphics, Minecraft minecraft, SpiritData data,
                                 SpiritAbility ability, String id, int x, int y, int size, Component label) {
        boolean owned = ability != null && data.hasAbility(ability);
        boolean on = owned && isToggledOn(ability, minecraft, data);
        boolean framed = size >= 24;

        if (ability == null) {
            graphics.fill(x, y, x + size, y + size, CELL_EMPTY);
            return;
        }

        int fill = on ? CELL_ON : CELL;
        if (framed) {
            plate(graphics, x, y, size, size, fill, on ? ACCENT : PLATE_TOP);
        } else {
            graphics.fill(x, y, x + size, y + size, fill);
        }
        if (on) {
            // Увімкнена здібність: м'ятна смужка знизу
            graphics.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, ACCENT);
        }

        int offset = (size - 16) / 2;
        SpiritIcons.draw(graphics, id, x + offset, y + offset);
        if (!owned) {
            graphics.fill(x, y, x + size, y + size, 0xB0050807);
        } else {
            // Перезарядка: затемнення знизу вгору, що поступово зникає, зі світлою кромкою
            float left = SpiritCooldownClient.remaining(id);
            if (left > 0.0F) {
                int height = (int) Math.ceil(16 * left);
                int top = y + offset + 16 - height;
                graphics.fill(x + offset, top, x + offset + 16, y + offset + 16, 0xB05A6A64);
                graphics.fill(x + offset, top, x + offset + 16, top + 1, 0xC0B8E6D2);
            }
        }

        if (label != null) {
            // Номер клавіші дрібним шрифтом у правому нижньому куті
            graphics.pose().pushPose();
            graphics.pose().translate(x + size - 2, y + size - 7, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.drawString(minecraft.font, label, -minecraft.font.width(label), 0, KEY_TEXT, true);
            graphics.pose().popPose();
        }
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        // Панель активних здібностей внизу по центру, на місці ванільного хотбару (182x22)
        int barX = width / 2 - 91;
        int barY = height - 22;
        plate(graphics, barX, barY, 182, 22, PLATE, PLATE_EDGE);
        graphics.fill(barX + 1, barY, barX + 181, barY + 1, PLATE_TOP);
        for (int i = 0; i < AUX; i++) {
            String id = SpiritLoadout.get(SpiritLoadout.Bar.AUX, i);
            Component label = minecraft.options.keyHotbarSlots[i].getTranslatedKeyMessage();
            drawSlot(graphics, minecraft, data, activeAt(SpiritLoadout.Bar.AUX, i), id,
                    barX + 2 + i * 20, barY + 2, 18, label);
        }

        // Основні здібності справа внизу (порожні клітинки не малюємо)
        int size = 24;
        int gap = 3;
        for (int i = 0; i < MAIN; i++) {
            SpiritAbility ability = activeAt(SpiritLoadout.Bar.MAIN, i);
            if (ability == null) {
                continue;
            }
            int x = width - 8 - (i + 1) * size - i * gap;
            drawSlot(graphics, minecraft, data, ability, ability.id, x, height - size - 8, size,
                    SpiritKeyMappings.MAIN[i].getTranslatedKeyMessage());
        }
    }
}
