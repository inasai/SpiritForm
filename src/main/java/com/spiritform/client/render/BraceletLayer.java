package com.spiritform.client.render;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.SpiritTransparency;
import com.spiritform.client.model.AccessoryModels;
import com.spiritform.data.SpiritData;
import com.spiritform.form.SpiritFormState;
import com.spiritform.item.FentonBracelet;
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
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Малює Стигмат Фантома на персонажі, коли він лежить у видимій комірці сховища (не в руці): манжета на зап'ясті
 * вільної руки (протилежної основній) з хрестом спереду. Сховаєш "оком", і манжета зникає.
 * Текстура: assets/spirit_form/textures/entity/fentons_bracelet.png (32x32).
 */
public class BraceletLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            SpiritFormMod.MODID, "textures/entity/fentons_bracelet.png");

    private final AccessoryModels model;

    public BraceletLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                         EntityModelSet models) {
        super(parent);
        this.model = new AccessoryModels(models.bakeLayer(AccessoryModels.BRACELET));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!FentonBracelet.shownOnBody(player)) {
            return;
        }

        SpiritData data = SpiritFormState.get(player);
        boolean ghost = data.active() || data.invisible();
        VertexConsumer consumer = buffer.getBuffer(
                ghost ? RenderType.entityTranslucent(TEXTURE) : RenderType.entityCutoutNoCull(TEXTURE));
        int color = ghost ? SpiritTransparency.GHOST_COLOR : -1;

        HumanoidArm side = player.getMainArm().getOpposite();
        boolean left = side == HumanoidArm.LEFT;
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        ModelPart arm = left ? getParentModel().leftArm : getParentModel().rightArm;
        // Середина руки відносно її точки обертання: широка рука зсунута на 1, тонка на 0,5
        float centre = (slim ? 0.5F : 1.0F) * (left ? 1.0F : -1.0F);

        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        poseStack.translate(centre / 16.0F, 0.0F, 0.0F);
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
    }
}
