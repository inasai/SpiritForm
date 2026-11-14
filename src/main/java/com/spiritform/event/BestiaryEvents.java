package com.spiritform.event;

import com.spiritform.SpiritFormMod;
import com.spiritform.form.SpiritBeyond;
import com.spiritform.registry.SpiritBiomes;
import com.spiritform.worldgen.HeatlandSites;
import com.spiritform.worldgen.MavkaTrees;
import com.spiritform.worldgen.TitanTrees;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Розблокування записів Бестіарію. Записи про біоми й предмети відкриваються досягненнями з data/spirit_form/advancement/bestiary
 * (місцезнаходження та вміст інвентаря), а тут обробляються зустрічі: щойно гравець бачить істоту (або вона поруч), видається
 * досягнення з назвою цієї істоти; Дерева Мавок і Титанічні Дерева розпізнаються за місцем.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class BestiaryEvents {

    private static final Set<String> CREATURES = Set.of(
            "mavka", "chuhaister", "hukha", "bisyk", "skarbnyk", "zlydeny", "vodianyk", "vodianytsia",
            "povitrulia", "perelisnyk", "nichnytsia", "potercha", "polovyk", "vovkulaka", "chort", "lisovyk", "klunnyk", "bahatko", "phoenix");
    private static final Set<String> HERALDS = Set.of("storm_herald", "hunger_herald", "cold_herald", "death_herald");

    private static final double SEE_RANGE = 32.0;
    private static final double HERALD_RANGE = 48.0;
    private static final double CLOSE_SQR = 8.0 * 8.0;

    private BestiaryEvents() {
    }

    private static void grant(ServerPlayer player, String id) {
        SpiritAdvancements.grant(player, "bestiary/" + id);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 7 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();

        List<Entity> found = level.getEntities(player, player.getBoundingBox().inflate(HERALD_RANGE), e -> {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            return key.getNamespace().equals(SpiritFormMod.MODID)
                    && (CREATURES.contains(key.getPath()) || HERALDS.contains(key.getPath()));
        });
        for (Entity entity : found) {
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            boolean herald = HERALDS.contains(path);
            double distSqr = player.distanceToSqr(entity);
            if (!herald && distSqr > SEE_RANGE * SEE_RANGE) {
                continue;
            }
            if (distSqr < CLOSE_SQR || player.hasLineOfSight(entity)) {
                grant(player, path);
                if (!herald) {
                    grant(player, "relations");
                    if (path.equals("mavka") || path.equals("nichnytsia")) {
                        grant(player, "affection");
                    }
                }
            }
        }

        // Вулкан Жарокраю: побачити його вершину
        if (level.dimension().equals(SpiritBeyond.KEY)) {
            HeatlandSites.Volcano volcano = HeatlandSites.nearest(level, player.getX(), player.getZ(), 1);
            if (volcano != null && volcano.cone().dist(player.getBlockX(), player.getBlockZ()) < 110.0) {
                grant(player, "volcano");
            }
        }

        // Дерева: лише в лісових біомах Потойбіччя
        if (level.dimension().equals(SpiritBeyond.KEY) && level.getBiome(player.blockPosition()).is(SpiritBiomes.FOREST_LIKE)) {
            int x = player.getBlockX();
            int z = player.getBlockZ();
            if (MavkaTrees.isNear(level.getSeed(), x, z, 48)) {
                grant(player, "mavka_tree");
            }
            if (TitanTrees.isNearSite(level.getSeed(), x, z, 70)) {
                grant(player, "titan_tree");
            }
        }
    }
}
