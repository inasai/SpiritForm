package com.spiritform.command;

import com.spiritform.SpiritFormMod;
import com.spiritform.form.SpiritBeyond;
import com.spiritform.worldgen.HeatlandSites;
import com.spiritform.worldgen.MavkaTrees;
import com.spiritform.worldgen.TitanTrees;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Пошук дерев: /spirit_locate titan і /spirit_locate mavka_tree (лише в Потойбіччі, потрібні права оператора).
 * Дерева створюються як фічі, а не структури, тому ванільне /locate structure їх не бачить.
 * Місця дерев задає сітка за зерном світу, тож команда знаходить їх, навіть якщо чанк ще не завантажений.
 * Але саме дерево з'явиться лише там, де чанки згенеровано після встановлення оновлення.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritLocateCommand {

    /** Скільки клітинок сітки перевіряти в кожен бік. */
    private static final int TITAN_CELLS = 6;
    private static final int MAVKA_CELLS = 10;

    private SpiritLocateCommand() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spirit_locate")
                .requires(source -> source.hasPermission(2));
        root.then(Commands.literal("titan").executes(ctx -> locate(ctx.getSource(), true)));
        root.then(Commands.literal("volcano").executes(ctx -> locateVolcano(ctx.getSource())));
        root.then(Commands.literal("mavka_tree").executes(ctx -> locate(ctx.getSource(), false)));
        dispatcher.register(root);
    }

    private static int locateVolcano(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (!level.dimension().equals(SpiritBeyond.KEY)) {
            source.sendFailure(Component.translatable("command.spirit_form.locate.wrong_dimension"));
            return 0;
        }
        Vec3 from = source.getPosition();
        HeatlandSites.Volcano v = HeatlandSites.nearest(level, from.x, from.z, 4);
        if (v == null) {
            source.sendFailure(Component.translatable("command.spirit_form.locate.no_volcano"));
            return 0;
        }
        BlockPos summit = HeatlandSites.summit(v);
        String tp = "/tp @s " + summit.getX() + " " + (summit.getY() + 3) + " " + summit.getZ();
        MutableComponent coords = Component.literal("[" + summit.getX() + ", " + summit.getY() + ", " + summit.getZ() + "]")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("command.spirit_form.locate.click"))));
        double dx = summit.getX() - from.x;
        double dz = summit.getZ() - from.z;
        int blocks = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        source.sendSuccess(() -> Component.translatable("command.spirit_form.locate.volcano", coords, blocks), false);
        return 1;
    }

    private static int locate(CommandSourceStack source, boolean titan) {
        ServerLevel level = source.getLevel();
        if (!level.dimension().equals(SpiritBeyond.KEY)) {
            source.sendFailure(Component.translatable("command.spirit_form.locate.wrong_dimension"));
            return 0;
        }
        Vec3 from = source.getPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int cell = titan ? TitanTrees.CELL : MavkaTrees.CELL;
        int radius = titan ? TITAN_CELLS : MAVKA_CELLS;
        int cx0 = Math.floorDiv((int) from.x, cell);
        int cz0 = Math.floorDiv((int) from.z, cell);
        for (int cx = cx0 - radius; cx <= cx0 + radius; cx++) {
            for (int cz = cz0 - radius; cz <= cz0 + radius; cz++) {
                BlockPos found;
                if (titan) {
                    TitanTrees.Site site = TitanTrees.site(level, cx, cz);
                    found = site != null && site.valid()
                            ? new BlockPos(site.center().getX(), site.baseY(), site.center().getZ()) : null;
                } else {
                    BlockPos c = MavkaTrees.center(level.getSeed(), cx, cz);
                    found = c == null ? null : new BlockPos(c.getX(), (int) from.y, c.getZ());
                }
                if (found == null) {
                    continue;
                }
                double dx = found.getX() - from.x;
                double dz = found.getZ() - from.z;
                double d = dx * dx + dz * dz;
                if (d < bestDist) {
                    bestDist = d;
                    best = found;
                }
            }
        }
        if (best == null) {
            source.sendFailure(Component.translatable("command.spirit_form.locate.not_found"));
            return 0;
        }
        int y = titan ? best.getY() + 2 : best.getY();
        String tp = "/tp @s " + best.getX() + " " + y + " " + best.getZ();
        MutableComponent coords = Component.literal("[" + best.getX() + ", " + (titan ? y : "~") + ", " + best.getZ() + "]")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("command.spirit_form.locate.click"))));
        int blocks = (int) Math.round(Math.sqrt(bestDist));
        source.sendSuccess(() -> Component.translatable(
                titan ? "command.spirit_form.locate.titan" : "command.spirit_form.locate.mavka", coords, blocks), false);
        return 1;
    }
}
