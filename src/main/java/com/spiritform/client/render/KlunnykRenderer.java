package com.spiritform.client.render;

import com.spiritform.SpiritFormMod;
import com.spiritform.entity.Klunnyk;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Клунник: людиноподібна модель (як у зомбі) з власною текстурою; вбрання світиться. */
public class KlunnykRenderer extends HumanoidMobRenderer<Klunnyk, HumanoidModel<Klunnyk>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "textures/entity/klunnyk.png");

    public KlunnykRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.4F);
    }

    @Override
    protected void scale(Klunnyk entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.85F, 0.85F, 0.85F);
    }

    @Override
    protected int getBlockLightLevel(Klunnyk entity, BlockPos pos) {
        return 12;
    }

    @Override
    public ResourceLocation getTextureLocation(Klunnyk entity) {
        return TEXTURE;
    }
}
