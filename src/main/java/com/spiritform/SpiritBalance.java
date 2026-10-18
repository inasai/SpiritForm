package com.spiritform;

import java.util.List;

/**
 * Усі числа балансу в одному місці. Щоб змінити швидкість, голод чи тривалість Темряви,
 * правити потрібно тільки цей файл.
 */
public final class SpiritBalance {

    public static final int MAX_LEVEL = 15;

    /** Стандартна швидкість польоту у Майнкрафті (Abilities.flyingSpeed). */
    public static final float DEFAULT_FLY_SPEED = 0.05F;

    // Орієнтири швидкості у блоках за секунду
    private static final double SLOW_WALK_BPS = 3.0;      // повільніше за ходьбу
    private static final double WALK_BPS = 4.317;         // звичайна ходьба
    private static final double SPRINT_BPS = 5.612;       // звичайний спринт

    // Приблизно: flyingSpeed 0.05 дає близько 10 блоків за секунду, залежність лінійна
    private static final double BPS_PER_FLY_SPEED = 202.0;

    // Витрата виснаження на один пройдений блок.
    // У ванільному Майнкрафті ходьба голод не витрачає зовсім, тому "ходьбі" тут
    // відповідає невелике додатне значення.
    private static final double HUNGER_SPRINT_JUMP = 0.15;
    private static final double HUNGER_SPRINT = 0.10;
    private static final double HUNGER_WALK = 0.03;
    private static final double HUNGER_HALF_WALK = 0.015;

    private SpiritBalance() {
    }

    public static int clamp(int level) {
        return Math.max(1, Math.min(MAX_LEVEL, level));
    }

    /**
     * Скільки рівнів досвіду коштує підвищення ДО цього рівня.
     * Значення беруться зі списку levelCosts у конфігу (1-й елемент: рівень 1, 2-й: рівень 2 тощо).
     * Якщо запису немає, вартість дорівнює номеру рівня.
     */
    public static int cost(int level) {
        level = clamp(level);
        try {
            List<? extends Number> costs = Config.LEVEL_COSTS.get();
            if (level <= costs.size()) {
                return Math.max(0, costs.get(level - 1).intValue());
            }
        } catch (IllegalStateException ignored) {
            // конфіг ще не завантажено
        }
        return level;
    }

    /** Скільки рівнів досвіду вкладено в одну гілку (без спільного рівня 1). */
    public static int branchSpent(int level) {
        int total = 0;
        for (int i = 2; i <= clamp(level); i++) {
            total += cost(i);
        }
        return total;
    }

    /** Скільки рівнів досвіду вкладено у всі гілки (без спільного рівня 1). */
    public static int branchesSpent(SpiritData data) {
        int total = 0;
        for (SpiritBranch branch : SpiritBranch.values()) {
            total += branchSpent(data.level(branch));
        }
        return total;
    }

    // ---------- Рух ----------

    public static double speedBlocksPerSecond(int level) {
        level = clamp(level);
        if (level <= 5) {
            return lerp(SLOW_WALK_BPS, WALK_BPS, (level - 1) / 4.0);
        }
        if (level <= 10) {
            return lerp(WALK_BPS, SPRINT_BPS, (level - 5) / 5.0);
        }
        if (level <= 14) {
            double bonus = 0.15 + 0.05 * (level - 11); // 15%, 20%, 25%, 30%
            return SPRINT_BPS * (1.0 + bonus);
        }
        return SPRINT_BPS * 2.0;
    }

    public static float flySpeed(int level) {
        return (float) (speedBlocksPerSecond(level) / BPS_PER_FLY_SPEED);
    }

    // ---------- Голод ----------

    public static double exhaustionPerBlock(int level) {
        level = clamp(level);
        if (level <= 5) {
            return lerp(HUNGER_SPRINT_JUMP, HUNGER_SPRINT, (level - 1) / 4.0);
        }
        if (level <= 10) {
            return lerp(HUNGER_SPRINT, HUNGER_WALK, (level - 5) / 5.0);
        }
        return lerp(HUNGER_WALK, HUNGER_HALF_WALK, (level - 10) / 5.0);
    }

    // ---------- Темрява ----------

    /** Рівні 1-5: Темрява діє постійно, поки гравець у формі. */
    public static boolean darknessConstantInForm(int level) {
        return clamp(level) <= 5;
    }

    /** Рівні 6-14: Темрява діє лише певний час після входу у форму. 0 = не діє. */
    public static int darknessInFormSeconds(int level) {
        level = clamp(level);
        if (level >= 6 && level <= 9) {
            return 120 - 20 * (level - 6); // 120, 100, 80, 60
        }
        if (level == 10) {
            return 30;
        }
        if (level >= 11 && level <= 14) {
            return 25 - 5 * (level - 11); // 25, 20, 15, 10
        }
        return 0;
    }

    /** Темрява після виходу з форми. Рівні 1-4: 30, 20, 10, 5 секунд, далі немає. */
    public static int darknessAfterSeconds(int level) {
        return switch (clamp(level)) {
            case 1 -> 30;
            case 2 -> 20;
            case 3 -> 10;
            case 4 -> 5;
            default -> 0;
        };
    }

    public static boolean highlightsMobs(int level) {
        return clamp(level) >= MAX_LEVEL;
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
