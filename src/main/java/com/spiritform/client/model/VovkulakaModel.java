package com.spiritform.client.model;

import com.spiritform.SpiritFormMod;
import com.spiritform.entity.Vovkulaka;
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
public class VovkulakaModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "vovkulaka"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart mane;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart earR;
    private final ModelPart earL;
    private final ModelPart tail;
    private final ModelPart legFr;
    private final ModelPart legFl;
    private final ModelPart legBr;
    private final ModelPart legBl;

    public VovkulakaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.mane = root.getChild("body").getChild("mane");
        this.head = root.getChild("head");
        this.snout = root.getChild("head").getChild("snout");
        this.earR = root.getChild("head").getChild("ear_r");
        this.earL = root.getChild("head").getChild("ear_l");
        this.tail = root.getChild("tail");
        this.legFr = root.getChild("leg_fr");
        this.legFl = root.getChild("leg_fl");
        this.legBr = root.getChild("leg_br");
        this.legBl = root.getChild("leg_bl");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -4F, -7F, 8F, 8F, 14F),
                PartPose.offset(0F, 14F, 0F));
        PartDefinition maneDef = bodyDef.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(44, 0).addBox(-4.5F, -4.5F, -7.5F, 9F, 9F, 7F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition headDef = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(76, 0).addBox(-3.5F, -3.5F, -6F, 7F, 7F, 6F),
                PartPose.offset(0F, 12F, -7F));
        PartDefinition snoutDef = headDef.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(102, 0).addBox(-2F, -0.5F, -4F, 4F, 3F, 4F),
                PartPose.offset(0F, 0.5F, -6F));
        PartDefinition earRDef = headDef.addOrReplaceChild("ear_r",
                CubeListBuilder.create().texOffs(118, 0).addBox(-1F, -3F, -0.5F, 2F, 3F, 1F),
                PartPose.offset(-2.3F, -3.5F, -2F));
        PartDefinition earLDef = headDef.addOrReplaceChild("ear_l",
                CubeListBuilder.create().texOffs(0, 22).addBox(-1F, -3F, -0.5F, 2F, 3F, 1F),
                PartPose.offset(2.3F, -3.5F, -2F));
        PartDefinition tailDef = root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(6, 22).addBox(-1.5F, 0F, -1.5F, 3F, 9F, 3F),
                PartPose.offsetAndRotation(0F, 11F, 7F, 1F, 0F, 0F));
        PartDefinition legFrDef = root.addOrReplaceChild("leg_fr",
                CubeListBuilder.create().texOffs(18, 22).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F),
                PartPose.offset(-2.6F, 16F, -5.5F));
        PartDefinition legFlDef = root.addOrReplaceChild("leg_fl",
                CubeListBuilder.create().texOffs(30, 22).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F),
                PartPose.offset(2.6F, 16F, -5.5F));
        PartDefinition legBrDef = root.addOrReplaceChild("leg_br",
                CubeListBuilder.create().texOffs(42, 22).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F),
                PartPose.offset(-2.6F, 16F, 5.5F));
        PartDefinition legBlDef = root.addOrReplaceChild("leg_bl",
                CubeListBuilder.create().texOffs(54, 22).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F),
                PartPose.offset(2.6F, 16F, 5.5F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        boolean sitting = entity instanceof Vovkulaka v && v.isOrderedToSit() && !v.isVehicle();
        this.body.xRot = 0.0F;
        this.body.y = 14.0F;
        this.head.y = 12.0F;
        this.head.z = -7.0F;
        if (sitting) {
            this.body.xRot = -0.55F;
            this.body.y = 17.0F;
            this.head.y = 9.0F;
            this.head.z = -6.0F;
            this.legBr.xRot = -1.45F;
            this.legBl.xRot = -1.45F;
            this.legBr.y = 20.0F;
            this.legBl.y = 20.0F;
            this.legFr.xRot = 0.0F;
            this.legFl.xRot = 0.0F;
            this.tail.xRot = 1.5F;
        } else {
            this.legBr.y = 16.0F;
            this.legBl.y = 16.0F;
            float swing = limbSwing * 0.6662F;
            this.legBr.xRot = Mth.cos(swing) * 1.4F * limbSwingAmount;
            this.legBl.xRot = Mth.cos(swing + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.legFr.xRot = Mth.cos(swing + (float) Math.PI) * 1.4F * limbSwingAmount;
            this.legFl.xRot = Mth.cos(swing) * 1.4F * limbSwingAmount;
            this.tail.xRot = 1.0F - Math.min(limbSwingAmount, 1.0F) * 0.5F;
        }
        this.tail.yRot = Mth.sin(ageInTicks * 0.1F) * 0.15F;
        this.head.yRot = netHeadYaw * deg;
        this.head.xRot = headPitch * deg;
        if (entity instanceof Mob mob && mob.isAggressive()) {
            this.head.xRot += 0.25F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
