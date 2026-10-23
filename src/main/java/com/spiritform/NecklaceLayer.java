package com.spiritform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Малює Намисто Потойбіччя на гравці, коли воно одягнене нагрудником або лежить у видимому слоті Curios "намисто"
 * (якщо в Curios вимкнути "око", намисто не малюється).
 * Текстура: assets/spirit_form/textures/entity/necklace_of_the_beyond.png.
 * У формі Привида (напівпрозорий вигляд) намисто теж напівпрозоре.
 */
public class NecklaceLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            SpiritFormMod.MODID, "textures/entity/necklace_of_the_beyond.png");

    private final NecklaceModel model;

    public NecklaceLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                         EntityModelSet models) {
        super(parent);
        this.model = new NecklaceModel(models.bakeLayer(NecklaceModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!FentonBracelet.showsNecklace(player)) {
            return;
        }

        SpiritData data = SpiritFormState.get(player);
        boolean ghost = data.active() || data.invisible();

        poseStack.pushPose();
        // Намисто кріпиться до тулуба, тож рухається разом із ним (присідання, нахил)
        getParentModel().body.translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(
                ghost ? RenderType.entityTranslucent(TEXTURE) : RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                ghost ? SpiritTransparency.GHOST_COLOR : -1);
        poseStack.popPose();
    }
}
