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

/** Багатько: вогняна кулька з трьома язиками полум'я зверху. */
public class BahatkoModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "bahatko"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] flames = new ModelPart[3];

    public BahatkoModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < 3; i++) {
            this.flames[i] = this.body.getChild("flame" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, -3F, 6F, 6F, 6F),
                PartPose.offset(0F, 20F, 0F));
        body.addOrReplaceChild("flame0",
                CubeListBuilder.create().texOffs(0, 12).addBox(-1F, -4F, -1F, 2F, 4F, 2F),
                PartPose.offset(-1.5F, -2.5F, 0.5F));
        body.addOrReplaceChild("flame1",
                CubeListBuilder.create().texOffs(8, 12).addBox(-1F, -5F, -1F, 2F, 5F, 2F),
                PartPose.offset(1.5F, -2.5F, -0.5F));
        body.addOrReplaceChild("flame2",
                CubeListBuilder.create().texOffs(16, 12).addBox(-1F, -3F, -1F, 2F, 3F, 2F),
                PartPose.offset(0F, -2.5F, 1.5F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.body.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.body.y = 20.0F + Mth.sin(ageInTicks * 0.2F) * 1.0F;
        for (int i = 0; i < 3; i++) {
            this.flames[i].xRot = Mth.sin(ageInTicks * 0.3F + i * 2.0F) * 0.25F;
            this.flames[i].zRot = Mth.cos(ageInTicks * 0.27F + i * 1.7F) * 0.25F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
