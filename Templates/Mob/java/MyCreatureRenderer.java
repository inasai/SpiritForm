package com.spiritform;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Відображення MyCreature: модель + текстура assets/spirit_form/textures/entity/my_creature.png. */
public class MyCreatureRenderer extends MobRenderer<MyCreature, MyCreatureModel<MyCreature>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "textures/entity/my_creature.png");

    public MyCreatureRenderer(EntityRendererProvider.Context context) {
        super(context, new MyCreatureModel<>(context.bakeLayer(MyCreatureModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(MyCreature entity) {
        return TEXTURE;
    }
}
