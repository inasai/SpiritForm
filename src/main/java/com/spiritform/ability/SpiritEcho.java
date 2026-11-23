package com.spiritform.ability;

import com.spiritform.data.SpiritData;
import com.spiritform.Config;
import com.spiritform.SpiritFormMod;
import com.spiritform.data.EchoSpots;
import com.spiritform.data.SpiritAttachments;
import com.spiritform.entity.EchoSmokeEntity;
import com.spiritform.registry.SpiritEntities;
import com.spiritform.data.SpiritEchoIndex;
import com.spiritform.form.SpiritBeyond;
import com.spiritform.form.SpiritFormState;
import com.spiritform.herald.SpiritHeralds;
import com.spiritform.registry.SpiritParticles;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Відлуння Смерті (пасивна здібність): коли її вмикають, на місці гравця лишається блок "Відлуння Потойбіччя".
 * Гравець грає як завжди, а коли вимикає здібність, його телепортує до Відлуння, і блок зникає.
 * Відлунь може бути кілька (їхню кількість купують у вікні прокачки): тоді вибір іде з колеса Відлунь, де їх
 * також можна перейменувати.
 *
 * МАЙБУТНЄ: ПКМ по блоку Відлуння для гравця з досягненням "Король Потойбіччя" має переносити в вимір
 * "Потойбіччя". Для цього перевизнач у блоці useWithoutItem і виклич тут новий метод onUse(player, pos).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritEcho {

    private SpiritEcho() {
    }

    /** Скільки Відлунь гравець може мати одночасно. */
    public static int limit(ServerPlayer player) {
        return Math.max(1, Math.min(Config.ECHO_LIMIT.get(), player.getData(SpiritAttachments.ECHO_LIMIT.get())));
    }

    /** Відлуння, що вже лежать у світі. */
    public static List<SpiritEchoIndex.Echo> echoes(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server == null ? List.of() : SpiritEchoIndex.get(server).echoesOf(player.getUUID());
    }

    /** Оновлює дзеркало Відлунь для клієнта й прапорець «Відлуння увімкнене» (є хоча б одне). */
    public static void push(ServerPlayer player) {
        List<EchoSpots.Spot> spots = new ArrayList<>();
        for (SpiritEchoIndex.Echo echo : echoes(player)) {
            spots.add(new EchoSpots.Spot(echo.name(), echo.dimension(), echo.pos()));
        }
        EchoSpots next = new EchoSpots(spots);
        if (!next.equals(player.getData(SpiritAttachments.ECHO_SPOTS.get()))) {
            player.setData(SpiritAttachments.ECHO_SPOTS.get(), next);
        }
        SpiritData data = SpiritFormState.get(player);
        if (data.echo() != !spots.isEmpty()) {
            SpiritFormState.set(player, data.withEcho(!spots.isEmpty()));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            push(player);
        }
    }

    /**
     * Активація здібності. Із одним місцем: перше натискання лишає Відлуння, друге повертає до нього.
     * Із кількома місцями вибір іде на колесі Відлунь (клієнт відкриває його сам), тут лише перше Відлуння.
     */
    public static void toggle(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        if (!data.hasAbility(SpiritAbility.ECHO)) {
            player.displayClientMessage(
                    Component.translatable("message.spirit_form.passive.locked", SpiritAbility.ECHO.displayName()), true);
            return;
        }
        if (player.getServer() == null || !(player.level() instanceof ServerLevel)) {
            return;
        }
        List<SpiritEchoIndex.Echo> list = echoes(player);
        if (list.isEmpty()) {
            place(player);
        } else if (limit(player) <= 1) {
            goTo(player, 0);
        } else {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.choose"), true);
        }
    }

    /** Лишає нове Відлуння на місці гравця (якщо є вільне місце під нього). */
    public static void place(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        MinecraftServer server = player.getServer();
        if (!data.hasAbility(SpiritAbility.ECHO) || server == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        SpiritEchoIndex index = SpiritEchoIndex.get(server);
        if (index.echoesOf(player.getUUID()).size() >= limit(player)) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.full"), true);
            return;
        }

        // Місце Відлуння запам'ятовується, а на ньому 5 секунд висить м'ятний дим, з яким можна взаємодіяти
        BlockPos place = player.blockPosition();
        index.add(player.getUUID(), new SpiritEchoIndex.Echo(level.dimension().location().toString(), place, ""));
        push(player);

        EchoSmokeEntity smoke = SpiritEntities.ECHO_SMOKE.get().create(level);
        if (smoke != null) {
            smoke.moveTo(place.getX() + 0.5, place.getY(), place.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(smoke);
        }
        level.sendParticles(SpiritParticles.MINT, place.getX() + 0.5, place.getY() + 0.5, place.getZ() + 0.5,
                30, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, place, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("message.spirit_form.echo.placed"), true);
    }

    /** Переносить гравця до Відлуння з цим номером; блок зникає, а місце в обліку звільняється. */
    public static void goTo(ServerPlayer player, int number) {
        MinecraftServer server = player.getServer();
        if (server == null || !SpiritFormState.get(player).hasAbility(SpiritAbility.ECHO)) {
            return;
        }
        SpiritEchoIndex index = SpiritEchoIndex.get(server);
        SpiritEchoIndex.Echo entry = index.removeAt(player.getUUID(), number);
        push(player);
        if (entry == null) {
            return;
        }

        ResourceLocation dimension = ResourceLocation.tryParse(entry.dimension());
        ServerLevel target = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.spirit_form.echo.lost"), true);
            return;
        }

        BlockPos pos = entry.pos();
        boolean fromBeyond = player.level().dimension().equals(SpiritBeyond.KEY);
        SpiritBeyond.rememberExit(player);
        puff(player);
        player.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
        puff(player);

        clearAt(target, pos);
        player.displayClientMessage(Component.translatable("message.spirit_form.echo.returned"), true);
        if (fromBeyond) {
            SpiritHeralds.onReturn(player);
        }
    }

    /** Нова назва Відлуння (порожня повертає стандартну). */
    public static void rename(ServerPlayer player, int number, String name) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        String clean = name == null ? "" : name.strip();
        if (clean.length() > EchoSpots.NAME_MAX) {
            clean = clean.substring(0, EchoSpots.NAME_MAX);
        }
        SpiritEchoIndex.get(server).rename(player.getUUID(), number, clean);
        push(player);
    }

    /** Скидання здібності: гравця повертає до першого Відлуння, решту блоків прибрано. */
    public static void releaseAll(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        List<SpiritEchoIndex.Echo> list = echoes(player);
        if (list.isEmpty()) {
            return;
        }
        // Спершу прибираємо всі, крім першого: після кожного видалення номери зсуваються
        for (int i = list.size() - 1; i >= 1; i--) {
            SpiritEchoIndex.Echo extra = SpiritEchoIndex.get(server).removeAt(player.getUUID(), i);
            if (extra != null) {
                ResourceLocation id = ResourceLocation.tryParse(extra.dimension());
                ServerLevel level = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                if (level != null) {
                    clearAt(level, extra.pos());
                }
            }
        }
        goTo(player, 0);
    }

    /** Прибирає дим Відлуння на цьому місці. */
    private static void clearAt(ServerLevel level, BlockPos pos) {
        for (EchoSmokeEntity smoke : level.getEntitiesOfClass(EchoSmokeEntity.class, new AABB(pos).inflate(1.5))) {
            smoke.discard();
        }
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
