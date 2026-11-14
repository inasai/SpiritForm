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
import net.minecraft.world.entity.Mob;

/** Модель істоти. Частини й розкладка текстури створені генератором, анімація написана вручну. */
public class LisovykModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "lisovyk"), "main");

    private final ModelPart root;
    private final ModelPart legR;
    private final ModelPart legL;
    private final ModelPart body;
    private final ModelPart mossShR;
    private final ModelPart mossShL;
    private final ModelPart mossBack;
    private final ModelPart head;
    private final ModelPart mossCrown;
    private final ModelPart beard;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart handR;
    private final ModelPart handL;

    public LisovykModel(ModelPart root) {
        this.root = root;
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.body = root.getChild("body");
        this.mossShR = root.getChild("body").getChild("moss_sh_r");
        this.mossShL = root.getChild("body").getChild("moss_sh_l");
        this.mossBack = root.getChild("body").getChild("moss_back");
        this.head = root.getChild("head");
        this.mossCrown = root.getChild("head").getChild("moss_crown");
        this.beard = root.getChild("head").getChild("beard");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.handR = root.getChild("arm_r").getChild("hand_r");
        this.handL = root.getChild("arm_l").getChild("hand_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition legRDef = root.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, 0F, -3F, 6F, 13F, 6F),
                PartPose.offset(-4F, 11F, 0F));
        PartDefinition legLDef = root.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(24, 0).addBox(-3F, 0F, -3F, 6F, 13F, 6F),
                PartPose.offset(4F, 11F, 0F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(48, 0).addBox(-7F, -16F, -4F, 14F, 16F, 8F),
                PartPose.offset(0F, 11F, 0F));
        PartDefinition mossShRDef = bodyDef.addOrReplaceChild("moss_sh_r",
                CubeListBuilder.create().texOffs(92, 0).addBox(-8.5F, -18.5F, -4.5F, 8F, 3F, 9F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition mossShLDef = bodyDef.addOrReplaceChild("moss_sh_l",
                CubeListBuilder.create().texOffs(0, 24).addBox(0.5F, -19F, -4.5F, 8F, 4F, 9F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition mossBackDef = bodyDef.addOrReplaceChild("moss_back",
                CubeListBuilder.create().texOffs(34, 24).addBox(-5F, -14F, 4F, 10F, 12F, 3F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(60, 24).addBox(-4.5F, -9F, -4.5F, 9F, 9F, 9F),
                PartPose.offsetAndRotation(0F, -5F, -1F, 0.1F, 0F, 0F));
        PartDefinition mossCrownDef = headDef.addOrReplaceChild("moss_crown",
                CubeListBuilder.create().texOffs(0, 42).addBox(-5F, -10.5F, -5F, 10F, 3F, 10F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition beardDef = headDef.addOrReplaceChild("beard",
                CubeListBuilder.create().texOffs(40, 42).addBox(-3.5F, -1F, -5.5F, 7F, 6F, 3F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition armRDef = root.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(60, 42).addBox(-2.5F, -1F, -2.5F, 5F, 20F, 5F),
                PartPose.offset(-9.5F, -2F, 0F));
        PartDefinition armLDef = root.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(80, 42).addBox(-2.5F, -1F, -2.5F, 5F, 20F, 5F),
                PartPose.offset(9.5F, -2F, 0F));
        PartDefinition handRDef = armRDef.addOrReplaceChild("hand_r",
                CubeListBuilder.create().texOffs(100, 42).addBox(-3.5F, 0F, -3.5F, 7F, 6F, 7F),
                PartPose.offset(0F, 19F, 0F));
        PartDefinition handLDef = armLDef.addOrReplaceChild("hand_l",
                CubeListBuilder.create().texOffs(0, 67).addBox(-3.5F, 0F, -3.5F, 7F, 6F, 7F),
                PartPose.offset(0F, 19F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg * 0.8F;
        this.head.xRot = 0.1F + headPitch * deg * 0.8F;
        float swing = limbSwing * 0.5F;
        this.legR.xRot = Mth.cos(swing) * 1.1F * limbSwingAmount;
        this.legL.xRot = Mth.cos(swing + (float) Math.PI) * 1.1F * limbSwingAmount;
        this.body.yRot = Mth.cos(swing) * 0.12F * limbSwingAmount;
        this.armR.xRot = Mth.cos(swing + (float) Math.PI) * 0.8F * limbSwingAmount;
        this.armL.xRot = Mth.cos(swing) * 0.8F * limbSwingAmount;
        this.armR.zRot = 0.08F + Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.armL.zRot = -0.08F - Mth.sin(ageInTicks * 0.05F) * 0.03F;
        if (entity instanceof Mob mob && mob.isAggressive()) {
            this.armR.xRot = -1.3F + Mth.sin(ageInTicks * 0.45F) * 0.45F;
            this.armL.xRot = -1.3F - Mth.sin(ageInTicks * 0.45F) * 0.45F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
