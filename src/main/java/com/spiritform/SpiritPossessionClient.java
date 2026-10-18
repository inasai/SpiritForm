package com.spiritform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Клієнтська частина вселення: поки камера на іншій сутності, надсилаємо серверу клавіші руху
 * й напрямок погляду, а власний рух гравця вимикаємо.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritPossessionClient {

    private SpiritPossessionClient() {
    }

    private static boolean isControllingOther(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        Entity camera = minecraft.getCameraEntity();
        return player != null && camera != null && camera != player;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isControllingOther(minecraft)) {
            return;
        }

        LocalPlayer player = minecraft.player;
        Options options = minecraft.options;
        boolean canMove = minecraft.screen == null;

        float forward = 0.0F;
        float strafe = 0.0F;
        boolean jump = false;
        if (canMove) {
            forward = (options.keyUp.isDown() ? 1.0F : 0.0F) - (options.keyDown.isDown() ? 1.0F : 0.0F);
            strafe = (options.keyLeft.isDown() ? 1.0F : 0.0F) - (options.keyRight.isDown() ? 1.0F : 0.0F);
            jump = options.keyJump.isDown();
        }

        float yaw = player.getYRot();
        float pitch = player.getXRot();

        PacketDistributor.sendToServer(new PossessInputPayload(forward, strafe, jump, yaw, pitch));

        // Камера на мобі: щоб погляд не відставав, повертаємо моба на клієнті одразу
        Entity camera = minecraft.getCameraEntity();
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (isControllingOther(Minecraft.getInstance())) {
            event.getInput().forwardImpulse = 0.0F;
            event.getInput().leftImpulse = 0.0F;
            event.getInput().jumping = false;
        }
    }
}
