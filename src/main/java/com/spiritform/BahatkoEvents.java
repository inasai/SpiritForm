package com.spiritform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Вогонь, на якому щось готують, приваблює Багатьків. Лише в Жарокраї (Потойбіччя). Коли гравець забирає результат
 * із печі, коптильні, домни чи варильної стійки (справжніх чи з Самодостатності), є невеликий шанс, що поруч
 * із ним з'явиться Багатько: кілька секунд він висить із іскрами, потім нападає.
 * Щоб не набридало: шанс 3% за забирання, не частіше ніж раз на хвилину й не більше двох таких Багатьків поряд.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class BahatkoEvents {

    private static final float CHANCE = 0.03F;
    private static final int COOLDOWN = 1200;
    private static final int MAX_NEARBY = 2;
    private static final String COOLDOWN_KEY = "spirit_form_bahatko_lure_cd";

    private BahatkoEvents() {
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tryLure(player);
        }
    }

    @SubscribeEvent
    public static void onBrewed(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tryLure(player);
        }
    }

    private static void tryLure(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(SpiritBeyond.KEY)
                || !level.getBiome(player.blockPosition()).is(SpiritBiomes.HEATLAND)) {
            return;
        }
        long now = level.getGameTime();
        if (now < player.getPersistentData().getLong(COOLDOWN_KEY) || level.random.nextFloat() >= CHANCE) {
            return;
        }
        int nearby = level.getEntitiesOfClass(Bahatko.class, player.getBoundingBox().inflate(24.0), Bahatko::isFromCraft).size();
        if (nearby >= MAX_NEARBY) {
            return;
        }
        Bahatko bahatko = SpiritCreatures.BAHATKO.get().create(level);
        if (bahatko == null) {
            return;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double distance = 1.5 + level.random.nextDouble();
        bahatko.moveTo(player.getX() + Math.cos(angle) * distance, player.getY() + 1.2,
                player.getZ() + Math.sin(angle) * distance, level.random.nextFloat() * 360.0F, 0.0F);
        BlockPos pos = bahatko.blockPosition();
        bahatko.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        bahatko.beginLure(player);
        level.addFreshEntity(bahatko);
        player.getPersistentData().putLong(COOLDOWN_KEY, now + COOLDOWN);
        player.displayClientMessage(Component.translatable("message.spirit_form.bahatko.lured"), true);
    }
}
