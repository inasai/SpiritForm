package com.spiritform.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * Спільне оформлення вікон у ванільному стилі: панель із фаскою, як у вікні інвентаря, заглиблені поля, піднесені мітки,
 * гравійована лінія й смуга, як у босів. Темна тема: панель майже чорна з холодним відтінком, текст світлий.
 */
public final class SpiritUi {

    public static final int FILL = 0xFF2A2A30;
    public static final int WHITE = 0xFF4A4A54;
    public static final int SHADOW = 0xFF15151A;
    public static final int BLACK = 0xFF000000;
    public static final int INSET = 0xFF1B1B20;
    public static final int INSET_DARK = 0xFF08080B;

    public static final int TEXT = 0xFFE4E4EA;
    public static final int TEXT_DIM = 0xFF9C9CAA;
    public static final int TEXT_OK = 0xFF6FE09A;
    public static final int TEXT_BAD = 0xFFFF7A7A;
    public static final int ACCENT = 0xFF5FE0B4;
    /** Світлий колір для підписів на темному тлі (HUD, колесо). */
    public static final int LIGHT = 0xFFE8E8E8;
    /** Старе ім'я акцентного кольору; тепер це темно-м'ятний колір, що читається на сірій панелі. */
    public static final int GOLD = ACCENT;

    private static final ResourceLocation BOSS_BG_PURPLE = ResourceLocation.withDefaultNamespace("boss_bar/purple_background");
    private static final ResourceLocation BOSS_FG_PURPLE = ResourceLocation.withDefaultNamespace("boss_bar/purple_progress");
    private static final ResourceLocation BOSS_BG_RED = ResourceLocation.withDefaultNamespace("boss_bar/red_background");
    private static final ResourceLocation BOSS_FG_RED = ResourceLocation.withDefaultNamespace("boss_bar/red_progress");
    private static final ResourceLocation NOTCH_BG = ResourceLocation.withDefaultNamespace("boss_bar/notched_10_background");
    private static final ResourceLocation NOTCH_FG = ResourceLocation.withDefaultNamespace("boss_bar/notched_10_progress");

    private SpiritUi() {
    }

    /** Панель, як вікно інвентаря: чорна рамка зі зрізаними кутами, біла фаска зверху й зліва, сіра знизу й справа. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + 1, BLACK);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, BLACK);
        g.fill(x, y + 1, x + 1, y + h - 1, BLACK);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, BLACK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, FILL);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, WHITE);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, WHITE);
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 1, x + w - 1, y + 3, FILL);
        g.fill(x + 1, y + h - 3, x + 3, y + h - 1, FILL);
    }

    /** Заглиблене поле, як слот: темно зверху й зліва, біло знизу й справа. */
    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, WHITE);
        g.fill(x, y, x + w - 1, y + h - 1, INSET_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, INSET);
    }

    /** Піднесена пластина (мітка). */
    public static void raised(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, SHADOW);
        g.fill(x, y, x + w - 1, y + h - 1, WHITE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, FILL);
    }

    /** Гравійована лінія: темна й під нею біла. */
    public static void engraved(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, SHADOW);
        g.fill(x, y + 1, x + w, y + 2, WHITE);
    }

    /** Гравійована вертикальна лінія. */
    public static void engravedV(GuiGraphics g, int x, int y, int h) {
        g.fill(x, y, x + 1, y + h, SHADOW);
        g.fill(x + 1, y, x + 2, y + h, WHITE);
    }

    public static void diamond(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 2, y, x + 3, y + 1, color);
        g.fill(x + 1, y + 1, x + 4, y + 2, color);
        g.fill(x, y + 2, x + 5, y + 3, color);
        g.fill(x + 1, y + 3, x + 4, y + 4, color);
        g.fill(x + 2, y + 4, x + 3, y + 5, color);
    }

    // ---------- текст без тіні ----------

    public static void text(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public static void text(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean ignoredShadow) {
        g.drawString(font, text, x, y, color, false);
    }

    public static void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public static void text(GuiGraphics g, Font font, String text, int x, int y, int color, boolean ignoredShadow) {
        g.drawString(font, text, x, y, color, false);
    }

    public static void text(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    public static void centered(GuiGraphics g, Font font, Component text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, false);
    }

    public static void centered(GuiGraphics g, Font font, String text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, false);
    }

    /** Заголовок розділу: акцентний ромб, текст і гравійована лінія під ним. Займає 14 пікселів по висоті. */
    public static void section(GuiGraphics g, Font font, int x, int y, int w, Component text) {
        diamond(g, x, y + 2, ACCENT);
        g.drawString(font, text, x + 9, y, ACCENT, false);
        engraved(g, x, y + 11, w);
    }

    /** Мітка у виступній рамці. Повертає її ширину. */
    public static int chip(GuiGraphics g, Font font, int x, int y, Component text) {
        int w = font.width(text) + 8;
        raised(g, x, y, w, 12);
        g.drawString(font, text, x + 4, y + 2, ACCENT, false);
        return w;
    }

    /** Абзац із переносом. Повертає y під останнім рядком. */
    public static int paragraph(GuiGraphics g, Font font, Component text, int x, int y, int w, int color) {
        for (FormattedCharSequence line : lines(font, text, w)) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private record ParagraphKey(Component text, int width) {
    }

    private static final java.util.Map<ParagraphKey, java.util.List<FormattedCharSequence>> PARAGRAPHS =
            new java.util.HashMap<>();
    private static final int PARAGRAPH_LIMIT = 256;

    /** Перенос рядків щокадру дорогий, а текст і ширина майже не змінюються: результат запам'ятовується. */
    private static java.util.List<FormattedCharSequence> lines(Font font, Component text, int width) {
        ParagraphKey key = new ParagraphKey(text, width);
        java.util.List<FormattedCharSequence> cached = PARAGRAPHS.get(key);
        if (cached == null) {
            if (PARAGRAPHS.size() >= PARAGRAPH_LIMIT) {
                PARAGRAPHS.clear();
            }
            cached = font.split(text, width);
            PARAGRAPHS.put(key, cached);
        }
        return cached;
    }

    /** Скидає запам'ятовані абзаци (при відкритті вікна, після зміни мови чи шрифту). */
    public static void clearCache() {
        PARAGRAPHS.clear();
    }

    /** Смуга, як у боса: фіолетова, а коли досягнуто порогу, червона. Шкала 182x5, виводиться з масштабом. */
    public static void bossBar(GuiGraphics g, int x, int y, float progress, boolean ready, float scale) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.blitSprite(ready ? BOSS_BG_RED : BOSS_BG_PURPLE, 182, 5, 0, 0, 0, 0, 182, 5);
        int filled = (int) (182 * p);
        if (filled > 0) {
            g.blitSprite(ready ? BOSS_FG_RED : BOSS_FG_PURPLE, 182, 5, 0, 0, 0, 0, filled, 5);
        }
        g.blitSprite(NOTCH_BG, 182, 5, 0, 0, 0, 0, 182, 5);
        if (filled > 0) {
            g.blitSprite(NOTCH_FG, 182, 5, 0, 0, 0, 0, filled, 5);
        }
        g.pose().popPose();
    }
}
