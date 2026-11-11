package com.spiritform;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Чуття Привида: поки здібність увімкнена, крізь стіни підсвічуються сховища (скрині, діжки, шалкерові ящики,
 * ендер-скрині та будь-які блоки з вмістом на основі BaseContainerBlockEntity, зокрема з інших модів).
 * Пошук іде раз на пів секунди по завантажених блок-сутностях поблизу, а щокадру малюються лише готові рамки.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID, value = Dist.CLIENT)
public final class SpiritContainerHighlight {

    private static final int SCAN_INTERVAL = 10;
    private static final int MAX_BOXES = 512;

    private static final List<AABB> BOXES = new ArrayList<>();

    private SpiritContainerHighlight() {
    }

    /** Чи є блок-сутність сховищем (піч, воронку, роздавач і варильну стійку не рахуємо). */
    private static boolean isStorage(BlockEntity be) {
        if (be instanceof EnderChestBlockEntity) {
            return true;
        }
        if (be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity
                || be instanceof DispenserBlockEntity || be instanceof BrewingStandBlockEntity
                || be instanceof CrafterBlockEntity) {
            return false;
        }
        return be instanceof BaseContainerBlockEntity;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !SpiritClientHighlight.isEnabled()
                || !SpiritFormState.get(player).hasAbility(SpiritAbility.SENSE)) {
            BOXES.clear();
            return;
        }
        if (player.tickCount % SCAN_INTERVAL != 0) {
            return;
        }
        scan(mc.level, player);
    }

    private static void scan(Level level, LocalPlayer player) {
        BOXES.clear();
        double radius = Config.HIGHLIGHT_RADIUS.get() * SpiritRange.mul(player);
        double radiusSqr = radius * radius;
        int minCx = (int) Math.floor(player.getX() - radius) >> 4;
        int maxCx = (int) Math.floor(player.getX() + radius) >> 4;
        int minCz = (int) Math.floor(player.getZ() - radius) >> 4;
        int maxCz = (int) Math.floor(player.getZ() + radius) >> 4;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                ChunkAccess access = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (BOXES.size() >= MAX_BOXES) {
                        return;
                    }
                    BlockPos pos = be.getBlockPos();
                    if (pos.distToCenterSqr(player.getX(), player.getY(), player.getZ()) > radiusSqr
                            || !isStorage(be)) {
                        continue;
                    }
                    AABB box = be.getBlockState().getShape(level, pos).isEmpty()
                            ? new AABB(pos)
                            : be.getBlockState().getShape(level, pos).bounds().move(pos);
                    BOXES.add(box.inflate(0.002));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BOXES.isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        var cam = event.getCamera().getPosition();

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (AABB box : BOXES) {
            LevelRenderer.renderLineBox(pose, builder, box, 0.37F, 0.88F, 0.71F, 0.9F);
        }
        pose.popPose();

        // Малюємо напряму, примусово вимкнувши перевірку глибини, щоб рамки було видно крізь блоки
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.lineWidth(2.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
