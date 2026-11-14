package com.spiritform.client.model;

import com.spiritform.SpiritFormMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/** Модель істоти. Частини й розкладка текстури створені генератором, анімація написана вручну. */
public class MavkaModel<T extends LivingEntity> extends EntityModel<T> implements ArmedModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "mavka"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hairUp;
    private final ModelPart wreath;
    private final ModelPart body;
    private final ModelPart hairLow;
    private final ModelPart skirt;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public MavkaModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.hairUp = root.getChild("head").getChild("hair_up");
        this.wreath = root.getChild("head").getChild("wreath");
        this.body = root.getChild("body");
        this.hairLow = root.getChild("body").getChild("hair_low");
        this.skirt = root.getChild("body").getChild("skirt");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairUpDef = headDef.addOrReplaceChild("hair_up",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, -8.5F, 3.6F, 9F, 9F, 2F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition wreathDef = headDef.addOrReplaceChild("wreath",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, -9.5F, -4.5F, 9F, 2F, 9F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(36, 16).addBox(-3F, 0F, -1.5F, 6F, 12F, 3F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairLowDef = bodyDef.addOrReplaceChild("hair_low",
                CubeListBuilder.create().texOffs(0, 31).addBox(-3.5F, -0.5F, 1.6F, 7F, 11F, 2F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition skirtDef = bodyDef.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(18, 31).addBox(-4F, 9.5F, -2.5F, 8F, 6F, 5F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(44, 31).addBox(-1.5F, -2F, -1.5F, 3F, 12F, 3F),
                PartPose.offsetAndRotation(-4.5F, 2F, 0F, 0F, 0F, 0.1F));
        PartDefinition leftArmDef = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 46).addBox(-1.5F, -2F, -1.5F, 3F, 12F, 3F),
                PartPose.offsetAndRotation(4.5F, 2F, 0F, 0F, 0F, -0.1F));
        PartDefinition rightLegDef = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(12, 46).addBox(-1.5F, 0F, -1.5F, 3F, 12F, 3F),
                PartPose.offset(-1.5F, 12F, 0F));
        PartDefinition leftLegDef = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(24, 46).addBox(-1.5F, 0F, -1.5F, 3F, 12F, 3F),
                PartPose.offset(1.5F, 12F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg;
        this.head.xRot = headPitch * deg;
        float swing = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        this.rightLeg.xRot = swing * 1.2F;
        this.leftLeg.xRot = -swing * 1.2F;
        this.rightArm.xRot = -swing * 0.9F;
        this.leftArm.xRot = swing * 0.9F;
        float sway = Mth.cos(ageInTicks * 0.09F) * 0.05F;
        this.rightArm.zRot = 0.1F + sway;
        this.leftArm.zRot = -0.1F - sway;
        this.hairLow.xRot = 0.05F + Math.abs(swing) * 0.35F + Mth.sin(ageInTicks * 0.07F) * 0.03F;
        if (this.attackTime > 0.0F) {
            float s = Mth.sin(this.attackTime * (float) Math.PI);
            this.rightArm.xRot = -1.3F - s * 0.9F;
            this.leftArm.xRot = -1.3F - s * 0.9F;
            this.rightArm.yRot = -0.25F;
            this.leftArm.yRot = 0.25F;
        } else {
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        this.rightArm.translateAndRotate(poseStack);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
