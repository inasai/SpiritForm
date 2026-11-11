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

/** Модель істоти. Частини й розкладка текстури створені генератором, анімація написана вручну. */
public class PerelisnykModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "perelisnyk"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart branchR1;
    private final ModelPart branchR2;
    private final ModelPart branchL1;
    private final ModelPart branchL2;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public PerelisnykModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.branchR1 = root.getChild("head").getChild("branch_r1");
        this.branchR2 = root.getChild("head").getChild("branch_r1").getChild("branch_r2");
        this.branchL1 = root.getChild("head").getChild("branch_l1");
        this.branchL2 = root.getChild("head").getChild("branch_l1").getChild("branch_l2");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -9F, -4F, 8F, 9F, 8F),
                PartPose.offset(0F, -10F, 0F));
        PartDefinition branchR1Def = headDef.addOrReplaceChild("branch_r1",
                CubeListBuilder.create().texOffs(32, 0).addBox(-0.5F, -9F, -0.5F, 1F, 9F, 1F),
                PartPose.offsetAndRotation(-2F, -9F, 0F, 0F, 0F, -0.4F));
        PartDefinition branchR2Def = branchR1Def.addOrReplaceChild("branch_r2",
                CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, -0.7F));
        PartDefinition branchL1Def = headDef.addOrReplaceChild("branch_l1",
                CubeListBuilder.create().texOffs(40, 0).addBox(-0.5F, -9F, -0.5F, 1F, 9F, 1F),
                PartPose.offsetAndRotation(2F, -9F, 0F, 0F, 0F, 0.4F));
        PartDefinition branchL2Def = branchL1Def.addOrReplaceChild("branch_l2",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0.7F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(48, 0).addBox(-5F, 0F, -3F, 10F, 14F, 6F),
                PartPose.offsetAndRotation(0F, -10F, 0F, 0.1F, 0F, 0F));
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(80, 0).addBox(-1.5F, -1F, -1.5F, 3F, 26F, 3F),
                PartPose.offset(-7F, -9F, 0F));
        PartDefinition leftArmDef = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(92, 0).addBox(-1.5F, -1F, -1.5F, 3F, 26F, 3F),
                PartPose.offset(7F, -9F, 0F));
        PartDefinition rightLegDef = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(104, 0).addBox(-1.5F, 0F, -1.5F, 3F, 20F, 3F),
                PartPose.offset(-2.5F, 4F, 0F));
        PartDefinition leftLegDef = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(116, 0).addBox(-1.5F, 0F, -1.5F, 3F, 20F, 3F),
                PartPose.offset(2.5F, 4F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg * 0.8F;
        this.head.xRot = headPitch * deg * 0.8F;
        float swing = Mth.cos(limbSwing * 0.55F) * limbSwingAmount;
        this.rightLeg.xRot = swing * 1.1F;
        this.leftLeg.xRot = -swing * 1.1F;
        float idle = Mth.cos(ageInTicks * 0.07F) * 0.05F;
        float s = this.attackTime > 0.0F ? Mth.sin(this.attackTime * (float) Math.PI) : 0.0F;
        this.rightArm.xRot = (-swing * 0.9F + idle) * (1.0F - s) - 2.0F * s;
        this.leftArm.xRot = (swing * 0.9F - idle) * (1.0F - s) - 2.0F * s;
        this.rightArm.zRot = 0.05F + idle;
        this.leftArm.zRot = -0.05F - idle;
        this.body.xRot = 0.1F + s * 0.3F;
        this.branchR1.zRot = -0.4F + idle;
        this.branchL1.zRot = 0.4F - idle;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
