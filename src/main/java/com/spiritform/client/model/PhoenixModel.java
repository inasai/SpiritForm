package com.spiritform.client.model;

import com.spiritform.SpiritFormMod;
import com.spiritform.entity.Phoenix;
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

/**
 * Жар-птиця: хижий птах із гачкуватим дзьобом, насупленими бровами, вогняним гребенем, крилами з маховим пір'ям
 * і віялом хвоста. Усі частини й розкладка текстури 128x128 створені генератором (текстура зібрана під ці UV).
 * Скриня на боках видно лише тоді, коли птах її несе.
 */
public class PhoenixModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "phoenix"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart wingR;
    private final ModelPart foreR;
    private final ModelPart wingL;
    private final ModelPart foreL;
    private final ModelPart chestL;
    private final ModelPart chestR;
    private final ModelPart[] tail = new ModelPart[5];

    public PhoenixModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.wingR = body.getChild("wing_r");
        this.foreR = wingR.getChild("fore_r");
        this.wingL = body.getChild("wing_l");
        this.foreL = wingL.getChild("fore_l");
        this.chestL = body.getChild("chest_l");
        this.chestR = body.getChild("chest_r");
        for (int i = 0; i < 5; i++) {
            this.tail[i] = body.getChild("tail" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -4F, -8F, 8F, 8F, 16F),
                PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, 0F));
        PartDefinition p_keel = p_body.addOrReplaceChild("keel",
                CubeListBuilder.create().texOffs(30, 40).addBox(-2.5F, 3F, -7F, 5F, 2F, 10F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(60, 40).addBox(-2.5F, -2.5F, -5F, 5F, 5F, 5F),
                PartPose.offsetAndRotation(0F, -1.5F, -7.5F, -0.6F, 0F, 0F));
        PartDefinition p_head = p_neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(80, 40).addBox(-2.5F, -2.5F, -5F, 5F, 5F, 5F),
                PartPose.offsetAndRotation(0F, -0.5F, -4.5F, 0.55F, 0F, 0F));
        PartDefinition p_brow = p_head.addOrReplaceChild("brow",
                CubeListBuilder.create().texOffs(32, 71).addBox(-3F, -3.2F, -5F, 6F, 1F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_beak = p_head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(84, 63).addBox(-1.5F, -1F, -9F, 3F, 3F, 4F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_hook = p_head.addOrReplaceChild("hook",
                CubeListBuilder.create().texOffs(50, 71).addBox(-1F, 1.5F, -9F, 2F, 3F, 1F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(56, 71).addBox(-1F, 1.8F, -8F, 2F, 1F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_crest0 = p_head.addOrReplaceChild("crest0",
                CubeListBuilder.create().texOffs(98, 63).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -2.5F, -1.5F, -0.3F, 0F, 0F));
        PartDefinition p_crest1 = p_head.addOrReplaceChild("crest1",
                CubeListBuilder.create().texOffs(102, 63).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -2.5F, 0F, -0.55F, 0F, 0F));
        PartDefinition p_crest2 = p_head.addOrReplaceChild("crest2",
                CubeListBuilder.create().texOffs(106, 63).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -2.5F, 1.5F, -0.8F, 0F, 0F));
        PartDefinition p_crest3 = p_head.addOrReplaceChild("crest3",
                CubeListBuilder.create().texOffs(110, 63).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -2.5F, 3F, -1.05F, 0F, 0F));
        PartDefinition p_crest4 = p_head.addOrReplaceChild("crest4",
                CubeListBuilder.create().texOffs(114, 63).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(0F, -2.5F, 4.5F, -1.3F, 0F, 0F));
        PartDefinition p_horn_l = p_head.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(80, 54).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 7F),
                PartPose.offsetAndRotation(2.5F, -1.5F, 0F, 0.2F, 0.3F, 0F));
        PartDefinition p_horn_r = p_head.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(96, 54).addBox(-0.5F, -0.5F, 0F, 1F, 1F, 7F),
                PartPose.offsetAndRotation(-2.5F, -1.5F, 0F, 0.2F, -0.3F, 0F));
        PartDefinition p_wing_r = p_body.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(0, 54).addBox(-13F, -1F, -3.5F, 13F, 2F, 7F),
                PartPose.offsetAndRotation(-4F, -3F, -1F, 0F, 0F, 0F));
        PartDefinition p_fore_r = p_wing_r.addOrReplaceChild("fore_r",
                CubeListBuilder.create().texOffs(0, 63).addBox(-11F, -0.75F, -3F, 11F, 2F, 6F),
                PartPose.offsetAndRotation(-13F, 0F, 0F, 0F, 0.2F, 0F));
        PartDefinition p_blade_r0 = p_fore_r.addOrReplaceChild("blade_r0",
                CubeListBuilder.create().texOffs(0, 76).addBox(-13F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(-11F, 0F, -2F, 0F, -0.1F, 0F));
        PartDefinition p_blade_r1 = p_fore_r.addOrReplaceChild("blade_r1",
                CubeListBuilder.create().texOffs(30, 76).addBox(-13F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(-11F, 0F, -1F, 0F, 0.05F, 0F));
        PartDefinition p_blade_r2 = p_fore_r.addOrReplaceChild("blade_r2",
                CubeListBuilder.create().texOffs(60, 76).addBox(-13F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(-11F, 0F, 0F, 0F, 0.2F, 0F));
        PartDefinition p_blade_r3 = p_fore_r.addOrReplaceChild("blade_r3",
                CubeListBuilder.create().texOffs(90, 76).addBox(-13F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(-11F, 0F, 1F, 0F, 0.35F, 0F));
        PartDefinition p_blade_r4 = p_fore_r.addOrReplaceChild("blade_r4",
                CubeListBuilder.create().texOffs(0, 79).addBox(-13F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(-11F, 0F, 2F, 0F, 0.5F, 0F));
        PartDefinition p_wing_l = p_body.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(40, 54).addBox(0F, -1F, -3.5F, 13F, 2F, 7F),
                PartPose.offsetAndRotation(4F, -3F, -1F, 0F, 0F, 0F));
        PartDefinition p_fore_l = p_wing_l.addOrReplaceChild("fore_l",
                CubeListBuilder.create().texOffs(34, 63).addBox(0F, -0.75F, -3F, 11F, 2F, 6F),
                PartPose.offsetAndRotation(13F, 0F, 0F, 0F, -0.2F, 0F));
        PartDefinition p_blade_l0 = p_fore_l.addOrReplaceChild("blade_l0",
                CubeListBuilder.create().texOffs(30, 79).addBox(0F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(11F, 0F, -2F, 0F, 0.1F, 0F));
        PartDefinition p_blade_l1 = p_fore_l.addOrReplaceChild("blade_l1",
                CubeListBuilder.create().texOffs(60, 79).addBox(0F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(11F, 0F, -1F, 0F, -0.05F, 0F));
        PartDefinition p_blade_l2 = p_fore_l.addOrReplaceChild("blade_l2",
                CubeListBuilder.create().texOffs(90, 79).addBox(0F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(11F, 0F, 0F, 0F, -0.2F, 0F));
        PartDefinition p_blade_l3 = p_fore_l.addOrReplaceChild("blade_l3",
                CubeListBuilder.create().texOffs(0, 82).addBox(0F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(11F, 0F, 1F, 0F, -0.35F, 0F));
        PartDefinition p_blade_l4 = p_fore_l.addOrReplaceChild("blade_l4",
                CubeListBuilder.create().texOffs(30, 82).addBox(0F, -0.5F, -1F, 13F, 1F, 2F),
                PartPose.offsetAndRotation(11F, 0F, 2F, 0F, -0.5F, 0F));
        PartDefinition p_tail0 = p_body.addOrReplaceChild("tail0",
                CubeListBuilder.create().texOffs(78, 24).addBox(-1F, -0.5F, 0F, 2F, 1F, 13F),
                PartPose.offsetAndRotation(0F, -1F, 7.5F, -0.15F, -0.5F, 0F));
        PartDefinition p_tail1 = p_body.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(86, 0).addBox(-1F, -0.5F, 0F, 2F, 1F, 15F),
                PartPose.offsetAndRotation(0F, -1F, 7.5F, -0.15F, -0.25F, 0F));
        PartDefinition p_tail2 = p_body.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1F, -0.5F, 0F, 2F, 1F, 17F),
                PartPose.offsetAndRotation(0F, -1F, 7.5F, -0.15F, 0F, 0F));
        PartDefinition p_tail3 = p_body.addOrReplaceChild("tail3",
                CubeListBuilder.create().texOffs(0, 24).addBox(-1F, -0.5F, 0F, 2F, 1F, 15F),
                PartPose.offsetAndRotation(0F, -1F, 7.5F, -0.15F, 0.25F, 0F));
        PartDefinition p_tail4 = p_body.addOrReplaceChild("tail4",
                CubeListBuilder.create().texOffs(0, 40).addBox(-1F, -0.5F, 0F, 2F, 1F, 13F),
                PartPose.offsetAndRotation(0F, -1F, 7.5F, -0.15F, 0.5F, 0F));
        PartDefinition p_leg_r = p_body.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(68, 63).addBox(-1F, 0F, -1F, 2F, 6F, 2F),
                PartPose.offsetAndRotation(-2.5F, 3F, 2F, 0.3F, 0F, 0F));
        PartDefinition p_foot_r = p_leg_r.addOrReplaceChild("foot_r",
                CubeListBuilder.create().texOffs(0, 71).addBox(-2F, 0F, -3F, 4F, 1F, 4F),
                PartPose.offsetAndRotation(0F, 6F, 0F, -0.3F, 0F, 0F));
        PartDefinition p_claw_r0 = p_foot_r.addOrReplaceChild("claw_r0",
                CubeListBuilder.create().texOffs(66, 71).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(-1.4F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_claw_r1 = p_foot_r.addOrReplaceChild("claw_r1",
                CubeListBuilder.create().texOffs(74, 71).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_claw_r2 = p_foot_r.addOrReplaceChild("claw_r2",
                CubeListBuilder.create().texOffs(82, 71).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(1.4F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leg_l = p_body.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(76, 63).addBox(-1F, 0F, -1F, 2F, 6F, 2F),
                PartPose.offsetAndRotation(2.5F, 3F, 2F, 0.3F, 0F, 0F));
        PartDefinition p_foot_l = p_leg_l.addOrReplaceChild("foot_l",
                CubeListBuilder.create().texOffs(16, 71).addBox(-2F, 0F, -3F, 4F, 1F, 4F),
                PartPose.offsetAndRotation(0F, 6F, 0F, -0.3F, 0F, 0F));
        PartDefinition p_claw_l0 = p_foot_l.addOrReplaceChild("claw_l0",
                CubeListBuilder.create().texOffs(90, 71).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(-1.4F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_claw_l1 = p_foot_l.addOrReplaceChild("claw_l1",
                CubeListBuilder.create().texOffs(98, 71).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_claw_l2 = p_foot_l.addOrReplaceChild("claw_l2",
                CubeListBuilder.create().texOffs(106, 71).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(1.4F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_chest_l = p_body.addOrReplaceChild("chest_l",
                CubeListBuilder.create().texOffs(34, 24).addBox(0F, -1F, -4F, 3F, 8F, 8F),
                PartPose.offsetAndRotation(4F, 0F, 3F, 0F, 0F, 0F));
        PartDefinition p_chest_r = p_body.addOrReplaceChild("chest_r",
                CubeListBuilder.create().texOffs(56, 24).addBox(-3F, -1F, -4F, 3F, 8F, 8F),
                PartPose.offsetAndRotation(-4F, 0F, 3F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        boolean resting = entity instanceof Phoenix p && p.isResting();
        boolean chest = entity instanceof Phoenix p && p.hasChest();
        this.chestL.visible = chest;
        this.chestR.visible = chest;
        if (resting) {
            // Спить на гнізді: крила складені, голова втягнута.
            this.wingR.zRot = -1.25F;
            this.wingL.zRot = 1.25F;
            this.foreR.zRot = 0.2F;
            this.foreL.zRot = -0.2F;
            this.body.y = 14.0F;
            this.body.xRot = 0.0F;
            this.neck.xRot = -0.2F;
            this.head.xRot = 0.9F;
            this.head.yRot = 0.0F;
        } else {
            float flap = Mth.cos(ageInTicks * 0.45F) * 0.55F;
            this.wingR.zRot = flap;
            this.wingL.zRot = -flap;
            this.foreR.zRot = Mth.cos(ageInTicks * 0.45F - 0.8F) * 0.4F;
            this.foreL.zRot = -Mth.cos(ageInTicks * 0.45F - 0.8F) * 0.4F;
            this.body.y = 14.0F + Mth.sin(ageInTicks * 0.45F) * 0.9F;
            this.body.xRot = 0.12F;
            this.neck.xRot = -0.6F;
            this.head.xRot = 0.55F + headPitch * ((float) Math.PI / 180.0F) * 0.4F;
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.5F;
        }
        for (int i = 0; i < 5; i++) {
            this.tail[i].xRot = -0.15F + Mth.sin(ageInTicks * 0.2F + i) * 0.06F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
