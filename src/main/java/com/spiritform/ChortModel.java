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
public class ChortModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "chort"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hairCap;
    private final ModelPart hairBack;
    private final ModelPart hairSideR;
    private final ModelPart hairSideL;
    private final ModelPart body;
    private final ModelPart skirt;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;

    public ChortModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.hairCap = root.getChild("head").getChild("hair_cap");
        this.hairBack = root.getChild("head").getChild("hair_back");
        this.hairSideR = root.getChild("head").getChild("hair_side_r");
        this.hairSideL = root.getChild("head").getChild("hair_side_l");
        this.body = root.getChild("body");
        this.skirt = root.getChild("body").getChild("skirt");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairCapDef = headDef.addOrReplaceChild("hair_cap",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, -8.5F, -4.5F, 9F, 3F, 9F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairBackDef = headDef.addOrReplaceChild("hair_back",
                CubeListBuilder.create().texOffs(68, 0).addBox(-4.5F, -6F, 3F, 9F, 20F, 2F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairSideRDef = headDef.addOrReplaceChild("hair_side_r",
                CubeListBuilder.create().texOffs(90, 0).addBox(-5F, -7F, -4F, 1F, 7F, 8F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hairSideLDef = headDef.addOrReplaceChild("hair_side_l",
                CubeListBuilder.create().texOffs(108, 0).addBox(4F, -7F, -4F, 1F, 7F, 8F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 22).addBox(-4F, 0F, -2F, 8F, 12F, 4F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition skirtDef = bodyDef.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(24, 22).addBox(-5F, 10F, -3F, 10F, 11F, 6F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition armRDef = root.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(56, 22).addBox(-3F, -2F, -2F, 4F, 12F, 4F),
                PartPose.offset(-5F, 2F, 0F));
        PartDefinition armLDef = root.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(72, 22).addBox(-1F, -2F, -2F, 4F, 12F, 4F),
                PartPose.offset(5F, 2F, 0F));
        PartDefinition legRDef = root.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(88, 22).addBox(-2F, 0F, -2F, 4F, 12F, 4F),
                PartPose.offset(-2F, 12F, 0F));
        PartDefinition legLDef = root.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(104, 22).addBox(-2F, 0F, -2F, 4F, 12F, 4F),
                PartPose.offset(2F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg;
        this.head.xRot = headPitch * deg;
        boolean frozen = entity instanceof Chort c && c.isFrozenPose();
        boolean aggressive = entity instanceof Mob mob && mob.isAggressive();
        float swing = limbSwing * 0.6662F;
        this.legR.xRot = Mth.cos(swing) * 1.2F * limbSwingAmount;
        this.legL.xRot = Mth.cos(swing + (float) Math.PI) * 1.2F * limbSwingAmount;
        this.armR.xRot = Mth.cos(swing + (float) Math.PI) * 1.0F * limbSwingAmount;
        this.armL.xRot = Mth.cos(swing) * 1.0F * limbSwingAmount;
        this.armR.zRot = 0.04F;
        this.armL.zRot = -0.04F;
        if (frozen) {
            // Завмерла: нерухома, голова трохи схилена набік
            this.head.zRot = 0.18F;
            this.armR.xRot = 0.0F;
            this.armL.xRot = 0.0F;
        } else {
            this.head.zRot = 0.0F;
            this.armR.zRot += Mth.cos(ageInTicks * 0.09F) * 0.03F + 0.03F;
            this.armL.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.03F + 0.03F;
        }
        if (aggressive) {
            this.armR.xRot = -1.45F + Mth.sin(ageInTicks * 0.5F) * 0.2F;
            this.armL.xRot = -1.45F - Mth.sin(ageInTicks * 0.5F) * 0.2F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
