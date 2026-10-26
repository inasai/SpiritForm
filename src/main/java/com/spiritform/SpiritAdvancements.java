package com.spiritform;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Досягнення мода. Усі вони описані у data/spirit_form/advancement/*.json з умовою "impossible"
 * (крім двох із предметами), а видає їх сам мод у потрібний момент.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritAdvancements {

    private SpiritAdvancements() {
    }

    /** Видає досягнення за назвою файлу (наприклад, "begin"). Повторно не видається. */
    public static void grant(ServerPlayer player, String id) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder holder = server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, id));
        if (holder == null) {
            return;
        }
        PlayerAdvancements advancements = player.getAdvancements();
        if (!advancements.getOrStartProgress(holder).isDone()) {
            advancements.award(holder, "trigger");
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) {
            return;
        }
        SpiritData data = SpiritFormState.get(player);
        if (!data.unlocked()) {
            return;
        }

        // Проходження крізь блоки у формі Привида
        if (data.active()) {
            AABB box = player.getBoundingBox().deflate(0.1);
            for (VoxelShape shape : player.level().getBlockCollisions(player, box)) {
                if (!shape.isEmpty()) {
                    grant(player, "phase");
                    break;
                }
            }
        }

        // Рівні всіх Шляхів
        if (player.tickCount % 40 == 0) {
            int lowest = Math.min(data.hunger(), Math.min(data.movement(), data.darkness()));
            if (lowest >= 5) {
                grant(player, "first_step");
            }
            if (lowest >= 10) {
                grant(player, "half_way");
            }
            if (lowest >= 15) {
                grant(player, "king");
            }
            // Нагорода за досягнення "Король Потойбіччя"
            if (!data.hasAbility(SpiritAbility.ARMY) && player.getServer() != null
                    && SpiritBeyond.hasKing(player, player.getServer())) {
                SpiritFormState.set(player, data.withAbility(SpiritAbility.ARMY, true));
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.spirit_form.army.unlocked"), false);
            }
        }
    }
}
