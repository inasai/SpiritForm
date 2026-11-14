package com.spiritform.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Анімація відкриття та закриття вікон: тло темніє, а панель виростає з 90% розміру й трохи зсувається вгору.
 * Закриття відтворює те саме у зворотному порядку, і лише потім виконується дія (повернення до попереднього вікна чи вихід).
 */
public final class SpiritAnim {

    public static final long MS = 150L;

    private long openedAt = Util.getMillis();
    private long closingAt = -1L;
    private Runnable after;

    /** Починає відкриття наново (викликається з init). */
    public void reopen() {
        openedAt = Util.getMillis();
        closingAt = -1L;
        after = null;
    }

    public boolean closing() {
        return closingAt >= 0L;
    }

    /** Вікно ще рухається, тож клацання й клавіші ігноруються. */
    public boolean busy() {
        return closingAt >= 0L || Util.getMillis() - openedAt < MS;
    }

    public void close(Runnable action) {
        if (closingAt < 0L) {
            closingAt = Util.getMillis();
            after = action;
        }
    }

    /** Викликається щотіку: коли закриття завершилося, виконує дію. */
    public void tick() {
        if (closingAt >= 0L && after != null && Util.getMillis() - closingAt >= MS) {
            Runnable action = after;
            after = null;
            action.run();
        }
    }

    private float eased() {
        long now = Util.getMillis();
        float p = closingAt >= 0L ? 1.0F - (now - closingAt) / (float) MS : (now - openedAt) / (float) MS;
        p = Math.max(0.0F, Math.min(1.0F, p));
        return p * p * (3.0F - 2.0F * p);
    }

    /** Малює затемнення й вмикає перетворення. Після малювання вікна обов'язково викликати end. */
    public void begin(GuiGraphics g, int width, int height, int cx, int cy) {
        float e = eased();
        int a1 = (int) (0xC0 * e);
        int a2 = (int) (0xD0 * e);
        g.fillGradient(0, 0, width, height, (a1 << 24) | 0x101010, (a2 << 24) | 0x101010);
        float scale = 0.9F + 0.1F * e;
        g.pose().pushPose();
        g.pose().translate(cx, cy + (1.0F - e) * 8.0F, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.pose().translate(-cx, -cy, 0.0F);
    }

    public void end(GuiGraphics g) {
        g.pose().popPose();
    }
}
