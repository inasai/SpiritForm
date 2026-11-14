package com.spiritform.client.model;

import com.spiritform.SpiritFormMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Модель істоти. Частини й розкладка текстури створені генератором, анімація написана вручну. */
public class PotercharModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "potercha"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart cap;
    private final ModelPart hairBack;
    private final ModelPart hairR;
    private final ModelPart hairL;
    private final ModelPart body;
    private final ModelPart dress;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public PotercharModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.cap = root.getChild("head").getChild("cap");
        this.hairBack = root.getChild("head").getChild("hair_back");
        this.hairR = root.getChild("head").getChild("hair_r");
        this.hairL = root.getChild("head").getChild("hair_l");
        this.body = root.getChild("body");
        this.dress = root.getChild("body").getChild("dress");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7F, -3.5F, 7F, 7F, 7F),
                PartPose.offset(0F, 6F, 0F));
        PartDefinition capDef = headDef.addOrReplaceChild("cap",
                CubeListBuilder.create().texOffs(28, 0).addBox(-4F, -7.6F, -4F, 8F, 1F, 8F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairBackDef = headDef.addOrReplaceChild("hair_back",
                CubeListBuilder.create().texOffs(0, 14).addBox(-4F, -6.5F, 3.6F, 8F, 14F, 1F, new CubeDeformation(-0.2F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairRDef = headDef.addOrReplaceChild("hair_r",
                CubeListBuilder.create().texOffs(18, 14).addBox(-4.4F, -6.5F, -3.5F, 1F, 9F, 7F, new CubeDeformation(-0.2F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairLDef = headDef.addOrReplaceChild("hair_l",
                CubeListBuilder.create().texOffs(34, 14).addBox(3.4F, -6.5F, -3.5F, 1F, 9F, 7F, new CubeDeformation(-0.2F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 30).addBox(-3F, 0F, -2F, 6F, 9F, 4F),
                PartPose.offset(0F, 6F, 0F));
        PartDefinition dressDef = bodyDef.addOrReplaceChild("dress",
                CubeListBuilder.create().texOffs(20, 30).addBox(-4.5F, 8F, -3F, 9F, 9F, 6F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(50, 30).addBox(-1F, -1F, -1F, 2F, 9F, 2F),
                PartPose.offset(-4F, 7F, 0F));
        PartDefinition leftArmDef = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 45).addBox(-1F, -1F, -1F, 2F, 9F, 2F),
                PartPose.offset(4F, 7F, 0F));
        PartDefinition rightLegDef = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(8, 45).addBox(-1F, 0F, -1F, 2F, 7F, 2F),
                PartPose.offset(-1.5F, 17F, 0F));
        PartDefinition leftLegDef = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 45).addBox(-1F, 0F, -1F, 2F, 7F, 2F),
                PartPose.offset(1.5F, 17F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg * 0.8F;
        this.head.xRot = 0.1F + headPitch * deg * 0.8F;
        float swing = Mth.cos(limbSwing * 0.7F) * limbSwingAmount;
        this.rightLeg.xRot = swing * 1.1F;
        this.leftLeg.xRot = -swing * 1.1F;
        this.rightArm.xRot = -swing * 0.8F;
        this.leftArm.xRot = swing * 0.8F;
        this.rightArm.zRot = 0.08F + Mth.cos(ageInTicks * 0.08F) * 0.03F;
        this.leftArm.zRot = -0.08F - Mth.cos(ageInTicks * 0.08F) * 0.03F;
        this.hairBack.xRot = 0.05F + limbSwingAmount * 0.5F + Mth.sin(ageInTicks * 0.1F) * 0.03F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
