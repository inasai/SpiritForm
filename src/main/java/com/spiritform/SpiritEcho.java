package com.spiritform;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Відлуння Смерті (пасивна здібність): коли її вмикають, на місці гравця лишається блок "Відлуння Потойбіччя".
 * Гравець грає як завжди, а коли вимикає здібність, його телепортує до Відлуння, і блок зникає.
 *
 * МАЙБУТНЄ: ПКМ по блоку Відлуння для гравця з досягненням "Король Потойбіччя" має переносити в вимір
 * "Потойбіччя". Для цього перевизнач у блоці useWithoutItem і виклич тут новий метод onUse(player, pos).
 */
public final class SpiritEcho {

    private SpiritEcho() {
    }

    /** Вмикає або вимикає Відлуння Смерті. */
    public static void toggle(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        if (!data.hasAbility(SpiritAbility.ECHO)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.passive.locked", SpiritAbility.ECHO.displayName()), true);
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        SpiritEchoIndex index = SpiritEchoIndex.get(server);
        if (!data.echo()) {
            enable(player, level, index, data);
        } else {
            disable(player, server, index, data);
        }
    }

    private static void enable(ServerPlayer player, ServerLevel level, SpiritEchoIndex index, SpiritData data) {
        BlockPos base = player.blockPosition();
        BlockPos place = null;
        for (int up = 0; up <= 2 && place == null; up++) {
            BlockPos candidate = base.above(up);
            if (level.isInWorldBounds(candidate) && level.getBlockState(candidate).canBeReplaced()) {
                place = candidate;
            }
        }
        if (place == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.no_space"), true);
            return;
        }

        level.setBlock(place, SpiritBlocks.ECHO_BLOCK.get().defaultBlockState(), 3);
        index.put(player.getUUID(), new SpiritEchoIndex.Entry(level.dimension().location().toString(), place));
        SpiritFormState.set(player, data.withEcho(true));

        level.sendParticles(SpiritParticles.MINT, place.getX() + 0.5, place.getY() + 0.5, place.getZ() + 0.5,
                30, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, place, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("message.spirit_form.echo.placed"), true);
    }

    private static void disable(ServerPlayer player, MinecraftServer server, SpiritEchoIndex index, SpiritData data) {
        UUID id = player.getUUID();
        SpiritEchoIndex.Entry entry = index.get(id);
        SpiritFormState.set(player, data.withEcho(false));
        index.remove(id);

        ResourceLocation dimension = ResourceLocation.tryParse(entry == null ? "" : entry.dimension());
        ServerLevel target = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (entry == null || target == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.lost"), true);
            return;
        }

        BlockPos pos = entry.pos();
        SpiritBeyond.rememberExit(player);
        puff(player);
        player.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        puff(player);

        BlockState state = target.getBlockState(pos);
        if (state.is(SpiritBlocks.ECHO_BLOCK.get())) {
            target.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        player.displayClientMessage(Component.translatable("message.spirit_form.echo.returned"), true);
    }

    private static void puff(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(SpiritParticles.MINT, player.getX(), player.getY() + 0.9, player.getZ(),
                    40, 0.4, 0.8, 0.4, 0.08);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }
}
