package com.spiritform.client.model;

import com.spiritform.SpiritFormMod;
import com.spiritform.herald.HeraldBase;
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

/**
 * Модель Вісників Потойбіччя. Один клас на всі чотири види: кубики кожного виду описані нижче,
 * а їхня розгортка збігається з текстурами textures/entity/(storm|hunger|cold|death)_herald.png (128x128).
 * Вісник Бурі має воронку (spin1..spin4), що обертається, і коливається в повітрі.
 */
public class HeraldModel extends EntityModel<HeraldBase> {

    public enum Variant { STORM, HUNGER, COLD, DEATH }

    public static ModelLayerLocation layer(Variant variant) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID,
                variant.name().toLowerCase(java.util.Locale.ROOT) + "_herald"), "main");
    }

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart[] spins;
    private final boolean floating;

    public HeraldModel(ModelPart root, Variant variant) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.hasChild("right_leg") ? root.getChild("right_leg") : null;
        this.leftLeg = root.hasChild("left_leg") ? root.getChild("left_leg") : null;
        this.spins = root.hasChild("spin1")
                ? new ModelPart[] {root.getChild("spin1"), root.getChild("spin2"), root.getChild("spin3"), root.getChild("spin4")}
                : new ModelPart[0];
        this.floating = variant == Variant.STORM;
    }

    public static LayerDefinition create(Variant variant) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        switch (variant) {
            case STORM -> storm(parts);
            case HUNGER -> hunger(parts);
            case COLD -> cold(parts);
            case DEATH -> death(parts);
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void storm(PartDefinition parts) {
        parts.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F)
                        .texOffs(32, 0).addBox(-5F, -12F, -1F, 2F, 5F, 2F)
                        .texOffs(40, 0).addBox(3F, -12F, -1F, 2F, 5F, 2F),
                PartPose.offset(0F, -4F, 0F));
        parts.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(48, 0).addBox(-4F, -12F, -2F, 8F, 12F, 4F),
                PartPose.offset(0F, 8F, 0F));
        parts.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(72, 0).addBox(-3F, -2F, -2F, 4F, 14F, 4F),
                PartPose.offset(-6F, -2F, 0F));
        parts.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(88, 0).addBox(-1F, -2F, -2F, 4F, 14F, 4F),
                PartPose.offset(6F, -2F, 0F));
        parts.addOrReplaceChild("spin1",
                CubeListBuilder.create()
                        .texOffs(104, 0).addBox(-3F, 0F, -3F, 6F, 4F, 6F),
                PartPose.offset(0F, 20F, 0F));
        parts.addOrReplaceChild("spin2",
                CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-5F, 0F, -5F, 10F, 4F, 10F),
                PartPose.offset(0F, 16F, 0F));
        parts.addOrReplaceChild("spin3",
                CubeListBuilder.create()
                        .texOffs(40, 18).addBox(-7F, 0F, -7F, 14F, 4F, 14F),
                PartPose.offset(0F, 12F, 0F));
        parts.addOrReplaceChild("spin4",
                CubeListBuilder.create()
                        .texOffs(0, 36).addBox(-9F, 0F, -9F, 18F, 4F, 18F),
                PartPose.offset(0F, 8F, 0F));
    }

    private static void hunger(PartDefinition parts) {
        parts.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F)
                        .texOffs(32, 0).addBox(-3F, 0F, -4F, 6F, 3F, 1F),
                PartPose.offset(0F, -6F, 0F));
        parts.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(46, 0).addBox(-4F, -14F, -2F, 8F, 14F, 4F)
                        .texOffs(70, 0).addBox(-5F, -12F, -3F, 10F, 1F, 1F)
                        .texOffs(92, 0).addBox(-5F, -9F, -3F, 10F, 1F, 1F)
                        .texOffs(0, 18).addBox(-5F, -6F, -3F, 10F, 1F, 1F),
                PartPose.offset(0F, 8F, 0F));
        parts.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(22, 18).addBox(-2F, -1F, -2F, 3F, 20F, 3F),
                PartPose.offset(-6F, -4F, 0F));
        parts.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(34, 18).addBox(-1F, -1F, -2F, 3F, 20F, 3F),
                PartPose.offset(6F, -4F, 0F));
        parts.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(46, 18).addBox(-1F, 0F, -1F, 3F, 16F, 3F),
                PartPose.offset(-2F, 8F, 0F));
        parts.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(58, 18).addBox(-2F, 0F, -1F, 3F, 16F, 3F),
                PartPose.offset(2F, 8F, 0F));
    }

    private static void cold(PartDefinition parts) {
        parts.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F)
                        .texOffs(32, 0).addBox(-3F, -12F, -3F, 2F, 4F, 2F)
                        .texOffs(40, 0).addBox(1F, -12F, -3F, 2F, 4F, 2F)
                        .texOffs(48, 0).addBox(-1F, -14F, -1F, 2F, 6F, 2F)
                        .texOffs(56, 0).addBox(-3F, -12F, 1F, 2F, 4F, 2F)
                        .texOffs(64, 0).addBox(1F, -12F, 1F, 2F, 4F, 2F),
                PartPose.offset(0F, -2F, 0F));
        parts.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(72, 0).addBox(-6F, -14F, -3F, 12F, 14F, 7F),
                PartPose.offset(0F, 12F, 0F));
        parts.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 21).addBox(-3F, -2F, -3F, 5F, 14F, 5F)
                        .texOffs(20, 21).addBox(-4F, -6F, -2F, 3F, 5F, 3F),
                PartPose.offset(-9F, 0F, 0F));
        parts.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(32, 21).addBox(-2F, -2F, -3F, 5F, 14F, 5F)
                        .texOffs(52, 21).addBox(1F, -6F, -2F, 3F, 5F, 3F),
                PartPose.offset(9F, 0F, 0F));
        parts.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(64, 21).addBox(-3F, 0F, -3F, 5F, 12F, 5F),
                PartPose.offset(-3F, 12F, 0F));
        parts.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(84, 21).addBox(-2F, 0F, -3F, 5F, 12F, 5F),
                PartPose.offset(3F, 12F, 0F));
    }

    private static void death(PartDefinition parts) {
        parts.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F)
                        .texOffs(32, 0).addBox(-6F, -14F, -1F, 2F, 7F, 2F)
                        .texOffs(40, 0).addBox(4F, -14F, -1F, 2F, 7F, 2F)
                        .texOffs(48, 0).addBox(-5F, -16F, -1F, 2F, 3F, 2F)
                        .texOffs(56, 0).addBox(3F, -16F, -1F, 2F, 3F, 2F),
                PartPose.offset(0F, -6F, 0F));
        parts.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(64, 0).addBox(-5F, -16F, -3F, 10F, 16F, 6F)
                        .texOffs(96, 0).addBox(-6F, -16F, 3F, 12F, 28F, 1F),
                PartPose.offset(0F, 10F, 0F));
        parts.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 29).addBox(-3F, -2F, -2F, 4F, 18F, 4F)
                        .texOffs(16, 29).addBox(-4F, -4F, -3F, 5F, 3F, 6F),
                PartPose.offset(-7F, -4F, 0F));
        parts.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(38, 29).addBox(-1F, -2F, -2F, 4F, 18F, 4F)
                        .texOffs(54, 29).addBox(-1F, -4F, -3F, 5F, 3F, 6F),
                PartPose.offset(7F, -4F, 0F));
        parts.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(76, 29).addBox(-2F, 0F, -2F, 4F, 14F, 4F),
                PartPose.offset(-2F, 10F, 0F));
        parts.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(92, 29).addBox(-2F, 0F, -2F, 4F, 14F, 4F),
                PartPose.offset(2F, 10F, 0F));
    }

    @Override
    public void setupAnim(HeraldBase entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float degrees = (float) Math.PI / 180.0F;
        head.yRot = netHeadYaw * degrees;
        head.xRot = headPitch * degrees;

        float swing = limbSwing * 0.6662F;
        float raise = entity.isAggressive() ? -1.4F : 0.0F;
        float idle = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        rightArm.xRot = raise + Mth.cos(swing + (float) Math.PI) * 0.9F * limbSwingAmount + idle;
        leftArm.xRot = raise + Mth.cos(swing) * 0.9F * limbSwingAmount - idle;
        rightArm.zRot = 0.06F + Mth.cos(ageInTicks * 0.09F) * 0.05F;
        leftArm.zRot = -0.06F - Mth.cos(ageInTicks * 0.09F) * 0.05F;

        if (rightLeg != null && leftLeg != null) {
            rightLeg.xRot = Mth.cos(swing) * 1.2F * limbSwingAmount;
            leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * 1.2F * limbSwingAmount;
        }
        for (int i = 0; i < spins.length; i++) {
            spins[i].yRot = ageInTicks * (0.16F + 0.05F * i) * (i % 2 == 0 ? 1.0F : -1.0F);
        }
        root.y = floating ? Mth.sin(ageInTicks * 0.1F) * 1.5F : 0.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
