package com.spiritform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Модель істоти. Частини й розкладка текстури створені генератором, анімація написана вручну. */
public class VodianykModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "vodianyk"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart beard;
    private final ModelPart crest;
    private final ModelPart earR;
    private final ModelPart earL;
    private final ModelPart body;
    private final ModelPart moss;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart handR;
    private final ModelPart handL;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart footR;
    private final ModelPart footL;

    public VodianykModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.beard = root.getChild("head").getChild("beard");
        this.crest = root.getChild("head").getChild("crest");
        this.earR = root.getChild("head").getChild("ear_r");
        this.earL = root.getChild("head").getChild("ear_l");
        this.body = root.getChild("body");
        this.moss = root.getChild("body").getChild("moss");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.handR = root.getChild("right_arm").getChild("hand_r");
        this.handL = root.getChild("left_arm").getChild("hand_l");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.footR = root.getChild("right_leg").getChild("foot_r");
        this.footL = root.getChild("left_leg").getChild("foot_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F),
                PartPose.offset(0F, -2F, 0F));
        PartDefinition beardDef = headDef.addOrReplaceChild("beard",
                CubeListBuilder.create().texOffs(32, 0).addBox(-3F, -3F, -5F, 6F, 8F, 2F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition crestDef = headDef.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1F, -6F, -5F, 1F, 6F, 10F),
                PartPose.offset(0F, -8F, 0F));
        PartDefinition earRDef = headDef.addOrReplaceChild("ear_r",
                CubeListBuilder.create().texOffs(70, 0).addBox(-4F, -2F, -1F, 4F, 4F, 1F),
                PartPose.offsetAndRotation(-4F, -4F, 0F, 0F, 0F, 0.35F));
        PartDefinition earLDef = headDef.addOrReplaceChild("ear_l",
                CubeListBuilder.create().texOffs(80, 0).addBox(0F, -2F, -1F, 4F, 4F, 1F),
                PartPose.offsetAndRotation(4F, -4F, 0F, 0F, 0F, -0.35F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(90, 0).addBox(-6F, 0F, -3F, 12F, 13F, 6F),
                PartPose.offsetAndRotation(0F, -2F, 0F, 0.1F, 0F, 0F));
        PartDefinition mossDef = bodyDef.addOrReplaceChild("moss",
                CubeListBuilder.create().texOffs(0, 19).addBox(-7F, -1F, -4F, 14F, 4F, 8F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(44, 19).addBox(-2F, -1F, -2F, 4F, 15F, 4F),
                PartPose.offset(-8F, -1F, 0F));
        PartDefinition leftArmDef = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(60, 19).addBox(-2F, -1F, -2F, 4F, 15F, 4F),
                PartPose.offset(8F, -1F, 0F));
        PartDefinition handRDef = rightArmDef.addOrReplaceChild("hand_r",
                CubeListBuilder.create().texOffs(76, 19).addBox(-3F, 0F, -3F, 6F, 1F, 6F),
                PartPose.offset(0F, 14F, 0F));
        PartDefinition handLDef = leftArmDef.addOrReplaceChild("hand_l",
                CubeListBuilder.create().texOffs(100, 19).addBox(-3F, 0F, -3F, 6F, 1F, 6F),
                PartPose.offset(0F, 14F, 0F));
        PartDefinition rightLegDef = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 38).addBox(-2F, 0F, -2F, 5F, 13F, 5F),
                PartPose.offset(-3F, 11F, 0F));
        PartDefinition leftLegDef = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(20, 38).addBox(-3F, 0F, -2F, 5F, 13F, 5F),
                PartPose.offset(3F, 11F, 0F));
        PartDefinition footRDef = rightLegDef.addOrReplaceChild("foot_r",
                CubeListBuilder.create().texOffs(40, 38).addBox(-3F, 0F, -6F, 6F, 1F, 9F),
                PartPose.offset(0F, 12F, 0F));
        PartDefinition footLDef = leftLegDef.addOrReplaceChild("foot_l",
                CubeListBuilder.create().texOffs(70, 38).addBox(-3F, 0F, -6F, 6F, 1F, 9F),
                PartPose.offset(0F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg * 0.8F;
        this.head.xRot = headPitch * deg * 0.8F;
        float swing = Mth.cos(limbSwing * 0.6F) * limbSwingAmount;
        float s = this.attackTime > 0.0F ? Mth.sin(this.attackTime * (float) Math.PI) : 0.0F;
        boolean aggressive = entity instanceof Mob mob && mob.isAggressive();
        if (entity.isInWater()) {
            float stroke = Mth.cos(ageInTicks * 0.25F);
            this.rightLeg.xRot = stroke * 0.8F;
            this.leftLeg.xRot = -stroke * 0.8F;
            this.rightArm.xRot = -0.7F - stroke * 0.5F - s * 1.0F;
            this.leftArm.xRot = -0.7F + stroke * 0.5F - s * 1.0F;
        } else {
            this.rightLeg.xRot = swing * 1.2F;
            this.leftLeg.xRot = -swing * 1.2F;
            float base = aggressive ? -1.45F : 0.0F;
            this.rightArm.xRot = base - swing * (aggressive ? 0.3F : 1.0F) - s * 0.9F;
            this.leftArm.xRot = base + swing * (aggressive ? 0.3F : 1.0F) - s * 0.9F;
        }
        this.rightArm.zRot = 0.06F;
        this.leftArm.zRot = -0.06F;
        this.body.xRot = 0.1F + s * 0.2F;
        this.earR.zRot = 0.35F + Mth.sin(ageInTicks * 0.12F) * 0.08F;
        this.earL.zRot = -0.35F - Mth.sin(ageInTicks * 0.12F) * 0.08F;
        this.beard.xRot = Math.abs(swing) * 0.2F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
