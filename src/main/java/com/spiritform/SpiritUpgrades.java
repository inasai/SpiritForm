package com.spiritform;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Покупка рівнів на сервері. Клієнт лише просить про дію, а все перевіряє сервер.
 */
public final class SpiritUpgrades {

    private SpiritUpgrades() {
    }

    public static void handle(ServerPlayer player, int action) {
        if (action == UpgradeSpiritPayload.TOGGLE_INSTINCT) {
            toggleInstinct(player);
            return;
        }

        if (action == UpgradeSpiritPayload.TOGGLE_ICY) {
            togglePassive(player, SpiritAbility.ICY_TOUCH);
            return;
        }

        if (action == UpgradeSpiritPayload.TOGGLE_PRESENCE) {
            togglePassive(player, SpiritAbility.PRESENCE);
            return;
        }

        if (action == UpgradeSpiritPayload.TOGGLE_GAZE) {
            togglePassive(player, SpiritAbility.GAZE);
            return;
        }

        if (action == UpgradeSpiritPayload.UNLOCK) {
            unlock(player);
            return;
        }

        SpiritBranch upgradeBranch = UpgradeSpiritPayload.branchFor(action);
        if (upgradeBranch != null) {
            upgrade(player, upgradeBranch);
            return;
        }

        SpiritBranch resetBranch = UpgradeSpiritPayload.resetBranchFor(action);
        if (resetBranch != null) {
            resetBranch(player, resetBranch);
            return;
        }

        SpiritAbility ability = UpgradeSpiritPayload.abilityFor(action);
        if (ability != null) {
            unlockAbility(player, ability);
        }
    }

    /** Вмикає або вимикає пасивну здібність (Крижаний Доторк, Присутність). */
    private static void togglePassive(ServerPlayer player, SpiritAbility ability) {
        SpiritData data = SpiritFormState.get(player);
        if (!data.hasAbility(ability)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.passive.locked", ability.displayName()), true);
            return;
        }

        boolean enable;
        SpiritData updated;
        if (ability == SpiritAbility.ICY_TOUCH) {
            enable = !data.icy();
            updated = data.withIcy(enable);
        } else if (ability == SpiritAbility.GAZE) {
            enable = !data.gaze();
            updated = data.withGaze(enable);
        } else {
            enable = !data.presence();
            updated = data.withPresence(enable);
        }
        SpiritFormState.set(player, updated);

        player.displayClientMessage(
                Component.translatable(
                        enable ? "message.spirit_form.passive.on" : "message.spirit_form.passive.off",
                        ability.displayName()),
                true);
    }

    private static void unlockAbility(ServerPlayer player, SpiritAbility ability) {
        SpiritData data = SpiritFormState.get(player);

        if (!data.unlocked()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.need_unlock"), true);
            return;
        }
        if (data.hasAbility(ability)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.ability.already"), true);
            return;
        }

        int required = ability.requiredLevel();
        if (!ability.isAvailable(data)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.ability.requirement", required),
                    true
            );
            return;
        }

        if (pay(player, ability.cost())) {
            return;
        }

        SpiritFormState.set(player, data.withAbility(ability, true));
        player.displayClientMessage(
                Component.translatable("message.spirit_form.ability.unlocked", ability.displayName()),
                true
        );

        SpiritAdvancements.grant(player, "expand");
        switch (ability.tier) {
            case BASIC -> SpiritAdvancements.grant(player, "death_felt");
            case ADVANCED -> SpiritAdvancements.grant(player, "death_eyes");
            case MASTER -> SpiritAdvancements.grant(player, "death_stronger");
        }
        if (ability == SpiritAbility.POSSESSION) {
            SpiritAdvancements.grant(player, "puppet");
        }
    }

    private static void toggleInstinct(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        if (!data.unlocked()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.need_unlock"), true);
            return;
        }

        boolean enabled = !data.instinct();
        SpiritFormState.set(player, data.withInstinct(enabled));
        player.displayClientMessage(
                Component.translatable(enabled
                        ? "message.spirit_form.instinct.on"
                        : "message.spirit_form.instinct.off"),
                true
        );
    }

    private static void unlock(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        if (data.unlocked()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.already_unlocked"), true);
            return;
        }
        if (pay(player, SpiritBalance.cost(1))) {
            return;
        }
        SpiritFormState.set(player, data.withUnlocked(true));
        player.displayClientMessage(Component.translatable("message.spirit_form.unlocked"), true);
        SpiritAdvancements.grant(player, "begin");
        SpiritGuide.give(player);
    }

    private static void upgrade(ServerPlayer player, SpiritBranch branch) {
        SpiritData data = SpiritFormState.get(player);

        if (!data.unlocked()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.need_unlock"), true);
            return;
        }

        int current = data.level(branch);
        if (current >= SpiritBalance.MAX_LEVEL) {
            player.displayClientMessage(Component.translatable("message.spirit_form.max_level"), true);
            return;
        }

        int next = current + 1;
        if (pay(player, SpiritBalance.cost(next))) {
            return;
        }

        SpiritFormState.set(player, data.withLevel(branch, next));
        SpiritForm.refreshStats(player);

        player.displayClientMessage(
                Component.translatable(
                        "message.spirit_form.upgraded",
                        Component.translatable("branch.spirit_form." + branch.id),
                        next
                ),
                true
        );
    }

    /** Скидає рівень однієї гілки до 1 і повертає витрачені на неї рівні досвіду. */
    private static void resetBranch(ServerPlayer player, SpiritBranch branch) {
        SpiritData data = SpiritFormState.get(player);

        int level = data.level(branch);
        if (level <= 1) {
            return;
        }

        int refund = SpiritBalance.branchSpent(level);

        SpiritFormState.set(player, data.withLevel(branch, 1));
        SpiritForm.refreshStats(player);

        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(refund);
        }

        player.displayClientMessage(
                Component.translatable(
                        "message.spirit_form.reset_branch",
                        Component.translatable("branch.spirit_form." + branch.id),
                        refund
                ),
                true
        );
    }

    private static boolean pay(ServerPlayer player, int cost) {
        if (player.getAbilities().instabuild) {
            return false; // у творчому режимі безкоштовно
        }
        if (player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("message.spirit_form.not_enough_xp", cost), true);
            return true;
        }
        player.giveExperienceLevels(-cost);
        return false;
    }
}
