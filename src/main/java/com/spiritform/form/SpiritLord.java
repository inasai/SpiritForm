package com.spiritform.form;

import com.spiritform.SpiritFormMod;
import com.spiritform.ability.SpiritAbility;
import com.spiritform.data.SpiritData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Володар Потойбіччя (поглинуто Серце Смерті): будь-яка витрата голоду від дій
 * (біг, стрибки, бій, рух у формі Привида) вдвічі менша, навіть поза формою.
 * Плата голодом за здібності зменшується окремо, у FentonBracelet.cost.
 *
 * Як це працює: вичерпання ("exhaustion") росте від дій. Щотіка дивимося, наскільки воно зросло,
 * і лишаємо половину приросту. Якщо за цей тік гра вже перевела вичерпання в голод, цей тік не чіпаємо.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class SpiritLord {

    private static final Map<UUID, Float> LAST = new HashMap<>();

    private static final ResourceLocation ENERGY_ID =
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "energy_of_the_beyond");

    private SpiritLord() {
    }

    /** Чи ввімкнена в гравця "Енергія Потойбіччя" (її знає й Армія: сила й міцність удвічі). */
    public static boolean energyOn(MinecraftServer server, UUID owner) {
        if (server == null) {
            return false;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        return player != null && energyOn(player);
    }

    public static boolean energyOn(ServerPlayer player) {
        SpiritData data = SpiritFormState.get(player);
        return data.energy() && data.hasAbility(SpiritAbility.ENERGY);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            boolean on = energyOn(player);
            boolean has = maxHealth.hasModifier(ENERGY_ID);
            if (on && !has) {
                // Енергія Потойбіччя: здоров'я гравця вдвічі більше
                maxHealth.addTransientModifier(new AttributeModifier(ENERGY_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (!on && has) {
                maxHealth.removeModifier(ENERGY_ID);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }

        FoodData food = player.getFoodData();
        float now = food.getExhaustionLevel();
        Float prev = LAST.get(player.getUUID());
        if (prev != null && now > prev && SpiritFormState.get(player).lord()) {
            now = prev + (now - prev) * 0.5F;
            food.setExhaustion(now);
        }
        LAST.put(player.getUUID(), now);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
