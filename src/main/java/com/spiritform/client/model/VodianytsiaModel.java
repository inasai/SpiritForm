package com.spiritform.client.model;

import com.spiritform.SpiritFormMod;
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
public class VodianytsiaModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "vodianytsia"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart cap;
    private final ModelPart hairBack;
    private final ModelPart hairR;
    private final ModelPart hairL;
    private final ModelPart earR;
    private final ModelPart earL;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart fin;

    public VodianytsiaModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.cap = root.getChild("head").getChild("cap");
        this.hairBack = root.getChild("head").getChild("hair_back");
        this.hairR = root.getChild("head").getChild("hair_r");
        this.hairL = root.getChild("head").getChild("hair_l");
        this.earR = root.getChild("head").getChild("ear_r");
        this.earL = root.getChild("head").getChild("ear_l");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.tail1 = root.getChild("tail1");
        this.tail2 = root.getChild("tail1").getChild("tail2");
        this.fin = root.getChild("tail1").getChild("tail2").getChild("fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -6F, -3F, 6F, 6F, 6F),
                PartPose.offset(0F, 12F, 0F));
        PartDefinition capDef = headDef.addOrReplaceChild("cap",
                CubeListBuilder.create().texOffs(24, 0).addBox(-3.5F, -7F, -3.5F, 7F, 1F, 7F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairBackDef = headDef.addOrReplaceChild("hair_back",
                CubeListBuilder.create().texOffs(0, 12).addBox(-3.5F, -6F, 3F, 7F, 13F, 1F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairRDef = headDef.addOrReplaceChild("hair_r",
                CubeListBuilder.create().texOffs(16, 12).addBox(-4F, -6F, -3F, 1F, 9F, 6F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairLDef = headDef.addOrReplaceChild("hair_l",
                CubeListBuilder.create().texOffs(30, 12).addBox(3F, -6F, -3F, 1F, 9F, 6F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition earRDef = headDef.addOrReplaceChild("ear_r",
                CubeListBuilder.create().texOffs(44, 12).addBox(-3F, -1F, -0.5F, 3F, 2F, 1F),
                PartPose.offsetAndRotation(-3F, -3F, 0F, 0F, 0F, 0.3F));
        PartDefinition earLDef = headDef.addOrReplaceChild("ear_l",
                CubeListBuilder.create().texOffs(52, 12).addBox(0F, -1F, -0.5F, 3F, 2F, 1F),
                PartPose.offsetAndRotation(3F, -3F, 0F, 0F, 0F, -0.3F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 27).addBox(-3F, 0F, -1.5F, 6F, 5F, 3F),
                PartPose.offset(0F, 12F, 0F));
        PartDefinition rightArmDef = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(18, 27).addBox(-1F, -1F, -1F, 2F, 7F, 2F),
                PartPose.offset(-4F, 12.5F, 0F));
        PartDefinition leftArmDef = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(26, 27).addBox(-1F, -1F, -1F, 2F, 7F, 2F),
                PartPose.offset(4F, 12.5F, 0F));
        PartDefinition tail1Def = root.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(34, 27).addBox(-2.5F, 0F, -1.5F, 5F, 4F, 3F),
                PartPose.offsetAndRotation(0F, 16.5F, 0F, 0.1F, 0F, 0F));
        PartDefinition tail2Def = tail1Def.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(50, 27).addBox(-1.5F, 0F, -1F, 3F, 3F, 2F),
                PartPose.offset(0F, 4F, 0F));
        PartDefinition finDef = tail2Def.addOrReplaceChild("fin",
                CubeListBuilder.create().texOffs(0, 36).addBox(-4F, 0F, -0.5F, 8F, 2F, 1F),
                PartPose.offset(0F, 3F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg * 0.8F;
        this.head.xRot = headPitch * deg * 0.8F;
        float wave = Mth.sin(ageInTicks * 0.2F + limbSwing * 0.4F);
        float amp = 0.25F + limbSwingAmount * 0.4F;
        this.tail1.xRot = 0.1F + wave * amp;
        this.tail2.xRot = Mth.sin(ageInTicks * 0.2F + limbSwing * 0.4F - 0.9F) * amp * 1.3F;
        this.fin.xRot = Mth.sin(ageInTicks * 0.2F + limbSwing * 0.4F - 1.8F) * amp * 1.5F;
        this.body.xRot = limbSwingAmount * 0.25F;
        this.rightArm.xRot = Mth.sin(ageInTicks * 0.15F) * 0.2F - 0.2F;
        this.leftArm.xRot = -Mth.sin(ageInTicks * 0.15F) * 0.2F - 0.2F;
        this.rightArm.zRot = 0.35F + Mth.cos(ageInTicks * 0.1F) * 0.08F;
        this.leftArm.zRot = -0.35F - Mth.cos(ageInTicks * 0.1F) * 0.08F;
        this.hairBack.xRot = 0.08F + wave * 0.06F + limbSwingAmount * 0.3F;
        this.earR.zRot = 0.3F + Mth.sin(ageInTicks * 0.12F) * 0.08F;
        this.earL.zRot = -0.3F - Mth.sin(ageInTicks * 0.12F) * 0.08F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
