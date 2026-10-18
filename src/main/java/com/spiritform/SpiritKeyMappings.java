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

@EventBusSubscriber(
        modid = SpiritFormMod.MODID,
        value = Dist.CLIENT,
        bus = Bus.MOD
)
public class SpiritKeyMappings {

    public static final KeyMapping TOGGLE_SPIRIT = new KeyMapping(
            "key.spirit_form.toggle_spirit",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.spirit_form"
    );

    public static final KeyMapping OPEN_UPGRADES = new KeyMapping(
            "key.spirit_form.open_upgrades",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.spirit_form"
    );

    public static final KeyMapping INVISIBILITY = new KeyMapping(
            "key.spirit_form.invisibility",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.spirit_form"
    );

    public static final KeyMapping TELEPORT = new KeyMapping(
            "key.spirit_form.teleport",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.spirit_form"
    );

    // G уже зайнята перемикачем форми, тому підсвічування мобів стоїть на V.
    // Клавішу можна змінити в налаштуваннях керування гри.
    public static final KeyMapping TOGGLE_HIGHLIGHT = new KeyMapping(
            "key.spirit_form.toggle_highlight",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.spirit_form"
    );

    public static final KeyMapping POSSESS = new KeyMapping(
            "key.spirit_form.possess",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.spirit_form"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_SPIRIT);
        event.register(OPEN_UPGRADES);
        event.register(INVISIBILITY);
        event.register(TELEPORT);
        event.register(TOGGLE_HIGHLIGHT);
        event.register(POSSESS);
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

            while (INVISIBILITY.consumeClick()) {
                if (player != null) {
                    PacketDistributor.sendToServer(new InvisibilityPayload());
                }
            }

            while (TELEPORT.consumeClick()) {
                if (player != null) {
                    PacketDistributor.sendToServer(new TeleportPayload());
                }
            }

            while (POSSESS.consumeClick()) {
                if (player != null && minecraft.screen == null) {
                    PacketDistributor.sendToServer(new PossessPayload());
                }
            }

            while (TOGGLE_HIGHLIGHT.consumeClick()) {
                if (player != null) {
                    SpiritClientHighlight.toggle();
                }
            }
        }
    }
}
