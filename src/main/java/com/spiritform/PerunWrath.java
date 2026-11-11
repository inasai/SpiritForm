package com.spiritform;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Гнів Богу. Той, хто вбив дику Жар-птицю, чує: «Перун, Бог Неба, помітив тебе», потім «Тікай!» і має 5 секунд.
 * Далі 5 хвилин діє Гнів: поки у вбивці в інвентарі лежить перо Жар-птиці, а він стоїть у Жарокраї під відкритим небом,
 * щосекунди втрачає сердечко (шкода проходить і крізь форму Привида). Це не ефект зілля,
 * тож Імунітет Привида, молоко й тотем його не знімають. Смерть гравця його скасовує.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class PerunWrath {

    private static final String PRE = "spirit_form_perun_pre";
    private static final String WRATH = "spirit_form_perun_wrath";
    private static final String SEEN = "spirit_form_perun_seen";
    /** 5 секунд на втечу після «Тікай!» і ще 2 секунди між першим повідомленням і другим. */
    private static final int PRE_TICKS = 140;
    private static final int RUN_AT = 100;
    private static final int WRATH_TICKS = 6000;
    private static final float DAMAGE = 2.0F;

    private PerunWrath() {
    }

    public static boolean isActive(ServerPlayer player) {
        CompoundTag d = player.getPersistentData();
        return d.getInt(PRE) > 0 || d.getInt(WRATH) > 0;
    }

    /** Починає відлік гніву (якщо він уже йде, нічого не змінює). */
    public static void mark(ServerPlayer player) {
        if (isActive(player)) {
            return;
        }
        player.getPersistentData().putInt(PRE, PRE_TICKS);
        SpiritAdvancements.grant(player, "bestiary/perun");
        player.sendSystemMessage(Component.translatable("message.spirit_form.perun.noticed"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 3.0F, 0.7F);
    }

    private static boolean hasFeather(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(SpiritCreatures.PHOENIX_FEATHER.get())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        int pre = data.getInt(PRE);
        if (pre > 0) {
            pre--;
            data.putInt(PRE, pre);
            if (pre == RUN_AT) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.spirit_form.perun.run")));
                player.sendSystemMessage(Component.translatable("message.spirit_form.perun.run"));
            }
            if (pre == 0) {
                data.putInt(WRATH, WRATH_TICKS);
                player.displayClientMessage(Component.translatable("message.spirit_form.perun.begins"), true);
            }
            return;
        }
        int wrath = data.getInt(WRATH);
        if (wrath <= 0) {
            return;
        }
        wrath--;
        data.putInt(WRATH, wrath);
        if (wrath == 0) {
            data.putBoolean(SEEN, false);
            player.displayClientMessage(Component.translatable("message.spirit_form.perun.ends"), true);
            return;
        }
        if (wrath % 20 != 0) {
            return;
        }
        boolean exposed = level.dimension().equals(SpiritBeyond.KEY)
                && level.getBiome(player.blockPosition()).is(SpiritBiomes.HEATLAND)
                && level.canSeeSky(BlockPos.containing(player.getEyePosition()))
                && hasFeather(player);
        if (!exposed) {
            data.putBoolean(SEEN, false);
            return;
        }
        if (!data.getBoolean(SEEN)) {
            data.putBoolean(SEEN, true);
            player.displayClientMessage(Component.translatable("message.spirit_form.perun.sees"), true);
        }
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().magic(), DAMAGE);
        if (wrath % 60 == 0) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                double ang = level.random.nextDouble() * Math.PI * 2.0;
                bolt.moveTo(player.getX() + Math.cos(ang) * 7.0, player.getY(), player.getZ() + Math.sin(ang) * 7.0);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag d = player.getPersistentData();
            d.putInt(PRE, 0);
            d.putInt(WRATH, 0);
            d.putBoolean(SEEN, false);
        }
    }
}
