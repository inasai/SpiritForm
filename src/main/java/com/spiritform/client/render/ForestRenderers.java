package com.spiritform.client.render;

import com.spiritform.SpiritFormMod;
import com.spiritform.client.model.BisykModel;
import com.spiritform.client.model.ChortModel;
import com.spiritform.client.model.ChuhaisterModel;
import com.spiritform.client.model.LisovykModel;
import com.spiritform.client.model.MavkaModel;
import com.spiritform.client.model.PerelisnykModel;
import com.spiritform.client.model.PolovykModel;
import com.spiritform.client.model.PotercharModel;
import com.spiritform.client.model.PovitruliaModel;
import com.spiritform.client.model.SkarbnykModel;
import com.spiritform.client.model.VodianykModel;
import com.spiritform.client.model.VodianytsiaModel;
import com.spiritform.client.model.VovkulakaModel;
import com.spiritform.entity.Bisyk;
import com.spiritform.entity.Chort;
import com.spiritform.entity.Chuhaister;
import com.spiritform.entity.Hukha;
import com.spiritform.entity.Lisovyk;
import com.spiritform.entity.Mavka;
import com.spiritform.entity.Nichnytsia;
import com.spiritform.entity.Perelisnyk;
import com.spiritform.entity.Polovyk;
import com.spiritform.entity.Potercha;
import com.spiritform.entity.Povitrulia;
import com.spiritform.entity.Skarbnyk;
import com.spiritform.entity.Vodianyk;
import com.spiritform.entity.Vodianytsia;
import com.spiritform.entity.Vovkulaka;
import com.spiritform.entity.Zlydeny;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Відображення істот лісу Потойбіччя: Мавка, Чугайстер, Хуха, Бісик. */
public final class ForestRenderers {

