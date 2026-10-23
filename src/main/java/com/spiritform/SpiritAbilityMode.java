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
        SpiritAbility ability = SpiritLoadout.ability(SpiritLoadout.get(bar, index));
        return ability != null && ability.kind == SpiritAbility.Kind.ACTIVE ? ability : null;
    }

    private static void trigger(SpiritAbility ability) {
        if (ability == SpiritAbility.SENSE) {
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

    /** Поки тримається Доторк, колесико миші відсуває й наближає моба (хотбар не гортається). */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!active || !isHolding(SpiritAbility.TOUCH) || Minecraft.getInstance().screen != null) {
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
            default -> ability.hold && isHolding(ability);
        };
    }

    private static void drawSlot(GuiGraphics graphics, Minecraft minecraft, SpiritData data,
                                 SpiritAbility ability, String id, int x, int y, int size, Component label) {
        boolean owned = ability != null && data.hasAbility(ability);
        boolean on = owned && isToggledOn(ability, minecraft, data);

        graphics.fill(x, y, x + size, y + size, 0xB0101018);
        graphics.renderOutline(x, y, size, size, on ? 0xFF55FF55 : 0xFF8888AA);

        if (ability != null) {
            int offset = (size - 16) / 2;
            SpiritIcons.draw(graphics, id, x + offset, y + offset);
            if (!owned) {
                graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xA0000000);
            }
        }

        if (label != null) {
            graphics.drawString(minecraft.font, label, x + size - 2 - minecraft.font.width(label),
                    y + size - 9, 0xFFFFFF, true);
        }
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        // Панель активних здібностей внизу по центру
        int baseX = width / 2 - 91;
        int y = height - 22;
        for (int i = 0; i < AUX; i++) {
            String id = SpiritLoadout.get(SpiritLoadout.Bar.AUX, i);
            Component label = minecraft.options.keyHotbarSlots[i].getTranslatedKeyMessage();
            drawSlot(graphics, minecraft, data, activeAt(SpiritLoadout.Bar.AUX, i), id,
                    baseX + i * 20, y, 20, label);
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
