package com.spiritform.client.render;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.SpiritTransparency;
import com.spiritform.client.model.AccessoryModels;
import com.spiritform.data.SpiritData;
import com.spiritform.form.SpiritFormState;
import com.spiritform.item.ShacklesItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Малює предмет на гравці, коли він одягнений слотом броні або лежить у видимій комірці сховища (з "оком" відкритим). */
public class ShacklesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            SpiritFormMod.MODID, "textures/entity/shackles_of_the_edge.png");

    private final AccessoryModels model;

    public ShacklesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                         EntityModelSet models) {
        super(parent);
        this.model = new AccessoryModels(models.bakeLayer(AccessoryModels.SHACKLES));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!ShacklesItem.shows(player)) {
            return;
        }
        SpiritData data = SpiritFormState.get(player);
        boolean ghost = data.active() || data.invisible();
        VertexConsumer consumer = buffer.getBuffer(
                ghost ? RenderType.entityTranslucent(TEXTURE) : RenderType.entityCutoutNoCull(TEXTURE));
        int color = ghost ? SpiritTransparency.GHOST_COLOR : -1;
        poseStack.pushPose();
        getParentModel().leftLeg.translateAndRotate(poseStack);
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
        poseStack.pushPose();
        getParentModel().rightLeg.translateAndRotate(poseStack);
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
    }
}
