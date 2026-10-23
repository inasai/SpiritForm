package com.spiritform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Малює Стигмат Фантома на персонажі, коли він лежить у видимому слоті Curios (не в руці): так само, як
 * коли предмет тримають у руці, але в вільній руці. Сховаєш "оком" у Curios, і предмет зникає.
 */
public class BraceletLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public BraceletLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!FentonBracelet.shownOnBody(player)) {
            return;
        }

        ItemStack stack = new ItemStack(SpiritItems.FENTONS_BRACELET.get());
        HumanoidArm arm = player.getMainArm().getOpposite();
        boolean left = arm == HumanoidArm.LEFT;

        poseStack.pushPose();
        getParentModel().translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                player, stack,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
