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
public class PolovykModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "polovyk"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hatBrim;
    private final ModelPart hatCrown;
    private final ModelPart body;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;

    public PolovykModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.hatBrim = root.getChild("head").getChild("hat_brim");
        this.hatCrown = root.getChild("head").getChild("hat_crown");
        this.body = root.getChild("body");
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
        PartDefinition hatBrimDef = headDef.addOrReplaceChild("hat_brim",
                CubeListBuilder.create().texOffs(32, 0).addBox(-7F, -7F, -7F, 14F, 1F, 14F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hatCrownDef = headDef.addOrReplaceChild("hat_crown",
                CubeListBuilder.create().texOffs(88, 0).addBox(-4.5F, -11F, -4.5F, 9F, 4F, 9F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition armRDef = root.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(24, 16).addBox(-3F, -2F, -2F, 4F, 12F, 4F),
                PartPose.offset(-5F, 2F, 0F));
        PartDefinition armLDef = root.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1F, -2F, -2F, 4F, 12F, 4F),
                PartPose.offset(5F, 2F, 0F));
        PartDefinition legRDef = root.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(56, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F),
                PartPose.offset(-1.9F, 12F, 0F));
        PartDefinition legLDef = root.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(72, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F),
                PartPose.offset(1.9F, 12F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg;
        this.head.xRot = headPitch * deg;
        // Парить над землею: плавно погойдується, ноги звисають
        this.root.y = Mth.sin(ageInTicks * 0.08F) * 1.2F - 2.0F;
        float lean = Math.min(limbSwingAmount, 1.0F) * 0.5F;
        this.body.xRot = lean;
        this.head.xRot += lean * 0.4F;
        float sway = Mth.sin(ageInTicks * 0.07F);
        this.legR.xRot = 0.2F + lean + sway * 0.08F;
        this.legL.xRot = 0.2F + lean - sway * 0.08F;
        this.armR.xRot = 0.0F;
        this.armL.xRot = 0.0F;
        this.armR.zRot = 0.35F + Mth.sin(ageInTicks * 0.09F) * 0.06F;
        this.armL.zRot = -0.35F - Mth.sin(ageInTicks * 0.09F) * 0.06F;
        if (entity instanceof Mob mob && mob.isAggressive()) {
            this.armR.xRot = -1.5F + Mth.sin(ageInTicks * 0.6F) * 0.25F;
            this.armL.xRot = -1.5F - Mth.sin(ageInTicks * 0.6F) * 0.25F;
            this.armR.zRot = 0.1F;
            this.armL.zRot = -0.1F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
