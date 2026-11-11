package com.spiritform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Модель Намиста Потойбіччя на персонажі: пласкій значок, як у інвентарі.
 * Координати задані відносно частини тіла "body" гравця: початок координат на шиї, y вниз, z уперед це мінус.
 */
public class NecklaceModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "necklace_of_the_beyond"), "main");

    private final ModelPart root;

    public NecklaceModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    /**
     * Намисто на персонажі: той самий пласкій значок 16x16, що й у інвентарі (текстура 64x32: спереду значок у (1,1),
     * ззаду копія). Початок координат на шиї, y униз, z уперед це мінус.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("Amulet",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, 0.0F, -5.0F, 16.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        float scale = 0.46F;
        poseStack.scale(scale, scale, scale);
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
