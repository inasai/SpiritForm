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
 * Модель Намиста Потойбіччя на персонажі (з Blockbench, Modded Entity, Mojang mappings).
 * Координати задані відносно частини тіла "body" гравця: початок координат на шиї, y вниз, z уперед це мінус.
 * Щоб замінити модель, встав свій createBodyLayer з нового експорту Blockbench (див. коментар нижче).
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
     * Нову модель з Blockbench вставляй сюди: скопіюй рядки від "PartDefinition Necklace = ..." до останнього
     * addOrReplaceChild. Якщо в експорті імена частин складаються з цифр, перейменуй змінні (наприклад, part1),
     * самі рядкові імена "1", "2" можна лишати. Розмір текстури в LayerDefinition.create (тут 16, 16) має збігатися
     * з розміром у Blockbench.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition necklace = partdefinition.addOrReplaceChild("Necklace", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 5.0F, -2.0F, 0.0F, 0.0F, 0.7854F));

        necklace.addOrReplaceChild("1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        necklace.addOrReplaceChild("2", CubeListBuilder.create()
                .texOffs(7, 0).addBox(-1.0F, -2.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(7, 0).addBox(1.0F, -3.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(7, 0).addBox(2.0F, -4.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(6, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 1.0F, 0.0F));

        necklace.addOrReplaceChild("3", CubeListBuilder.create()
                .texOffs(3, 2).addBox(0.0F, -1.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(3, 2).addBox(1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(4, 2).addBox(2.0F, -2.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, -1.0F, 0.0F));

        necklace.addOrReplaceChild("4", CubeListBuilder.create()
                .texOffs(8, 3).addBox(-3.0F, -3.0F, -1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
