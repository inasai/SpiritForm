package com.spiritform;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
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

    /**
     * Погляд камери береться з гравця щокадру, а не раз на тік (20 разів на секунду). Інакше, поки камера на мобі,
     * поворот миші оновлювався б зависаннями, бо повертаємо моба лише в onClientTick.
     */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isControllingOther(minecraft)) {
            return;
        }
        LocalPlayer player = minecraft.player;
        float partial = (float) event.getPartialTick();
        float yaw = player.getViewYRot(partial);
        float pitch = player.getViewXRot(partial);
        if (minecraft.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            yaw += 180.0F;
            pitch = -pitch;
        }
        event.setYaw(yaw);
        event.setPitch(pitch);
    }

    /**
     * Ванільні атака й використання беруть ціль з погляду камери і б'ють по власному тілу гравця,
     * через що сервер розриває з'єднання. Тому під час вселення скасовуємо їх і надсилаємо
     * власний пакет: удар або використання виконує сам моб.
     */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isControllingOther(Minecraft.getInstance())) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);

        if (event.isAttack()) {
            PacketDistributor.sendToServer(new PossessActionPayload(SpiritPossession.ACTION_ATTACK));
        } else if (event.isUseItem()) {
            PacketDistributor.sendToServer(new PossessActionPayload(SpiritPossession.ACTION_USE));
        }
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
