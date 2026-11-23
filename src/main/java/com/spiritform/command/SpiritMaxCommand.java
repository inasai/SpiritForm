package com.spiritform.command;

import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.ability.SpiritAbility;
import com.spiritform.data.SpiritAttachments;
import com.spiritform.data.SpiritData;
import com.spiritform.data.StationData;
import com.spiritform.form.SpiritBalance;
import com.spiritform.form.SpiritBranch;
import com.spiritform.form.SpiritForm;
import com.spiritform.form.SpiritFormState;
import com.spiritform.vault.Station;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /spirit_max: відкриває форму Привида й усі здібності та розвиває все до максимуму (потрібні права оператора).
 * /spirit_max &lt;гравець&gt; робить те саме для іншого гравця. Рівні досвіду не витрачаються.
 *
 * Максимум: форма й усі три шляхи, рівні здібності «Форма Привида», усі здібності (разом із нагородами за
 * досягнення), Уламки Душі, місця Армії, кількість Відлунь і всі станції Самодостатності з їхніми сторінками.
 * Сховище артефактів і спорядження команда не чіпає. Увімкнені чи вимкнені стани здібностей лишаються як були.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritMaxCommand {

    private SpiritMaxCommand() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spirit_max")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer player;
                    try {
                        player = ctx.getSource().getPlayerOrException();
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(Component.translatable("command.spirit_form.unlock.no_player"));
                        return 0;
                    }
                    return maximize(ctx.getSource(), player);
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> maximize(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))));
        dispatcher.register(root);
    }

    private static int maximize(CommandSourceStack source, ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player).withUnlocked(true);
        for (SpiritBranch branch : SpiritBranch.values()) {
            data = data.withLevel(branch, SpiritBalance.MAX_LEVEL);
        }
        data = data.withFormLevel(SpiritData.FORM_MAX);
        for (SpiritAbility ability : SpiritAbility.values()) {
            data = data.withAbility(ability, true);
        }
        data = data.withShards(Config.SOUL_SHARD_LIMIT.get()).withArmySlots(Config.ARMY_MAX_SLOTS.get());
        SpiritFormState.set(player, data);
        SpiritForm.refreshStats(player);

        player.setData(SpiritAttachments.ECHO_LIMIT.get(), Config.ECHO_LIMIT.get());

        StationData stations = player.getData(SpiritAttachments.STATIONS.get());
        for (Station station : Station.values()) {
            if (!station.external()) {
                stations = stations.withUnlocked(station);
            }
        }
        stations = stations.withPages(Math.max(stations.pages(), Station.pages(stations.blocks())));
        player.setData(SpiritAttachments.STATIONS.get(), stations);

        source.sendSuccess(() -> Component.translatable("command.spirit_form.max.done", player.getDisplayName()), true);
        return 1;
    }
}
