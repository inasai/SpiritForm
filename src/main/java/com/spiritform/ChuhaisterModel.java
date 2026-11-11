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
public class ChuhaisterModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "chuhaister"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart beard;
    private final ModelPart antlerR;
    private final ModelPart antlerRTine;
    private final ModelPart antlerL;
    private final ModelPart antlerLTine;
    private final ModelPart body;
    private final ModelPart mantle;
    private final ModelPart cloak;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ChuhaisterModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.beard = root.getChild("head").getChild("beard");
        this.antlerR = root.getChild("head").getChild("antler_r");
        this.antlerRTine = root.getChild("head").getChild("antler_r").getChild("antler_r_tine");
        this.antlerL = root.getChild("head").getChild("antler_l");
        this.antlerLTine = root.getChild("head").getChild("antler_l").getChild("antler_l_tine");
        this.body = root.getChild("body");
        this.mantle = root.getChild("body").getChild("mantle");
        this.cloak = root.getChild("body").getChild("cloak");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -9F, -3.5F, 8F, 9F, 7F),
                PartPose.offsetAndRotation(0F, -4F, -1F, 0.12F, 0F, 0F));
        PartDefinition beardDef = headDef.addOrReplaceChild("beard",
                CubeListBuilder.create().texOffs(30, 0).addBox(-3F, -3F, -5.5F, 6F, 8F, 2F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition antlerRDef = headDef.addOrReplaceChild("antler_r",
                CubeListBuilder.create().texOffs(46, 0).addBox(-0.5F, -10F, -0.5F, 1F, 10F, 1F),
                PartPose.offsetAndRotation(-2.5F, -9F, 0F, 0F, 0F, -0.35F));
        PartDefinition antlerRTineDef = antlerRDef.addOrReplaceChild("antler_r_tine",
                CubeListBuilder.create().texOffs(50, 0).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, -0.8F));
        PartDefinition antlerLDef = headDef.addOrReplaceChild("antler_l",
                CubeListBuilder.create().texOffs(54, 0).addBox(-0.5F, -10F, -0.5F, 1F, 10F, 1F),
                PartPose.offsetAndRotation(2.5F, -9F, 0F, 0F, 0F, 0.35F));
        PartDefinition antlerLTineDef = antlerLDef.addOrReplaceChild("antler_l_tine",
                CubeListBuilder.create().texOffs(58, 0).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -6F, 0F, 0F, 0F, 0.8F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(62, 0).addBox(-6F, 0F, -3.5F, 12F, 15F, 7F),
                PartPose.offsetAndRotation(0F, -4F, 0F, 0.1F, 0F, 0F));
        PartDefinition mantleDef = bodyDef.addOrReplaceChild("mantle",
                CubeListBuilder.create().texOffs(0, 22).addBox(-7.5F, -1F, -4.5F, 15F, 4F, 9F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition cloakDef = bodyDef.addOrReplaceChild("cloak",
                CubeListBuilder.create().texOffs(48, 22).addBox(-6.5F, 2F, 3.4F, 13F, 17F, 2F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(78, 22).addBox(-2.5F, -1F, -2.5F, 5F, 22F, 5F),
                PartPose.offset(-8F, -2F, 0F));
        PartDefinition leftArmDef = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(98, 22).addBox(-2.5F, -1F, -2.5F, 5F, 22F, 5F),
                PartPose.offset(8F, -2F, 0F));
        PartDefinition rightLegDef = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 49).addBox(-3F, 0F, -3F, 6F, 13F, 6F),
                PartPose.offset(-3.5F, 11F, 0F));
        PartDefinition leftLegDef = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(24, 49).addBox(-3F, 0F, -3F, 6F, 13F, 6F),
                PartPose.offset(3.5F, 11F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg * 0.8F;
        this.head.xRot = 0.12F + headPitch * deg * 0.8F;
        float swing = Mth.cos(limbSwing * 0.5F) * limbSwingAmount;
        this.rightLeg.xRot = swing * 0.9F;
        this.leftLeg.xRot = -swing * 0.9F;
        float idle = Mth.cos(ageInTicks * 0.06F) * 0.04F;
        float s = this.attackTime > 0.0F ? Mth.sin(this.attackTime * (float) Math.PI) : 0.0F;
        this.rightArm.xRot = (-swing * 0.7F + idle) * (1.0F - s) - 2.3F * s;
        this.leftArm.xRot = (swing * 0.7F - idle) * (1.0F - s) - 2.3F * s;
        this.rightArm.zRot = 0.06F + idle;
        this.leftArm.zRot = -0.06F - idle;
        this.body.xRot = 0.1F + s * 0.25F;
        this.cloak.xRot = Math.abs(swing) * 0.25F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
