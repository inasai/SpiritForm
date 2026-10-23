package com.spiritform;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/**
 * Інвертований скін для гравця з Браслетом Фентона (лише клієнт).
 * Піксели скіна зчитуються з відеопам'яті, кольори RGB інвертуються (прозорість лишається),
 * результат реєструється як окрема текстура й кешується для кожного скіна.
 */
public final class FentonSkin {

    private static final Map<ResourceLocation, ResourceLocation> CACHE = new HashMap<>();

    private FentonSkin() {
    }

    public static ResourceLocation invert(ResourceLocation skin) {
        ResourceLocation cached = CACHE.get(skin);
        if (cached != null) {
            return cached;
        }

        try {
            TextureManager manager = Minecraft.getInstance().getTextureManager();
            AbstractTexture texture = manager.getTexture(skin);

            NativeImage image = new NativeImage(64, 64, false);
            RenderSystem.bindTexture(texture.getId());
            image.downloadTexture(0, false);

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    image.setPixelRGBA(x, y, (pixel & 0xFF000000) | (~pixel & 0x00FFFFFF));
                }
            }

            String path = skin.getNamespace() + "/" + skin.getPath().replaceAll("[^a-z0-9/._-]", "_");
            ResourceLocation inverted = ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "fenton/" + path);
            manager.register(inverted, new DynamicTexture(image));
            CACHE.put(skin, inverted);
            return inverted;
        } catch (Throwable e) {
            CACHE.put(skin, skin);
            return skin;
        }
    }
}
