package com.spiritform.event;

import com.spiritform.SpiritFormMod;
import com.spiritform.data.VodianykIndex;
import com.spiritform.entity.Vodianyk;
import com.spiritform.form.SpiritBeyond;
import com.spiritform.registry.SpiritCreatures;
import com.spiritform.worldgen.MavkaTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Події біому річок: Водяник з'являється один раз на кожну ділянку річки, коли гравець опиняється поруч.
 */
@EventBusSubscriber(modid = SpiritFormMod.MODID)
public final class RiverEvents {

    private RiverEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(SpiritBeyond.KEY)
                || level.getGameTime() % 80 != 0 || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        VodianykIndex index = VodianykIndex.get(level.getServer());
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !isRiver(level, player.blockPosition())) {
                continue;
            }
            long cell = VodianykIndex.cellOf(player.getBlockX(), player.getBlockZ());
            if (index.has(cell)) {
                continue;
            }
            BlockPos pos = findWater(level, player);
            if (pos == null) {
                continue;
            }
            Vodianyk vodianyk = SpiritCreatures.VODIANYK.get().create(level);
            if (vodianyk == null) {
                continue;
            }
            vodianyk.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(vodianyk)) {
                vodianyk.discard();
                continue;
            }
            vodianyk.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            level.addFreshEntity(vodianyk);
            index.add(cell);
        }
    }

    private static boolean isRiver(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(MavkaTreeFeature.RIVER);
    }

    /** Глибока (щонайменше 2 блоки) вода річки на 12..32 блоків від гравця, або null. */
    private static BlockPos findWater(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 12.0 + random.nextDouble() * 20.0;
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos surface = new BlockPos(x, top - 1, z);
            BlockPos below = surface.below();
            if (level.getFluidState(surface).is(FluidTags.WATER) && level.getFluidState(below).is(FluidTags.WATER)
                    && level.getBiome(surface).is(MavkaTreeFeature.RIVER)) {
                return below;
            }
        }
        return null;
    }
}
