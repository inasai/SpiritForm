package com.spiritform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Вимір my_dimension: ключ і допоміжний метод для телепорту. Викликайте MyDimension.enter(...) з того місця, де гравець має потрапляти у вимір. */
public final class MyDimension {

    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(SpiritFormMod.MODID, "my_dimension"));

    private MyDimension() {
    }

    /** Переносить гравця у вимір у точку (x, y, z). Повертає false, якщо виміру не знайдено. */
    public static boolean enter(ServerPlayer player, double x, double y, double z) {
        MinecraftServer server = player.getServer();
        ServerLevel level = server == null ? null : server.getLevel(KEY);
        if (level == null) {
            return false;
        }
        player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        return true;
    }

    /** Повертає гравця в Верхній світ на його точку відродження чи на спавн світу. */
    public static void leave(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        double x = overworld.getSharedSpawnPos().getX() + 0.5;
        double y = overworld.getSharedSpawnPos().getY();
        double z = overworld.getSharedSpawnPos().getZ() + 0.5;
        player.teleportTo(overworld, x, y, z, player.getYRot(), player.getXRot());
    }
}
