package com.spiritform.client.gui;

import com.spiritform.SpiritFormMod;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Спільний вигляд кругових меню (колесо пасивних здібностей і колеса Армії Потойбіччя):
 * кільце з рівними секторами, розділеними темними проміжками, м'ятна дуга над сектором під курсором,
 * кругла кнопка в центрі та панель підказки внизу.
 * Кільце малюється однією кешованою текстурою на кожен стан, тож без зайвих обчислень і викликів на кожному кадрі.
 */
public final class SpiritWheelUi {

    public static final int ACCENT = 0xFF8CFFC8;
    public static final int TEXT = 0xFFE8FFF4;
    public static final int TEXT_DIM = 0xFF8FB5A5;
    private static final int LABEL = 0xFFBFE8D6;
    private static final int DANGER = 0xFFFF8A8A;

    private static final int BODY = 0xD00E1613;
    private static final int BODY_HOT = 0xF01F3D33;
    private static final int BODY_ON = 0xE0183A2F;
    private static final int BODY_ON_HOT = 0xF02F6A55;
    private static final int BODY_DANGER = 0xD02A1616;
    private static final int BODY_DANGER_HOT = 0xE04A2222;
    private static final int BODY_DANGER_PENDING = 0xF0702424;
    private static final int GAP = 0xFF050807;
    private static final int RIM = 0xFF2E5A4A;
    private static final int RIM_DANGER = 0xFFB04A4A;
    private static final int CENTER_RING = 0xFF3F7A64;
    private static final int CENTER = 0xF00A100E;
    private static final int CENTER_HOT = 0xF02B5A49;

    private static final int T_NONE = 0;
    private static final int T_BODY = 1;
    private static final int T_GAP = 2;
    private static final int T_RIM = 3;
    private static final int T_ARC = 4;
    private static final int T_BORDER = 5;
    private static final int T_CRING = 6;
    private static final int T_CENTER = 7;

    private static final Map<Long, int[]> CACHE = new HashMap<>();

    private SpiritWheelUi() {
    }

    /** Розміри кільця: зовнішній радіус залежить від висоти екрана, щоб колесо й підказка вміщалися. */
    public record Geo(int outer, int inner) {
        public static Geo of(int screenHeight) {
            int outer = Math.max(64, Math.min(100, (screenHeight - 96) / 2));
            return new Geo(outer, Math.round(outer * 0.46F));
        }

        int iconRadius() {
            return (outer + inner) / 2 + 1;
        }
    }

    /** Один сектор: назва, короткий рядок стану, іконка (файл здібності або предмет) і прапорці вигляду. */
    public record Slice(Component name, Component info, String icon, ItemStack stack,
                        boolean on, boolean danger, boolean pending) {
        public static Slice of(Component name, Component info, String icon, boolean on) {
            return new Slice(name, info, icon, ItemStack.EMPTY, on, false, false);
        }
    }

    /** Кнопка в центрі: іконка (може бути null), назва (може бути null) та рядок стану. Без назви рядок стану читається як число під іконкою. */
    public record Center(String icon, Component text, Component info) {
    }

    /** Підказка внизу: заголовок із коротким станом, необов'язковий опис і рядок керування. */
    public record Tip(Component title, Component info, Component detail, Component hint) {
    }

    // ---------- наведення ----------

    private static double angleOf(double dx, double dy) {
        return Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
    }

    private static int sectorOf(int count, double dx, double dy) {
        double size = 360.0 / count;
        double shifted = ((angleOf(dx, dy) + size / 2.0) % 360.0 + 360.0) % 360.0;
        return Math.min(count - 1, (int) (shifted / size));
    }

