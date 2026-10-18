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
        }
    }

    private static void unlock(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        if (data.unlocked()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.already_unlocked"), true);
            return;
        }
        if (!pay(player, SpiritBalance.cost(1))) {
            return;
        }
        SpiritFormState.set(player, data.withUnlocked(true));
        player.displayClientMessage(Component.translatable("message.spirit_form.unlocked"), true);
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
        if (!pay(player, SpiritBalance.cost(next))) {
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
            return true; // у творчому режимі безкоштовно
        }
        if (player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("message.spirit_form.not_enough_xp", cost), true);
            return false;
        }
        player.giveExperienceLevels(-cost);
        return true;
    }
}
