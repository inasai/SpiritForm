package com.spiritform;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Правила Клунника: захист від вогню поруч із ним, його гнів і «Клунників жар».
 * Жар не є ефектом зілля, тож Імунітет Привида його не знімає. Він шкодить і формі Привида,
 * але не добиває (лишає хоча б сердечко), і псує міцність речей, не ламаючи їх (лишається 1 міцність).
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class KlunnykEvents {

    public static final String HEAT = "spirit_form_klunnyk_heat";
    /** 12 секунд. */
    public static final int DURATION = 240;
    private static final float DAMAGE = 1.0F;
    private static final float MIN_HEALTH = 2.0F;
    private static final float WEAR = 0.08F;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private KlunnykEvents() {
    }

    public static int heat(Player player) {
        return player.getPersistentData().getInt(HEAT);
    }

    /** Накладає (або поновлює) жар. */
    public static void punish(Player player, Klunnyk source) {
        CompoundTag data = player.getPersistentData();
        boolean fresh = data.getInt(HEAT) <= 0;
        data.putInt(HEAT, Math.max(data.getInt(HEAT), DURATION));
        if (fresh) {
            player.displayClientMessage(Component.translatable("message.spirit_form.klunnyk.angry"), true);
        }
        if (player.isOnFire()) {
            player.clearFire();
        }
    }

    public static void calm(Player player) {
        player.getPersistentData().putInt(HEAT, 0);
    }

    /** Чи береже гравця спокійний Клунник поблизу. */
    public static boolean protects(Player player) {
        if (heat(player) > 0 || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        List<Klunnyk> list = level.getEntitiesOfClass(Klunnyk.class, player.getBoundingBox().inflate(Klunnyk.PROTECT_RADIUS),
                k -> k.isAlive() && !k.isAngryAt(player) && k.distanceToSqr(player) <= Klunnyk.PROTECT_RADIUS * Klunnyk.PROTECT_RADIUS);
        return !list.isEmpty();
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // 1. Удар по Клунникові: прямий удар гравця або його стріла. Армія й ауры не рахуються.
        if (event.getEntity() instanceof Klunnyk klunnyk && !klunnyk.level().isClientSide) {
            DamageSource source = event.getSource();
            Player attacker = null;
            Entity direct = source.getDirectEntity();
            if (direct instanceof Player p) {
                attacker = p;
            } else if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player p) {
                attacker = p;
            }
            if (attacker != null) {
                if (klunnyk.level() instanceof ServerLevel level) {
                    for (Klunnyk k : level.getEntitiesOfClass(Klunnyk.class, klunnyk.getBoundingBox().inflate(24.0))) {
                        k.angerAt(attacker);
                    }
                }
                klunnyk.angerAt(attacker);
                punish(attacker, klunnyk);
            }
            return;
        }
        // 2. Вогонь поруч зі спокійним Клунником не шкодить.
        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_FIRE) && protects(player)) {
            event.setCanceled(true);
            if (player.isOnFire()) {
                player.clearFire();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        int heat = data.getInt(HEAT);
        if (heat <= 0) {
            return;
        }
        heat--;
        data.putInt(HEAT, heat);
        if (player.level() instanceof ServerLevel level && heat % 4 == 0) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.3, 0.5, 0.3, 0.01);
        }
        if (heat % 20 == 0) {
            burn(player);
        }
    }

    private static void burn(ServerPlayer player) {
        float hp = player.getHealth();
        if (hp > MIN_HEALTH) {
            player.hurt(player.damageSources().magic(), Math.min(DAMAGE, hp - MIN_HEALTH));
        }
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }
            int max = stack.getMaxDamage();
            int add = Math.max(1, Math.round(max * WEAR));
            stack.setDamageValue(Math.min(max - 1, stack.getDamageValue() + add));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getPersistentData().putInt(HEAT, 0);
        }
    }
}
