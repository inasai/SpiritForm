package com.spiritform;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /spirit_unlock: відкриває всі записи Бестіарію й Щоденника (потрібні права оператора).
 * Видає досягнення, що ховають записи: усі bestiary/* та beyond_home (запис про Бестіарій у Щоденнику).
 * Інші досягнення мода (Король Потойбіччя тощо) команда не чіпає.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritUnlockCommand {

    private SpiritUnlockCommand() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spirit_unlock")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> unlock(ctx.getSource()));
        dispatcher.register(root);
    }

    private static int unlock(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.spirit_form.unlock.no_player"));
            return 0;
        }
        PlayerAdvancements advancements = player.getAdvancements();
        int count = 0;
        for (AdvancementHolder holder : source.getServer().getAdvancements().getAllAdvancements()) {
            if (!holder.id().getNamespace().equals(SpiritFormMod.MODID)) {
                continue;
            }
            String path = holder.id().getPath();
            if (!path.startsWith("bestiary/") && !path.equals("beyond_home")) {
                continue;
            }
            AdvancementProgress progress = advancements.getOrStartProgress(holder);
            if (progress.isDone()) {
                continue;
            }
            for (String criterion : progress.getRemainingCriteria()) {
                advancements.award(holder, criterion);
            }
            count++;
        }
        int opened = count;
        source.sendSuccess(() -> Component.translatable("command.spirit_form.unlock.done", opened), true);
        return count;
    }
}
