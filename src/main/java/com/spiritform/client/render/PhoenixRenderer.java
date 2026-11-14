package com.spiritform.client.render;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.model.PhoenixModel;
import com.spiritform.entity.Phoenix;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Жар-птиця: майже втричі більша за модель і світиться, як сонце. */
public class PhoenixRenderer extends MobRenderer<Phoenix, PhoenixModel<Phoenix>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "textures/entity/phoenix.png");

    public PhoenixRenderer(EntityRendererProvider.Context context) {
        super(context, new PhoenixModel<>(context.bakeLayer(PhoenixModel.LAYER)), 1.8F);
    }

    @Override
    protected void scale(Phoenix entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2.8F, 2.8F, 2.8F);
    }

    @Override
    protected int getBlockLightLevel(Phoenix entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(Phoenix entity) {
        return TEXTURE;
    }
}
