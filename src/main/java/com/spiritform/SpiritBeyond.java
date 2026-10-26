package com.spiritform;

import net.minecraft.advancements.AdvancementHolder;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Вимір "Потойбіччя" (spirit_form:beyond). Сам вимір описаний даними:
 * data/spirit_form/dimension/beyond.json, dimension_type/beyond.json і worldgen/biome/beyond.json.
 *
 * Зайти можна, лише якщо гравець має досягнення "Король Потойбіччя" (king), у нього ввімкнене Відлуння Смерті
 * і він клацнув ПКМ по блоку "Відлуння Потойбіччя". Вийти можна клацнувши по такому блоку в самому Потойбіччі.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritBeyond {

    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "beyond"));

    private SpiritBeyond() {
    }

    /** ПКМ по блоку Відлуння Потойбіччя. */
    public static void use(ServerPlayer player, BlockPos echoPos) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        if (player.level().dimension().equals(KEY)) {
            leave(player, server);
            return;
        }

        SpiritData data = SpiritFormState.get(player);
        if (!data.hasAbility(SpiritAbility.ECHO) || !data.echo()) {
            player.displayClientMessage(Component.translatable("message.spirit_form.beyond.need_echo"), true);
            return;
        }
        if (!hasKing(player, server)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.beyond.need_king"), true);
            return;
        }
        ServerLevel beyond = server.getLevel(KEY);
        if (beyond == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.beyond.missing"), true);
            return;
        }

        SpiritEchoIndex.get(server).putReturn(player.getUUID(),
                new SpiritEchoIndex.Entry(player.level().dimension().location().toString(), player.blockPosition()));

        // Туди, звідки гравець вийшов минулого разу, а вперше на тих самих x/z, що й блок
        SpiritEchoIndex.Entry exit = SpiritEchoIndex.get(server).getExit(player.getUUID());
        BlockPos arrival = null;
        if (exit != null && exit.dimension().equals(KEY.location().toString())) {
            if (canStandAt(beyond, exit.pos())) {
                arrival = exit.pos();
            } else {
                arrival = new BlockPos(exit.pos().getX(), surfaceY(beyond, exit.pos().getX(), exit.pos().getZ()),
                        exit.pos().getZ());
            }
        }
        if (arrival == null) {
            arrival = new BlockPos(echoPos.getX(), surfaceY(beyond, echoPos.getX(), echoPos.getZ()), echoPos.getZ());
        }
        travel(player, beyond, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
        player.displayClientMessage(Component.translatable("message.spirit_form.beyond.entered"), true);
        SpiritAdvancements.grant(player, "beyond_home");
    }

    /** Запам'ятовує місце гравця в Потойбіччі (якщо він там). */
    public static void rememberExit(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null && player.level().dimension().equals(KEY)) {
            SpiritEchoIndex.get(server).putExit(player.getUUID(),
                    new SpiritEchoIndex.Entry(KEY.location().toString(), player.blockPosition()));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 40 == 0) {
            rememberExit(player);
        }
    }

    /** Чи є в цій клітинці місце для гравця: ноги й голова вільні, під ногами твердо. */
    private static boolean canStandAt(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isInWorldBounds(pos.above()) || !level.isInWorldBounds(pos.below())) {
            return false;
        }
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();
    }

    /**
     * Висота, на якій гравець стане на поверхню (або на воду) у цьому стовпчику. Шукаємо згори донизу й самі
     * дивимося на блоки, бо карта висот у щойно створеному чанку буває ще порожньою (тоді гравця закидало до
     * бедроку). Якщо поверхні немає, повертаємо найвищу точку світу: гравець падатиме.
     */
    private static int surfaceY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 0, z);
        int min = level.getMinBuildHeight();
        for (int y = level.getMaxBuildHeight() - 3; y > min + 1; y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            boolean water = !state.getFluidState().isEmpty();
            if (!water && state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            pos.setY(y + 1);
            boolean freeAbove = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            pos.setY(y + 2);
            freeAbove &= level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            if (freeAbove) {
                return y + 1;
            }
        }
        return level.getMaxBuildHeight() - 2;
    }

    private static void leave(ServerPlayer player, MinecraftServer server) {
        rememberExit(player);
        SpiritEchoIndex index = SpiritEchoIndex.get(server);
        SpiritEchoIndex.Entry back = index.getReturn(player.getUUID());
        index.removeReturn(player.getUUID());

        ServerLevel target = server.overworld();
        BlockPos pos = target.getSharedSpawnPos();
        if (back != null) {
            ResourceLocation id = ResourceLocation.tryParse(back.dimension());
            ServerLevel level = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (level != null) {
                target = level;
                pos = back.pos();
            }
        }
        travel(player, target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.displayClientMessage(Component.translatable("message.spirit_form.beyond.left"), true);
    }

    private static void travel(ServerPlayer player, ServerLevel target, double x, double y, double z) {
        if (player.level() instanceof ServerLevel from) {
            from.sendParticles(SpiritParticles.MINT, player.getX(), player.getY() + 0.9, player.getZ(),
                    40, 0.4, 0.8, 0.4, 0.08);
        }
        player.teleportTo(target, x, y, z, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        target.sendParticles(SpiritParticles.MINT, x, y + 0.9, z, 40, 0.4, 0.8, 0.4, 0.08);
        target.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    static boolean hasKing(ServerPlayer player, MinecraftServer server) {
        AdvancementHolder holder = server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "king"));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
