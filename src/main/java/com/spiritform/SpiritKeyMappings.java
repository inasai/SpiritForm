package com.spiritform;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Клавіші мода. G і H працюють завжди. R вмикає режим активних здібностей, у якому працюють
 * цифри (панель активних здібностей) та гарячі клавіші основних (V, B, N, Z, X).
 * T показує колесо пасивних здібностей, поки утримується.
 * Решту обробляє SpiritAbilityMode.
 */
@EventBusSubscriber(
        modid = SpiritFormMod.MODID,
        value = Dist.CLIENT,
        bus = Bus.MOD
)
public class SpiritKeyMappings {

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, code, "key.categories.spirit_form");
    }

    public static final KeyMapping TOGGLE_SPIRIT = key("key.spirit_form.toggle_spirit", GLFW.GLFW_KEY_G);
    public static final KeyMapping OPEN_UPGRADES = key("key.spirit_form.open_upgrades", GLFW.GLFW_KEY_H);

    /** Режим активних здібностей: замінює хотбар панеллю здібностей. */
    public static final KeyMapping ABILITY_MODE = key("key.spirit_form.ability_mode", GLFW.GLFW_KEY_R);

    /** Утримувати: колесо пасивних здібностей. */
    public static final KeyMapping PASSIVE_WHEEL = key("key.spirit_form.passive_wheel", GLFW.GLFW_KEY_T);

    /** Гарячі клавіші основних здібностей (справа внизу, працюють у режимі активних здібностей). */
    public static final KeyMapping[] MAIN = {
            key("key.spirit_form.main_1", GLFW.GLFW_KEY_V),
            key("key.spirit_form.main_2", GLFW.GLFW_KEY_B),
            key("key.spirit_form.main_3", GLFW.GLFW_KEY_N)
    };

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_SPIRIT);
        event.register(OPEN_UPGRADES);
        event.register(ABILITY_MODE);
        event.register(PASSIVE_WHEEL);
        for (KeyMapping mapping : MAIN) {
            event.register(mapping);
        }
    }

    @EventBusSubscriber(
            modid = SpiritFormMod.MODID,
            value = Dist.CLIENT
    )
    public static class ClientEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            var player = minecraft.player;

            while (TOGGLE_SPIRIT.consumeClick()) {
                if (player != null) {
                    // Стан змінює лише сервер, клієнт просто просить
                    PacketDistributor.sendToServer(new ToggleSpiritPayload());
                }
            }

            while (OPEN_UPGRADES.consumeClick()) {
                if (player != null && minecraft.screen == null) {
                    minecraft.setScreen(new SpiritUpgradeScreen());
                }
            }
        }
    }
}
