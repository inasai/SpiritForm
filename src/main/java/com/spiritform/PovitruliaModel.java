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
public class PovitruliaModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "povitrulia"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart seg1;
    private final ModelPart seg2;
    private final ModelPart seg3;
    private final ModelPart seg4;
    private final ModelPart seg5;
    private final ModelPart seg6;
    private final ModelPart seg7;
    private final ModelPart seg8;
    private final ModelPart seg9;
    private final ModelPart head;
    private final ModelPart tongue;
    private final ModelPart wingR;
    private final ModelPart wingL;

    public PovitruliaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.seg1 = root.getChild("body").getChild("seg1");
        this.seg2 = root.getChild("body").getChild("seg1").getChild("seg2");
        this.seg3 = root.getChild("body").getChild("seg1").getChild("seg2").getChild("seg3");
        this.seg4 = root.getChild("body").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4");
        this.seg5 = root.getChild("body").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5");
        this.seg6 = root.getChild("body").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("seg6");
        this.seg7 = root.getChild("body").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("seg6").getChild("seg7");
        this.seg8 = root.getChild("body").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("seg6").getChild("seg7").getChild("seg8");
        this.seg9 = root.getChild("body").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("seg6").getChild("seg7").getChild("seg8").getChild("seg9");
        this.head = root.getChild("body").getChild("head");
        this.tongue = root.getChild("body").getChild("head").getChild("tongue");
        this.wingR = root.getChild("body").getChild("wing_r");
        this.wingL = root.getChild("body").getChild("wing_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bodyDef = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, 0F, 6F, 6F, 5F),
                PartPose.offset(0F, 20F, 0F));
        PartDefinition seg1Def = bodyDef.addOrReplaceChild("seg1",
                CubeListBuilder.create().texOffs(22, 0).addBox(-3F, -3F, 0F, 6F, 6F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition seg2Def = seg1Def.addOrReplaceChild("seg2",
                CubeListBuilder.create().texOffs(44, 0).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition seg3Def = seg2Def.addOrReplaceChild("seg3",
                CubeListBuilder.create().texOffs(64, 0).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition seg4Def = seg3Def.addOrReplaceChild("seg4",
                CubeListBuilder.create().texOffs(84, 0).addBox(-2F, -2F, 0F, 4F, 4F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition seg5Def = seg4Def.addOrReplaceChild("seg5",
                CubeListBuilder.create().texOffs(102, 0).addBox(-2F, -2F, 0F, 4F, 4F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition seg6Def = seg5Def.addOrReplaceChild("seg6",
                CubeListBuilder.create().texOffs(0, 11).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition seg7Def = seg6Def.addOrReplaceChild("seg7",
                CubeListBuilder.create().texOffs(16, 11).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition seg8Def = seg7Def.addOrReplaceChild("seg8",
                CubeListBuilder.create().texOffs(32, 11).addBox(-1F, -1F, 0F, 2F, 2F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition seg9Def = seg8Def.addOrReplaceChild("seg9",
                CubeListBuilder.create().texOffs(46, 11).addBox(-1F, -1F, 0F, 2F, 2F, 5F),
                PartPose.offset(0F, 0F, 5F));
        PartDefinition headDef = bodyDef.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(60, 11).addBox(-3F, -3F, -6F, 6F, 6F, 6F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition tongueDef = headDef.addOrReplaceChild("tongue",
                CubeListBuilder.create().texOffs(84, 11).addBox(-0.5F, 0F, -4F, 1F, 0F, 4F),
                PartPose.offset(0F, 1.5F, -6F));
        PartDefinition wingRDef = bodyDef.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(94, 11).addBox(-9F, 0F, -2F, 9F, 0F, 6F),
                PartPose.offset(-3F, -2F, 1F));
        PartDefinition wingLDef = bodyDef.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(0, 23).addBox(0F, 0F, -2F, 9F, 0F, 6F),
                PartPose.offset(3F, -2F, 1F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float deg = (float) Math.PI / 180.0F;
        this.head.yRot = netHeadYaw * deg * 0.5F;
        this.head.xRot = headPitch * deg * 0.5F;
        float t = ageInTicks * 0.2F;
        this.body.y = 20.0F + Mth.sin(ageInTicks * 0.1F) * 0.8F;
        this.body.xRot = 0.15F + limbSwingAmount * 0.3F;
        ModelPart[] segs = {this.seg1, this.seg2, this.seg3, this.seg4, this.seg5, this.seg6, this.seg7, this.seg8, this.seg9};
        for (int i = 0; i < segs.length; i++) {
            segs[i].yRot = Mth.sin(t - i * 0.7F) * 0.35F;
            segs[i].xRot = Mth.cos(t - i * 0.7F) * 0.08F;
        }
        float flap = Mth.sin(ageInTicks * 0.45F) * 0.5F;
        this.wingR.zRot = -0.2F - flap;
        this.wingL.zRot = 0.2F + flap;
        this.tongue.xRot = Mth.sin(ageInTicks * 0.5F) * 0.2F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
