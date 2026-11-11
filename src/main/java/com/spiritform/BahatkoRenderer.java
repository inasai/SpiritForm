package com.spiritform;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Багатько світиться на повну яскравість. */
public class BahatkoRenderer extends MobRenderer<Bahatko, BahatkoModel<Bahatko>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "textures/entity/bahatko.png");

    public BahatkoRenderer(EntityRendererProvider.Context context) {
        super(context, new BahatkoModel<>(context.bakeLayer(BahatkoModel.LAYER)), 0.0F);
    }

    @Override
    protected void scale(Bahatko entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    protected int getBlockLightLevel(Bahatko entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(Bahatko entity) {
        return TEXTURE;
    }
}
