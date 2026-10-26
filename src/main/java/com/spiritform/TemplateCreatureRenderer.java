package com.spiritform;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * ШАБЛОН ВІДОБРАЖЕННЯ ІСТОТИ: поєднує модель і текстуру.
 * Текстура лежить у assets/spirit_form/textures/entity/template_creature.png.
 */
public class TemplateCreatureRenderer extends MobRenderer<TemplateCreature, TemplateCreatureModel<TemplateCreature>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "textures/entity/template_creature.png");

    public TemplateCreatureRenderer(EntityRendererProvider.Context context) {
        // Останнє число: розмір тіні під істотою
        super(context, new TemplateCreatureModel<>(context.bakeLayer(TemplateCreatureModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(TemplateCreature entity) {
        return TEXTURE;
    }
}
