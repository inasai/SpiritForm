package com.spiritform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Моделі Дзеркала Душ (пояс із дзеркальцем, малюється на тулубі) та Кайданів Межі (кільця на щиколотці, малюється
 * на кожній нозі). Координати задані відносно частини тіла гравця (body чи leg), початок на її верхній точці,
 * y униз. Текстури: assets/spirit_form/textures/entity/soul_mirror.png та shackles_of_the_edge.png (32x32).
 */
public class AccessoryModels extends Model {

    public static final ModelLayerLocation MIRROR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "soul_mirror"), "main");
    public static final ModelLayerLocation SHACKLES = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "shackles_of_the_edge"), "main");

    public static final ModelLayerLocation BRACELET = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "fentons_bracelet"), "main");

    private final ModelPart root;

    public AccessoryModels(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition mirrorLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();
        part.addOrReplaceChild("main",
                CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, 10F, -2.5F, 9F, 2F, 5F)
                .texOffs(0, 7).addBox(0.5F, 10F, -3.4F, 5F, 5F, 1F)
                .texOffs(12, 7).addBox(1.5F, 11F, -3.9F, 3F, 3F, 1F)
                .texOffs(20, 7).addBox(-1F, 10F, -3F, 2F, 2F, 1F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition shacklesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();
        part.addOrReplaceChild("main",
                CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, 9F, -2.5F, 5F, 2F, 5F)
                .texOffs(0, 7).addBox(-2.5F, 8F, -2.5F, 5F, 1F, 5F)
                .texOffs(20, 7).addBox(2.5F, 9.5F, -0.5F, 1F, 1F, 1F)
                .texOffs(24, 7).addBox(-3.5F, 9.5F, -0.5F, 1F, 1F, 1F)
                .texOffs(28, 7).addBox(-0.5F, 9.5F, -2.75F, 1F, 1F, 1F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Стигмат Фантома: манжета на зап'ясті та спереду хрест (біло-чорний із зеленим ядром). Початок на плечі руки. */
    public static LayerDefinition braceletLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();
        part.addOrReplaceChild("main",
                CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, 6.5F, -2.5F, 5F, 2F, 5F)
                .texOffs(0, 10).addBox(-2.5F, 5F, -2.6F, 5F, 5F, 1F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
