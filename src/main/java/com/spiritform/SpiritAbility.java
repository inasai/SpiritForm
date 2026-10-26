package com.spiritform;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Здібності Привида. Кожна відкривається за рівні досвіду, коли потрібна гілка (або всі гілки)
 * досягла потрібного рівня. Порядок не змінювати: за ним зберігається маска відкритих здібностей.
 */
public enum SpiritAbility {
    SENSE("sense", SpiritBranch.DARKNESS, Tier.BASIC, Kind.ACTIVE, false, Items.ENDER_EYE),
    VANISH("vanish", SpiritBranch.HUNGER, Tier.BASIC, Kind.ACTIVE, false, Items.GLASS),
    STEP("step", SpiritBranch.MOVEMENT, Tier.BASIC, Kind.ACTIVE, false, Items.ENDER_PEARL),
    POSSESSION("possession", null, Tier.MASTER, Kind.ACTIVE, false, Items.SKELETON_SKULL),
    TOUCH("touch", SpiritBranch.MOVEMENT, Tier.MASTER, Kind.ACTIVE, true, Items.PHANTOM_MEMBRANE),
    DELIRIUM("delirium", SpiritBranch.HUNGER, Tier.MASTER, Kind.ACTIVE, false, Items.ARMOR_STAND),
    ICY_TOUCH("icy_touch", SpiritBranch.MOVEMENT, Tier.ADVANCED, Kind.PASSIVE, false, Items.BLUE_ICE),
    PRESENCE("presence", null, Tier.BASIC, Kind.PASSIVE, false, Items.CAMPFIRE),
    IMMUNITY("immunity", SpiritBranch.HUNGER, Tier.ADVANCED, Kind.ACTIVE, false, Items.MILK_BUCKET),
    LIFE_GIFT("life_gift", SpiritBranch.DARKNESS, Tier.ADVANCED, Kind.ACTIVE, true, Items.GOLDEN_APPLE),
    SCALES("scales", SpiritBranch.DARKNESS, Tier.MASTER, Kind.ACTIVE, true, Items.WITHER_ROSE),
    GAZE("gaze", null, Tier.ADVANCED, Kind.PASSIVE, false, Items.SPIDER_EYE),
    ECHO("echo", SpiritBranch.DARKNESS, Tier.NOVICE, Kind.PASSIVE, false, Items.ECHO_SHARD),
    SCREAM("scream", SpiritBranch.HUNGER, Tier.NOVICE, Kind.ACTIVE, false, Items.GOAT_HORN),
    SUMMON("summon", SpiritBranch.MOVEMENT, Tier.NOVICE, Kind.ACTIVE, false, Items.LEAD),
    /** Нагорода за досягнення "Король Потойбіччя": не купується й не скидається. */
    ARMY("army", null, Tier.SPECIAL, Kind.ACTIVE, false, Items.SOUL_LANTERN);

    /** Яка вимога до рівня шляху: початкова (5 за замовчуванням) чи висока (10). Обидві задаються в конфігу. */
    public enum Tier {
        NOVICE,
        BASIC,
        ADVANCED,
        MASTER,
        /** Здібність-нагорода: рівні й вартість не потрібні. */
        SPECIAL
    }

    /** Активні здібності стоять на панелях (цифри й гарячі клавіші), пасивні вмикаються колесом. */
    public enum Kind {
        ACTIVE,
        PASSIVE
    }

    public final String id;

    /** Гілка, яка має мати потрібний рівень. null означає, що потрібні всі гілки. */
    public final SpiritBranch branch;

    public final Tier tier;
    public final Kind kind;

    /** true: здібність працює, поки утримується клавіша (Доторк, Дарування Життя, Терези). */
    public final boolean hold;

    private final Item icon;

    SpiritAbility(String id, SpiritBranch branch, Tier tier, Kind kind, boolean hold, Item icon) {
        this.id = id;
        this.branch = branch;
        this.tier = tier;
        this.kind = kind;
        this.hold = hold;
        this.icon = icon;
    }

    public Item icon() {
        return icon;
    }

    /** Здібності, відсортовані за потрібним рівнем (від меншого до більшого), далі за порядком. */
    public static java.util.List<SpiritAbility> sorted() {
        java.util.List<SpiritAbility> list = new java.util.ArrayList<>(java.util.List.of(values()));
        list.removeIf(SpiritAbility::rewardOnly);
        list.sort(java.util.Comparator.comparingInt(SpiritAbility::requiredLevel));
        return list;
    }

    public static SpiritAbility byId(String id) {
        for (SpiritAbility ability : values()) {
            if (ability.id.equals(id)) {
                return ability;
            }
        }
        return null;
    }

    public int bit() {
        return 1 << ordinal();
    }

    /** Потрібний рівень шляху (шляхів) для відкриття здібності. */
    public int requiredLevel() {
        return switch (tier) {
            case NOVICE -> Config.ABILITY_REQUIRED_LEVEL_NOVICE.get();
            case BASIC -> Config.ABILITY_REQUIRED_LEVEL_BASIC.get();
            case ADVANCED -> Config.ABILITY_REQUIRED_LEVEL_ADVANCED.get();
            case MASTER -> Config.ABILITY_REQUIRED_LEVEL_MASTER.get();
            case SPECIAL -> 0;
        };
    }

    /** Вартість відкриття здібності в рівнях досвіду (за рівнем здібності). */
    public int cost() {
        return switch (tier) {
            case NOVICE -> Config.ABILITY_COST_NOVICE.get();
            case BASIC -> Config.ABILITY_COST_BASIC.get();
            case ADVANCED -> Config.ABILITY_COST_ADVANCED.get();
            case MASTER -> Config.ABILITY_COST_MASTER.get();
            case SPECIAL -> 0;
        };
    }

    /** Здібність видається за досягнення, її не можна купити чи скинути. */
    public boolean rewardOnly() {
        return this == ARMY;
    }

    public boolean isAvailable(SpiritData data) {
        if (rewardOnly()) {
            return false;
        }
        int requiredLevel = requiredLevel();
        if (branch != null) {
            return data.level(branch) >= requiredLevel;
        }
        for (SpiritBranch other : SpiritBranch.values()) {
            if (data.level(other) < requiredLevel) {
                return false;
            }
        }
        return true;
    }

    public Component displayName() {
        return Component.translatable("ability.spirit_form." + id);
    }

    /** Опис здібності. Аргумент: клавіша режиму здібностей або колеса (її підставляє клієнт). */
    public Component description() {
        return Component.translatable(
                "ability.spirit_form." + id + ".desc",
                Component.keybind(kind == Kind.PASSIVE ? "key.spirit_form.passive_wheel" : "key.spirit_form.ability_mode")
        );
    }

    /** Вимога до рівня: один рядок. */
    public Component requirement() {
        if (rewardOnly()) {
            return Component.translatable("screen.spirit_form.ability.requirement_reward");
        }
        if (branch != null) {
            return Component.translatable(
                    "screen.spirit_form.ability.requirement",
                    Component.translatable("path.spirit_form." + branch.id),
                    requiredLevel()
            );
        }
        return Component.translatable("screen.spirit_form.ability.requirement_all", requiredLevel());
    }
}