    /** -2 центральна кнопка, -1 нічого, інакше номер сектора (перший зверху, далі за годинниковою стрілкою). */
    public static int hit(Geo geo, int count, double dx, double dy) {
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < geo.inner()) {
            return -2;
        }
        if (count <= 0 || dist > geo.outer() + 6) {
            return -1;
        }
        return sectorOf(count, dx, dy);
    }

    // ---------- кільце ----------

    private static int[] build(Geo geo, int count) {
        int r = geo.outer() + 3;
        int d = r * 2;
        int[] cells = new int[d * d];
        double size = 360.0 / count;
        for (int j = 0; j < d; j++) {
            for (int i = 0; i < d; i++) {
                double dx = i - r + 0.5;
                double dy = j - r + 0.5;
                double dist = Math.sqrt(dx * dx + dy * dy);
                int type;
                int sector = 0;
                if (dist < geo.inner() - 6) {
                    type = T_CENTER;
                } else if (dist < geo.inner() - 5) {
                    type = T_CRING;
                } else if (dist < geo.inner()) {
                    type = T_BORDER;
                } else if (dist < geo.outer() + 3) {
                    double shifted = ((angleOf(dx, dy) + size / 2.0) % 360.0 + 360.0) % 360.0;
                    sector = Math.min(count - 1, (int) (shifted / size));
                    boolean gap = false;
                    if (count > 1) {
                        double off = shifted - sector * size;
                        double toEdge = Math.min(off, size - off);
                        gap = dist * Math.sin(Math.toRadians(toEdge)) < 1.5;
                    }
                    if (dist >= geo.outer()) {
                        type = gap ? T_NONE : T_ARC;
                    } else if (gap) {
                        type = T_GAP;
                    } else if (dist >= geo.outer() - 1) {
                        type = T_RIM;
                    } else {
                        type = T_BODY;
                    }
                } else {
                    type = T_NONE;
                }
                cells[j * d + i] = type | (sector << 4);
            }
        }
        return cells;
    }

    private static int colorOf(int cell, int count, int hover, boolean centerHover, List<Slice> slices) {
        int type = cell & 15;
        int sector = cell >> 4;
        boolean hot = count > 0 && sector == hover;
        Slice slice = count > 0 && sector < slices.size() ? slices.get(sector) : null;
        boolean danger = slice != null && slice.danger();
        boolean on = slice != null && slice.on();
        return switch (type) {
            case T_BODY -> {
                if (danger) {
                    yield slice.pending() ? BODY_DANGER_PENDING : hot ? BODY_DANGER_HOT : BODY_DANGER;
                }
                yield hot ? (on ? BODY_ON_HOT : BODY_HOT) : (on ? BODY_ON : BODY);
            }
            case T_GAP, T_BORDER -> GAP;
            case T_RIM -> hot ? (danger ? RIM_DANGER : ACCENT) : (danger ? 0xFF5A2A2A : RIM);
            case T_ARC -> hot ? (danger ? DANGER : ACCENT) : 0;
            case T_CRING -> centerHover ? ACCENT : CENTER_RING;
            case T_CENTER -> centerHover ? CENTER_HOT : CENTER;
            default -> 0;
        };
    }

    // Готове кільце кешується як одна текстура на кожен стан (сектор під курсором, увімкнені тощо),
    // тож щокадру це один виклик малювання замість тисяч прямокутників.
    private static final Map<String, ResourceLocation> TEXTURES = new LinkedHashMap<>();
    private static final int TEXTURE_LIMIT = 40;
    private static int textureCounter;

    private static int toAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF);
    }

    private static ResourceLocation texture(Geo geo, int count, int hover, boolean centerHover, List<Slice> slices) {
        StringBuilder key = new StringBuilder();
        key.append(geo.outer()).append(':').append(geo.inner()).append(':').append(count).append(':')
                .append(hover).append(':').append(centerHover ? 1 : 0).append(':');
        for (Slice slice : slices) {
            key.append((slice.on() ? 1 : 0) | (slice.danger() ? 2 : 0) | (slice.pending() ? 4 : 0));
        }
        String id = key.toString();
        ResourceLocation found = TEXTURES.get(id);
        if (found != null) {
            return found;
        }

        int c = Math.max(1, count);
        long cellKey = ((long) geo.outer() << 40) | ((long) geo.inner() << 20) | c;
        int[] cells = CACHE.computeIfAbsent(cellKey, k -> build(geo, c));
        int d = (geo.outer() + 3) * 2;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, d, d, true);
        for (int j = 0; j < d; j++) {
            for (int i = 0; i < d; i++) {
                image.setPixelRGBA(i, j, toAbgr(colorOf(cells[j * d + i], count, hover, centerHover, slices)));
            }
        }

        TextureManager manager = Minecraft.getInstance().getTextureManager();
        if (TEXTURES.size() >= TEXTURE_LIMIT) {
            Iterator<Map.Entry<String, ResourceLocation>> eldest = TEXTURES.entrySet().iterator();
            manager.release(eldest.next().getValue());
            eldest.remove();
        }
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "wheel/ring_" + textureCounter++);
        manager.register(location, new DynamicTexture(image));
        TEXTURES.put(id, location);
        return location;
    }

    /** Звільняє кешовані текстури кільця (викликається, коли колесо закривається). */
    public static void release() {
        if (TEXTURES.isEmpty()) {
            return;
        }
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation location : TEXTURES.values()) {
            manager.release(location);
        }
        TEXTURES.clear();
    }

    private static void ring(GuiGraphics g, Geo geo, int cx, int cy, int count, int hover,
                             boolean centerHover, List<Slice> slices) {
        ResourceLocation location = texture(geo, count, hover, centerHover, slices);
        int r = geo.outer() + 3;
        int d = r * 2;
        RenderSystem.enableBlend();
        g.blit(location, cx - r, cy - r, d, d, 0.0F, 0.0F, d, d, d, d);
    }

    // ---------- вміст ----------

    private static void drawSlices(GuiGraphics g, Font font, int cx, int cy, Geo geo, List<Slice> slices, int hover) {
        int count = slices.size();
        if (count == 0) {
            return;
        }
        double size = 360.0 / count;
        int radius = geo.iconRadius();
        boolean labels = count <= 6;
        int maxWidth = Math.max(30, Math.min(84, (int) (2.0 * Math.PI * radius / count) - 8));
        for (int i = 0; i < count; i++) {
            Slice slice = slices.get(i);
            double angle = Math.toRadians(-90.0 + i * size);
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            int iconY = labels ? y - 14 : y - 8;
            if (!slice.stack().isEmpty()) {
                g.renderItem(slice.stack(), x - 8, iconY);
            } else if (slice.icon() != null) {
                SpiritIcons.draw(g, slice.icon(), x - 8, iconY);
            }
            if (labels) {
                String text = slice.name().getString();
                if (font.width(text) > maxWidth) {
                    text = font.plainSubstrByWidth(text, maxWidth - 6) + "…";
                }
                int color = i == hover ? TEXT : slice.danger() ? DANGER : LABEL;
                SpiritUi.centered(g, font, text, x, y + 5, color);
            }
        }
    }

    private static void drawCenter(GuiGraphics g, Font font, int cx, int cy, Geo geo, Center center, boolean hot) {
        int width = (int) (geo.inner() * 1.5F);
        List<FormattedCharSequence> lines = center.text() == null ? List.of() : font.split(center.text(), width);
        int shown = Math.min(2, lines.size());
        boolean icon = center.icon() != null;
        int total = (icon ? 18 : 0) + shown * 9 + (center.info() != null ? 12 : 0);
        int y = cy - total / 2;
        if (icon) {
            SpiritIcons.draw(g, center.icon(), cx - 8, y);
            y += 18;
        }
        int color = hot ? 0xFFFFFFFF : TEXT;
        for (int i = 0; i < shown; i++) {
            FormattedCharSequence line = lines.get(i);
            g.drawString(font, line, cx - font.width(line) / 2, y, color, false);
            y += 9;
        }
        if (center.info() != null) {
            int infoColor = center.text() == null ? (hot ? 0xFFFFFFFF : TEXT) : TEXT_DIM;
            SpiritUi.centered(g, font, center.info(), cx, y + 3, infoColor);
        }
    }

    private static void drawTip(GuiGraphics g, Font font, int cx, int top, int screenWidth, int screenHeight, Tip tip) {
        Component head = tip.info() == null ? tip.title()
                : Component.empty().append(tip.title()).append(Component.literal("   ")).append(tip.info());
        List<FormattedCharSequence> detail = tip.detail() == null ? List.of() : font.split(tip.detail(), 300);

        int w = font.width(head);
        for (FormattedCharSequence line : detail) {
            w = Math.max(w, font.width(line));
        }
        if (tip.hint() != null) {
            w = Math.max(w, font.width(tip.hint()));
        }
        w += 18;
        int h = 8 + 11 + detail.size() * 10 + (tip.hint() != null ? 11 : 0);
        int x = Math.max(2, Math.min(screenWidth - w - 2, cx - w / 2));
        int y = Math.max(2, Math.min(top, screenHeight - h - 2));

        g.fill(x, y, x + w, y + h, 0xE0070B09);
        g.fill(x, y, x + w, y + 2, CENTER_RING);
        g.fill(x, y, x + 1, y + h, 0xFF1B3A30);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF1B3A30);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF1B3A30);

        int ty = y + 6;
        g.drawString(font, head, x + 9, ty, ACCENT, false);
        ty += 11;
        for (FormattedCharSequence line : detail) {
            g.drawString(font, line, x + 9, ty, TEXT, false);
            ty += 10;
        }
        if (tip.hint() != null) {
            SpiritUi.text(g, font, tip.hint(), x + 9, ty, TEXT_DIM);
        }
    }

    /**
     * Малює все колесо з центром у точці (cx, cy).
     *
     * @param hover      номер сектора під курсором, -1 якщо немає
     * @param centerHot  курсор над центральною кнопкою
     */
    public static void draw(GuiGraphics g, Font font, int screenWidth, int screenHeight, Geo geo,
                            List<Slice> slices, int hover, Center center, boolean centerHot, Tip tip) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        ring(g, geo, cx, cy, slices.size(), hover, centerHot, slices);
        drawSlices(g, font, cx, cy, geo, slices, hover);
        drawCenter(g, font, cx, cy, geo, center, centerHot);
        drawTip(g, font, cx, cy + geo.outer() + 10, screenWidth, screenHeight, tip);
    }
}
