package com.spiritform;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Налаштування мода.
 * <p>
 * COMMON - ігрові числа, які читає сервер: config/spirit_form-common.toml
 * CLIENT - налаштування відображення, лише на клієнті: config/spirit_form-client.toml
 * <p>
 * Підписи в екрані налаштувань беруться з файлів перекладів за ключами
 * spirit_form.configuration.<назва> (підказка: те саме з суфіксом .tooltip).
 */
public final class Config {

    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    private static final List<Number> DEFAULT_LEVEL_COSTS = List.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    );

    // ---------- COMMON ----------

    public static final ModConfigSpec.ConfigValue<List<? extends Number>> LEVEL_COSTS = COMMON_BUILDER
            .comment(
                    "Experience levels charged for each upgrade level (15 values).",
                    "The 1st value is the shared Spirit Form unlock (level 1),",
                    "the 2nd-15th values are the cost of levels 2-15 of every branch."
            )
            .translation("spirit_form.configuration.levelCosts")
            .defineListAllowEmpty("levelCosts", DEFAULT_LEVEL_COSTS, () -> 1, Config::validateCost);

    public static final ModConfigSpec.IntValue INVISIBILITY_DURATION_SECONDS = COMMON_BUILDER
            .comment("How long invisibility lasts, in seconds.")
            .translation("spirit_form.configuration.invisibilityDurationSeconds")
            .defineInRange("invisibilityDurationSeconds", 60, 1, 3600);

    public static final ModConfigSpec.IntValue ABILITY_HUNGER_COST = COMMON_BUILDER
            .comment("How many hunger points invisibility and teleportation cost per use.")
            .translation("spirit_form.configuration.abilityHungerCost")
            .defineInRange("abilityHungerCost", 6, 0, 20);

    public static final ModConfigSpec.DoubleValue TELEPORT_RANGE = COMMON_BUILDER
            .comment("Maximum teleportation range, in blocks.")
            .translation("spirit_form.configuration.teleportRange")
            .defineInRange("teleportRange", 64.0, 1.0, 256.0);

    public static final ModConfigSpec.IntValue ABILITY_COOLDOWN_SECONDS = COMMON_BUILDER
            .comment("Cooldown of invisibility and teleportation, in seconds.")
            .translation("spirit_form.configuration.abilityCooldownSeconds")
            .defineInRange("abilityCooldownSeconds", 5, 0, 3600);

    // ---------- CLIENT ----------

    public static final ModConfigSpec.DoubleValue HIGHLIGHT_RADIUS = CLIENT_BUILDER
            .comment("Radius (in blocks) in which mobs are highlighted at Darkness level 15.")
            .translation("spirit_form.configuration.highlightRadius")
            .defineInRange("highlightRadius", 30.0, 1.0, 128.0);

    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();
    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private static boolean validateCost(Object value) {
        return value instanceof Number number && number.intValue() >= 0 && number.intValue() <= 1000;
    }

    private Config() {
    }
}
