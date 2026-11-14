package com.spiritform.command;

import com.spiritform.SpiritFormMod;
import com.spiritform.item.SoulShardTools;
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
 * /spirit_shard: повертає гравцеві його Уламок Душі з резервної копії, якщо той не з'явився або зник.
 * /spirit_shard &lt;гравець&gt; (права оператора) робить те саме для іншого гравця.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritShardCommand {

    private SpiritShardCommand() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spirit_shard")
                .executes(ctx -> {
                    ServerPlayer player;
                    try {
                        player = ctx.getSource().getPlayerOrException();
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(Component.translatable("command.spirit_form.unlock.no_player"));
                        return 0;
                    }
                    return SoulShardTools.recall(player) ? 1 : 0;
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> SoulShardTools.recall(EntityArgument.getPlayer(ctx, "player")) ? 1 : 0));
        dispatcher.register(root);
    }
}
