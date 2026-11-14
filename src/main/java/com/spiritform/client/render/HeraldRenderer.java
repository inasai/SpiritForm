package com.spiritform.client.render;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.model.HeraldModel;
import com.spiritform.herald.HeraldBase;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Відображення Вісників: модель HeraldModel і текстура textures/entity/<вид>_herald.png. */
public class HeraldRenderer extends MobRenderer<HeraldBase, HeraldModel> {

    private final ResourceLocation texture;
    private final float scale;

    public HeraldRenderer(EntityRendererProvider.Context context, HeraldModel.Variant variant, float scale, float shadow) {
        super(context, new HeraldModel(context.bakeLayer(HeraldModel.layer(variant)), variant), shadow);
        this.scale = scale;
        this.texture = ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID,
                "textures/entity/" + variant.name().toLowerCase(java.util.Locale.ROOT) + "_herald.png");
    }

    @Override
    public ResourceLocation getTextureLocation(HeraldBase entity) {
        return texture;
    }

    @Override
    protected void scale(HeraldBase entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }
}
