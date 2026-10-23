package com.spiritform;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Іконки здібностей в меню (лише клієнт). Кожна здібність малюється з власного файлу
 * assets/spirit_form/textures/gui/ability/<id>.png (id: sense, vanish, step, possession, touch, delirium,
 * icy_touch, presence, immunity, life_gift, scales, gaze та instinct). Розмір будь-який, але квадратний:
 * картинка стискається до 16x16 у меню.
 */
public final class SpiritIcons {

    private record Size(int width, int height) {
    }

    private static final Map<String, ResourceLocation> LOCATIONS = new HashMap<>();
    private static final Map<ResourceLocation, Size> SIZES = new HashMap<>();

    private SpiritIcons() {
    }

    public static ResourceLocation location(String id) {
        return LOCATIONS.computeIfAbsent(id,
                key -> ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "textures/gui/ability/" + key + ".png"));
    }

    private static Size size(ResourceLocation location) {
        Size cached = SIZES.get(location);
        if (cached != null) {
            return cached;
        }
        Size size = new Size(16, 16);
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(location);
             NativeImage image = NativeImage.read(in)) {
            size = new Size(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()));
        } catch (Exception ignored) {
            // немає файлу чи він пошкоджений: малюємо як 16x16
        }
        SIZES.put(location, size);
        return size;
    }

    /** Після F3+T (перезавантаження ресурсів) розміри читаються заново. */
    public static void clearCache() {
        SIZES.clear();
    }

    /** Малює іконку здібності 16x16 у точці (x, y). */
    public static void draw(GuiGraphics graphics, String id, int x, int y) {
        ResourceLocation location = location(id);
        Size size = size(location);
        RenderSystem.enableBlend();
        graphics.blit(location, x, y, 16, 16, 0.0F, 0.0F, size.width(), size.height(), size.width(), size.height());
    }
}
