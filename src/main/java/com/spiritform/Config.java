package com.spiritform;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Налаштування мода.
 *
 * COMMON - ігрові числа, які читає сервер: config/spirit_form-common.toml
 * CLIENT - налаштування відображення, лише на клієнті: config/spirit_form-client.toml
 *
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

    public static final ModConfigSpec.IntValue ABILITY_COST_BASIC = COMMON_BUILDER
            .comment("Experience levels charged for unlocking: Sense, Vanish, Step, Presence.")
            .translation("spirit_form.configuration.abilityCostBasic")
            .defineInRange("abilityCostBasic", 5, 0, 1000);

    public static final ModConfigSpec.IntValue ABILITY_COST_ADVANCED = COMMON_BUILDER
            .comment("Experience levels charged for unlocking: Icy Touch, Immunity, Life Gift, Gaze.")
            .translation("spirit_form.configuration.abilityCostAdvanced")
            .defineInRange("abilityCostAdvanced", 10, 0, 1000);

    public static final ModConfigSpec.IntValue ABILITY_COST_MASTER = COMMON_BUILDER
            .comment("Experience levels charged for unlocking: Possession, Touch, Delirium, Scales.")
            .translation("spirit_form.configuration.abilityCostMaster")
            .defineInRange("abilityCostMaster", 15, 0, 1000);

    public static final ModConfigSpec.IntValue ABILITY_REQUIRED_LEVEL_BASIC = COMMON_BUILDER
            .comment("Path level required for Spirit Sense, Vanish, Spirit Step and Presence (Presence needs all three paths).")
            .translation("spirit_form.configuration.abilityRequiredLevelBasic")
            .defineInRange("abilityRequiredLevelBasic", 5, 1, 15);

    public static final ModConfigSpec.IntValue ABILITY_REQUIRED_LEVEL_ADVANCED = COMMON_BUILDER
            .comment("Path level required for the other abilities (Possession, Life Gift and Scales need all three paths).")
            .translation("spirit_form.configuration.abilityRequiredLevelAdvanced")
            .defineInRange("abilityRequiredLevelAdvanced", 10, 1, 15);

    public static final ModConfigSpec.IntValue ABILITY_REQUIRED_LEVEL_MASTER = COMMON_BUILDER
            .comment("Path level required for the strongest abilities (Possession, Spirit Touch, Delirium, Scales).")
            .translation("spirit_form.configuration.abilityRequiredLevelMaster")
            .defineInRange("abilityRequiredLevelMaster", 15, 1, 15);

    public static final ModConfigSpec.IntValue ABILITY_REQUIRED_LEVEL_NOVICE = COMMON_BUILDER
            .comment("Path level required for Echo of Death, Spirit Scream and Summon.")
            .translation("spirit_form.configuration.abilityRequiredLevelNovice")
            .defineInRange("abilityRequiredLevelNovice", 3, 1, 15);

    public static final ModConfigSpec.IntValue ABILITY_COST_NOVICE = COMMON_BUILDER
            .comment("Experience levels charged for unlocking: Echo of Death, Spirit Scream, Summon.")
            .translation("spirit_form.configuration.abilityCostNovice")
            .defineInRange("abilityCostNovice", 3, 0, 1000);

    public static final ModConfigSpec.DoubleValue SCREAM_RADIUS = COMMON_BUILDER
            .comment("Radius in blocks in which Spirit Scream turns mobs to the player's side.")
            .translation("spirit_form.configuration.screamRadius")
            .defineInRange("screamRadius", 3.0, 1.0, 32.0);

    public static final ModConfigSpec.IntValue SCREAM_DURATION_SECONDS = COMMON_BUILDER
            .comment("For how many seconds mobs stay on the player's side after Spirit Scream.")
            .translation("spirit_form.configuration.screamDurationSeconds")
            .defineInRange("screamDurationSeconds", 60, 1, 3600);

    public static final ModConfigSpec.DoubleValue SUMMON_RANGE = COMMON_BUILDER
            .comment("How far (in blocks) Summon can reach the creature the player looks at.")
            .translation("spirit_form.configuration.summonRange")
            .defineInRange("summonRange", 48.0, 4.0, 128.0);

    public static final ModConfigSpec.BooleanValue GIVE_GUIDE_BOOK = COMMON_BUILDER
            .comment("Give the guide book \"Diary of My Death\" (needs Patchouli) when the Spirit Form is unlocked.")
            .translation("spirit_form.configuration.giveGuideBook")
            .define("giveGuideBook", true);

    public static final ModConfigSpec.IntValue SOUL_SHARD_LIMIT = COMMON_BUILDER
            .comment("Maximum number of Soul Shards a player can buy in the abilities window (the N-th costs N levels). Older shards vanish when the limit is exceeded.")
            .translation("spirit_form.configuration.soulShardLimit")
            .defineInRange("soulShardMax", 5, 1, 10);

    public static final ModConfigSpec.DoubleValue POSSESS_RANGE = COMMON_BUILDER
            .comment("Maximum distance, in blocks, from which a mob can be possessed.")
            .translation("spirit_form.configuration.possessRange")
            .defineInRange("possessRange", 16.0, 1.0, 128.0);

    public static final ModConfigSpec.DoubleValue POSSESSED_SPEED_MULTIPLIER = COMMON_BUILDER
            .comment("Speed multiplier of a possessed mob (1.0 = the mob's own speed).")
            .translation("spirit_form.configuration.possessedSpeedMultiplier")
            .defineInRange("possessedSpeedMultiplier", 1.0, 0.1, 10.0);

    public static final ModConfigSpec.IntValue TELEPORT_SMOKE_SECONDS = COMMON_BUILDER
            .comment("For how many seconds extra smoke surrounds the player after a teleport.")
            .translation("spirit_form.configuration.teleportSmokeSeconds")
            .defineInRange("teleportSmokeSeconds", 5, 0, 60);

    public static final ModConfigSpec.IntValue FORM_COOLDOWN_SECONDS = COMMON_BUILDER
            .comment("Cooldown in seconds before Spirit Form can be entered again after draining a possessed mob.")
            .translation("spirit_form.configuration.formCooldownSeconds")
            .defineInRange("formCooldownSeconds", 10, 0, 3600);

    public static final ModConfigSpec.IntValue INSTINCT_HUNGER_COST = COMMON_BUILDER
            .comment("How many hunger points one use of Spirit Instinct costs.")
            .translation("spirit_form.configuration.instinctHungerCost")
            .defineInRange("instinctHungerCost", 6, 0, 20);

    public static final ModConfigSpec.DoubleValue TOUCH_RANGE = COMMON_BUILDER
            .comment("How far (in blocks) Spirit Touch can reach a mob.")
            .translation("spirit_form.configuration.touchRange")
            .defineInRange("touchRange", 24.0, 3.0, 128.0);

    public static final ModConfigSpec.DoubleValue TOUCH_THROW_POWER = COMMON_BUILDER
            .comment("Speed (blocks per tick) a mob is thrown with when Spirit Touch is released.")
            .translation("spirit_form.configuration.touchThrowPower")
            .defineInRange("touchThrowPower", 1.0, 0.1, 5.0);

    public static final ModConfigSpec.DoubleValue TOUCH_IMPACT_DAMAGE = COMMON_BUILDER
            .comment("Damage per unit of speed when a thrown mob hits a block.")
            .translation("spirit_form.configuration.touchImpactDamage")
            .defineInRange("touchImpactDamage", 8.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue TOUCH_BLOCK_DAMAGE = COMMON_BUILDER
            .comment("Damage per unit of speed when a thrown block hits a creature.")
            .translation("spirit_form.configuration.touchBlockDamage")
            .defineInRange("touchBlockDamage", 10.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue TOUCH_BLOCK_MAX_HARDNESS = COMMON_BUILDER
            .comment("Spirit Touch cannot lift blocks harder than this (obsidian is 50, stone is 1.5, bedrock is unbreakable).")
            .translation("spirit_form.configuration.touchBlockMaxHardness")
            .defineInRange("touchBlockMaxHardness", 20.0, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue DELIRIUM_DECOY_SECONDS = COMMON_BUILDER
            .comment("How many seconds the decoy left by Delirium lasts.")
            .translation("spirit_form.configuration.deliriumDecoySeconds")
            .defineInRange("deliriumDecoySeconds", 15, 1, 600);

    public static final ModConfigSpec.IntValue ICY_RADIUS = COMMON_BUILDER
            .comment("Radius in blocks in which Icy Touch freezes water around the player.")
            .translation("spirit_form.configuration.icyRadius")
            .defineInRange("icyRadius", 3, 1, 8);

    public static final ModConfigSpec.DoubleValue PRESENCE_RADIUS = COMMON_BUILDER
            .comment("Radius in blocks in which Presence affects mobs.")
            .translation("spirit_form.configuration.presenceRadius")
            .defineInRange("presenceRadius", 24.0, 2.0, 64.0);

    public static final ModConfigSpec.DoubleValue DRAIN_HP_PER_HUNGER = COMMON_BUILDER
            .comment("How many points of mob health are converted into one hunger point when leaving Spirit Form inside a mob.")
            .translation("spirit_form.configuration.drainHpPerHunger")
            .defineInRange("drainHpPerHunger", 1.0, 0.1, 20.0);

    public static final ModConfigSpec.IntValue CHARGE_TICKS = COMMON_BUILDER
            .comment("Ticks needed to charge one heart of Life Gift or Scales while the key is held.")
            .translation("spirit_form.configuration.chargeTicks")
            .defineInRange("chargeTicks", 10, 1, 200);

    public static final ModConfigSpec.IntValue CHARGE_MAX = COMMON_BUILDER
            .comment("Maximum charge (in hearts) of Life Gift and Scales.")
            .translation("spirit_form.configuration.chargeMax")
            .defineInRange("chargeMax", 10, 1, 40);

    public static final ModConfigSpec.DoubleValue CHARGE_RANGE = COMMON_BUILDER
            .comment("How far (in blocks) the ball of Life Gift or Scales flies.")
            .translation("spirit_form.configuration.chargeRange")
            .defineInRange("chargeRange", 32.0, 4.0, 128.0);

    public static final ModConfigSpec.IntValue ARMY_MAX_SLOTS = COMMON_BUILDER
            .comment("Maximum number of souls the Army of the Beyond can store (the N-th extra slot costs N levels; the first slot is free).")
            .translation("spirit_form.configuration.armyMaxSlots")
            .defineInRange("armyMaxSlots", 10, 1, 50);

    public static final ModConfigSpec.DoubleValue ARMY_SOUL_CHANCE = COMMON_BUILDER
            .comment("Chance (0..1) that a killed mob leaves a soul wisp. Bosses always leave one.")
            .translation("spirit_form.configuration.armySoulChance")
            .defineInRange("armySoulChance", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.IntValue ARMY_WISP_SECONDS = COMMON_BUILDER
            .comment("How many seconds a soul wisp stays before it fades.")
            .translation("spirit_form.configuration.armyWispSeconds")
            .defineInRange("armyWispSeconds", 20, 1, 600);

    public static final ModConfigSpec.IntValue SCYTHE_MAX_CHARGE = COMMON_BUILDER
            .comment("Maximum charge of the Scythe of the Beyond.")
            .translation("spirit_form.configuration.scytheMaxCharge")
            .defineInRange("scytheMaxCharge", 5, 1, 20);

    public static final ModConfigSpec.IntValue SCYTHE_FREEZE_RADIUS = COMMON_BUILDER
            .comment("Radius in blocks of the mint smoke circle that freezes mobs.")
            .translation("spirit_form.configuration.scytheFreezeRadius")
            .defineInRange("scytheFreezeRadius", 10, 1, 64);

    public static final ModConfigSpec.IntValue SCYTHE_FREEZE_SECONDS = COMMON_BUILDER
            .comment("Seconds mobs stay frozen and the player stays empowered after a Scythe charge is spent.")
            .translation("spirit_form.configuration.scytheFreezeSeconds")
            .defineInRange("scytheFreezeSeconds", 10, 1, 600);

    public static final ModConfigSpec.DoubleValue SCYTHE_POWER_MULTIPLIER = COMMON_BUILDER
            .comment("Damage multiplier while the Scythe empowerment lasts.")
            .translation("spirit_form.configuration.scythePowerMultiplier")
            .defineInRange("scythePowerMultiplier", 5.0, 1.0, 100.0);

    public static final ModConfigSpec.IntValue SCYTHE_LOCK_SECONDS = COMMON_BUILDER
            .comment("Seconds the Scythe cannot gain charge after the Reaper mode ends.")
            .translation("spirit_form.configuration.scytheLockSeconds")
            .defineInRange("scytheLockSeconds", 60, 0, 3600);

    public static final ModConfigSpec.IntValue SCYTHE_HEAL_MIN_PERCENT = COMMON_BUILDER
            .comment("Minimum percent of dealt damage the Scythe heals.")
            .translation("spirit_form.configuration.scytheHealMinPercent")
            .defineInRange("scytheHealMinPercent", 20, 0, 100);

    public static final ModConfigSpec.IntValue SCYTHE_HEAL_MAX_PERCENT = COMMON_BUILDER
            .comment("Maximum percent of dealt damage the Scythe heals.")
            .translation("spirit_form.configuration.scytheHealMaxPercent")
            .defineInRange("scytheHealMaxPercent", 30, 0, 100);

    public static final ModConfigSpec.IntValue FORM_EDGE_COOLDOWN_SECONDS = COMMON_BUILDER
            .comment("Spirit Form cooldown in seconds while the Cloak of the Beyond is worn.")
            .translation("spirit_form.configuration.formEdgeCooldownSeconds")
            .defineInRange("formEdgeCooldownSeconds", 5, 0, 600);

    public static final ModConfigSpec.IntValue GEAR_PERK_SECONDS = COMMON_BUILDER
            .comment("Seconds the Trousers (speed) and Boots (soul fire trail) perks last after a kill.")
            .translation("spirit_form.configuration.gearPerkSeconds")
            .defineInRange("gearPerkSeconds", 3, 1, 60);

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