    private ForestRenderers() {
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "textures/entity/" + name + ".png");
    }

    public static class MavkaRenderer extends MobRenderer<Mavka, MavkaModel<Mavka>> {
        private static final ResourceLocation TEXTURE = tex("mavka");

        public MavkaRenderer(EntityRendererProvider.Context context) {
            super(context, new MavkaModel<>(context.bakeLayer(MavkaModel.LAYER)), 0.45F);
            this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        public ResourceLocation getTextureLocation(Mavka entity) {
            return TEXTURE;
        }
    }

    public static class ChuhaisterRenderer extends MobRenderer<Chuhaister, ChuhaisterModel<Chuhaister>> {
        private static final ResourceLocation TEXTURE = tex("chuhaister");

        public ChuhaisterRenderer(EntityRendererProvider.Context context) {
            super(context, new ChuhaisterModel<>(context.bakeLayer(ChuhaisterModel.LAYER)), 1.6F);
        }

        /** Модель має висоту близько 37 пікселів: 2,3 разів більше дає хітбокс 5,4 блока. */
        @Override
        protected void scale(Chuhaister entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(2.3F, 2.3F, 2.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(Chuhaister entity) {
            return TEXTURE;
        }
    }

    /** Модель ванільного кота, збільшена до розміру вівці. */
    public static class HukhaRenderer extends MobRenderer<Hukha, CatModel<Hukha>> {
        private static final ResourceLocation TEXTURE = tex("hukha");

        public HukhaRenderer(EntityRendererProvider.Context context) {
            super(context, new CatModel<>(context.bakeLayer(ModelLayers.CAT)), 0.8F);
        }

        @Override
        protected void scale(Hukha entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(1.7F, 1.7F, 1.7F);
        }

        @Override
        public ResourceLocation getTextureLocation(Hukha entity) {
            return TEXTURE;
        }
    }

    public static class BisykRenderer extends MobRenderer<Bisyk, BisykModel<Bisyk>> {
        private static final ResourceLocation TEXTURE = tex("bisyk");

        public BisykRenderer(EntityRendererProvider.Context context) {
            super(context, new BisykModel<>(context.bakeLayer(BisykModel.LAYER)), 0.15F);
        }

        @Override
        protected void scale(Bisyk entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(1.2F, 1.2F, 1.2F);
        }

        /** Світлячок світиться: завжди повна яскравість. */
        @Override
        protected int getBlockLightLevel(Bisyk entity, BlockPos pos) {
            return 15;
        }

        @Override
        public ResourceLocation getTextureLocation(Bisyk entity) {
            return TEXTURE;
        }
    }

    public static class SkarbnykRenderer extends MobRenderer<Skarbnyk, SkarbnykModel<Skarbnyk>> {
        private static final ResourceLocation TEXTURE = tex("skarbnyk");

        public SkarbnykRenderer(EntityRendererProvider.Context context) {
            super(context, new SkarbnykModel<>(context.bakeLayer(SkarbnykModel.LAYER)), 0.35F);
            this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        protected void scale(Skarbnyk entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(0.85F, 0.85F, 0.85F);
        }

        @Override
        public ResourceLocation getTextureLocation(Skarbnyk entity) {
            return TEXTURE;
        }
    }

    /** Злидень: та сама модель і текстура, що й у Хухи (ванільна модель кота = модель оцелота). */
    public static class ZlydenyRenderer extends MobRenderer<Zlydeny, OcelotModel<Zlydeny>> {
        private static final ResourceLocation TEXTURE = tex("hukha");

        public ZlydenyRenderer(EntityRendererProvider.Context context) {
            super(context, new OcelotModel<>(context.bakeLayer(ModelLayers.OCELOT)), 0.8F);
        }

        @Override
        protected void scale(Zlydeny entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(1.7F, 1.7F, 1.7F);
        }

        @Override
        public ResourceLocation getTextureLocation(Zlydeny entity) {
            return TEXTURE;
        }
    }

    public static class VodianykRenderer extends MobRenderer<Vodianyk, VodianykModel<Vodianyk>> {
        private static final ResourceLocation TEXTURE = tex("vodianyk");

        public VodianykRenderer(EntityRendererProvider.Context context) {
            super(context, new VodianykModel<>(context.bakeLayer(VodianykModel.LAYER)), 0.6F);
        }

        @Override
        protected void scale(Vodianyk entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(1.1F, 1.1F, 1.1F);
        }

        @Override
        public ResourceLocation getTextureLocation(Vodianyk entity) {
            return TEXTURE;
        }
    }

    public static class VodianytsiaRenderer extends MobRenderer<Vodianytsia, VodianytsiaModel<Vodianytsia>> {
        private static final ResourceLocation TEXTURE = tex("vodianytsia");

        public VodianytsiaRenderer(EntityRendererProvider.Context context) {
            super(context, new VodianytsiaModel<>(context.bakeLayer(VodianytsiaModel.LAYER)), 0.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(Vodianytsia entity) {
            return TEXTURE;
        }
    }

    public static class PovitruliaRenderer extends MobRenderer<Povitrulia, PovitruliaModel<Povitrulia>> {
        private static final ResourceLocation TEXTURE = tex("povitrulia");

        public PovitruliaRenderer(EntityRendererProvider.Context context) {
            super(context, new PovitruliaModel<>(context.bakeLayer(PovitruliaModel.LAYER)), 0.4F);
        }

        @Override
        protected void scale(Povitrulia entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(1.3F, 1.3F, 1.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(Povitrulia entity) {
            return TEXTURE;
        }
    }

    public static class PerelisnykRenderer extends MobRenderer<Perelisnyk, PerelisnykModel<Perelisnyk>> {
        private static final ResourceLocation TEXTURE = tex("perelisnyk");

        public PerelisnykRenderer(EntityRendererProvider.Context context) {
            super(context, new PerelisnykModel<>(context.bakeLayer(PerelisnykModel.LAYER)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(Perelisnyk entity) {
            return TEXTURE;
        }
    }

    /** Нічниця: модель Мавки в темних відтінках. */
    public static class NichnytsiaRenderer extends MobRenderer<Nichnytsia, MavkaModel<Nichnytsia>> {
        private static final ResourceLocation TEXTURE = tex("nichnytsia");

        public NichnytsiaRenderer(EntityRendererProvider.Context context) {
            super(context, new MavkaModel<>(context.bakeLayer(MavkaModel.LAYER)), 0.45F);
            this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        public ResourceLocation getTextureLocation(Nichnytsia entity) {
            return TEXTURE;
        }
    }

    public static class PotercharRenderer extends MobRenderer<Potercha, PotercharModel<Potercha>> {
        private static final ResourceLocation TEXTURE = tex("potercha");

        public PotercharRenderer(EntityRendererProvider.Context context) {
            super(context, new PotercharModel<>(context.bakeLayer(PotercharModel.LAYER)), 0.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(Potercha entity) {
            return TEXTURE;
        }
    }

    public static class PolovykRenderer extends MobRenderer<Polovyk, PolovykModel<Polovyk>> {
        private static final ResourceLocation TEXTURE = tex("polovyk");

        public PolovykRenderer(EntityRendererProvider.Context context) {
            super(context, new PolovykModel<>(context.bakeLayer(PolovykModel.LAYER)), 0.4F);
        }

        @Override
        public ResourceLocation getTextureLocation(Polovyk entity) {
            return TEXTURE;
        }
    }

    /** Вовкулака: модель вовка, збільшена до розміру коня. */
    public static class VovkulakaRenderer extends MobRenderer<Vovkulaka, VovkulakaModel<Vovkulaka>> {
        private static final ResourceLocation TEXTURE = tex("vovkulaka");

        public VovkulakaRenderer(EntityRendererProvider.Context context) {
            super(context, new VovkulakaModel<>(context.bakeLayer(VovkulakaModel.LAYER)), 0.9F);
        }

        @Override
        protected void scale(Vovkulaka entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(1.8F, 1.8F, 1.8F);
        }

        @Override
        public ResourceLocation getTextureLocation(Vovkulaka entity) {
            return TEXTURE;
        }
    }

    /** Чорт: біла форма й чорна форма (після удару по ній) мають різні текстури. */
    public static class ChortRenderer extends MobRenderer<Chort, ChortModel<Chort>> {
        private static final ResourceLocation WHITE = tex("chort");
        private static final ResourceLocation DARK = tex("chort_dark");

        public ChortRenderer(EntityRendererProvider.Context context) {
            super(context, new ChortModel<>(context.bakeLayer(ChortModel.LAYER)), 0.4F);
        }

        @Override
        protected void scale(Chort entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(0.9F, 0.9F, 0.9F);
        }

        @Override
        public ResourceLocation getTextureLocation(Chort entity) {
            return entity.isDark() ? DARK : WHITE;
        }
    }

    public static class LisovykRenderer extends MobRenderer<Lisovyk, LisovykModel<Lisovyk>> {
        private static final ResourceLocation TEXTURE = tex("lisovyk");

        public LisovykRenderer(EntityRendererProvider.Context context) {
            super(context, new LisovykModel<>(context.bakeLayer(LisovykModel.LAYER)), 1.2F);
        }

        /** Модель висотою близько 36 пікселів: 1,7 разу більше дає хітбокс 4 блоки. */
        @Override
        protected void scale(Lisovyk entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(1.7F, 1.7F, 1.7F);
        }

        @Override
        public ResourceLocation getTextureLocation(Lisovyk entity) {
            return TEXTURE;
        }
    }
}
