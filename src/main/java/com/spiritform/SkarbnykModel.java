package com.spiritform;

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
public class SkarbnykModel<T extends LivingEntity> extends EntityModel<T> implements ArmedModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "skarbnyk"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart earR;
    private final ModelPart earL;
    private final ModelPart body;
    private final ModelPart loin;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public SkarbnykModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.nose = root.getChild("head").getChild("nose");
        this.earR = root.getChild("head").getChild("ear_r");
        this.earL = root.getChild("head").getChild("ear_l");
        this.body = root.getChild("body");
        this.loin = root.getChild("body").getChild("loin");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -6F, -3F, 6F, 6F, 6F),
                PartPose.offset(0F, 10F, 0F));
        PartDefinition noseDef = headDef.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1F, -3F, -5F, 2F, 3F, 2F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition earRDef = headDef.addOrReplaceChild("ear_r",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4F, -1F, -0.5F, 4F, 2F, 1F),
                PartPose.offsetAndRotation(-3F, -3F, 0F, 0F, 0F, 0.3F));
        PartDefinition earLDef = headDef.addOrReplaceChild("ear_l",
                CubeListBuilder.create().texOffs(42, 0).addBox(0F, -1F, -0.5F, 4F, 2F, 1F),
                PartPose.offsetAndRotation(3F, -3F, 0F, 0F, 0F, -0.3F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 12).addBox(-3F, 0F, -2F, 6F, 7F, 4F),
                PartPose.offsetAndRotation(0F, 10F, 0F, 0.15F, 0F, 0F));
        PartDefinition loinDef = bodyDef.addOrReplaceChild("loin",
                CubeListBuilder.create().texOffs(20, 12).addBox(-4F, 6F, -3F, 8F, 4F, 6F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(48, 12).addBox(-1F, -1F, -1F, 2F, 8F, 2F),
                PartPose.offset(-4F, 11F, 0F));
        PartDefinition leftArmDef = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(56, 12).addBox(-1F, -1F, -1F, 2F, 8F, 2F),
                PartPose.offset(4F, 11F, 0F));
        PartDefinition rightLegDef = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 23).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F),
                PartPose.offset(-1.5F, 17F, 0F));
        PartDefinition leftLegDef = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(12, 23).addBox(-1.5F, 0F, -1.5F, 3F, 7F, 3F),
                PartPose.offset(1.5F, 17F, 0F));
        return LayerDefinition.create(mesh, 64, 48);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg;
        this.head.xRot = headPitch * deg;
        float swing = Mth.cos(limbSwing * 0.8F) * limbSwingAmount;
        this.rightLeg.xRot = swing * 1.4F;
        this.leftLeg.xRot = -swing * 1.4F;
        if (!entity.getMainHandItem().isEmpty()) {
            // Тримає здобич обома руками перед собою
            this.rightArm.xRot = -1.1F;
            this.leftArm.xRot = -1.1F;
            this.rightArm.yRot = -0.25F;
            this.leftArm.yRot = 0.25F;
        } else {
            this.rightArm.xRot = -swing * 1.2F;
            this.leftArm.xRot = swing * 1.2F;
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
        }
        this.earR.zRot = 0.3F + Mth.sin(ageInTicks * 0.15F) * 0.05F;
        this.earL.zRot = -0.3F - Mth.sin(ageInTicks * 0.15F) * 0.05F;
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
