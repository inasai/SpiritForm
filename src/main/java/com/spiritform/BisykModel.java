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
public class BisykModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "bisyk"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart abdomen;
    private final ModelPart head;
    private final ModelPart hornR;
    private final ModelPart hornL;
    private final ModelPart wingR;
    private final ModelPart wingL;
    private final ModelPart legR;
    private final ModelPart legL;

    public BisykModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.abdomen = root.getChild("body").getChild("abdomen");
        this.head = root.getChild("body").getChild("head");
        this.hornR = root.getChild("body").getChild("head").getChild("horn_r");
        this.hornL = root.getChild("body").getChild("head").getChild("horn_l");
        this.wingR = root.getChild("body").getChild("wing_r");
        this.wingL = root.getChild("body").getChild("wing_l");
        this.legR = root.getChild("body").getChild("leg_r");
        this.legL = root.getChild("body").getChild("leg_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -1.5F, 3F, 3F, 3F),
                PartPose.offset(0F, 20F, 0F));
        PartDefinition abdomenDef = bodyDef.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(12, 0).addBox(-1.5F, -1.2F, 0F, 3F, 3F, 4F),
                PartPose.offsetAndRotation(0F, 0F, 1.5F, 0.35F, 0F, 0F));
        PartDefinition headDef = bodyDef.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 7).addBox(-2F, -2F, -3F, 4F, 4F, 3F),
                PartPose.offset(0F, -0.5F, -1.5F));
        PartDefinition hornRDef = headDef.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(14, 7).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F),
                PartPose.offsetAndRotation(-1.2F, -2F, -1.5F, 0F, 0F, -0.35F));
        PartDefinition hornLDef = headDef.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(18, 7).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F),
                PartPose.offsetAndRotation(1.2F, -2F, -1.5F, 0F, 0F, 0.35F));
        PartDefinition wingRDef = bodyDef.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(0, 14).addBox(-5F, 0F, -1.5F, 5F, 0F, 3F),
                PartPose.offset(-1.5F, -1.5F, 0F));
        PartDefinition wingLDef = bodyDef.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(16, 14).addBox(0F, 0F, -1.5F, 5F, 0F, 3F),
                PartPose.offset(1.5F, -1.5F, 0F));
        PartDefinition legRDef = bodyDef.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, 0F, -0.5F, 1F, 2F, 1F),
                PartPose.offset(-0.8F, 1.5F, -0.5F));
        PartDefinition legLDef = bodyDef.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(4, 17).addBox(-0.5F, 0F, -0.5F, 1F, 2F, 1F),
                PartPose.offset(0.8F, 1.5F, -0.5F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg * 0.6F;
        this.head.xRot = headPitch * deg * 0.6F;
        this.body.y = 20.0F + Mth.cos(ageInTicks * 0.3F) * 0.6F;
        this.body.xRot = 0.25F + limbSwingAmount * 0.5F;
        float flap = Mth.cos(ageInTicks * 2.4F) * 0.9F;
        this.wingR.zRot = -0.25F - flap;
        this.wingL.zRot = 0.25F + flap;
        this.abdomen.xRot = 0.35F + Mth.sin(ageInTicks * 0.2F) * 0.1F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
