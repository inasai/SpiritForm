package com.spiritform.registry;

import net.minecraft.world.level.GameRules;

/**
 * Ігрове правило, що задає режим форми Привида.
 * <p>
 * false (за замовчуванням, режим «Потойбіччя»): здібність «Форма Привида» треба розвивати: політ з 2 рівня,
 * проходження крізь блоки з 3, невразливість з 4.
 * true (режим «Overworld»): здібність одразу на максимумі, форма працює повністю, як раніше.
 * Змінюється командою /gamerule spiritFormOverworldMode true|false або в налаштуваннях правил при створенні світу.
 */
public final class SpiritGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> OVERWORLD_MODE = GameRules.register(
            "spiritFormOverworldMode",
            GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(false));

    private SpiritGameRules() {
    }

    /** Викликається з конструктора мода, щоб правило зареєструвалося до завантаження світу. */
    public static void init() {
    }
}
