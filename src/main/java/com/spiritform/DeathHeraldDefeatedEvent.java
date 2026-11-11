package com.spiritform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

/**
 * Подія для майбутньої нагороди за Вісника Смерті. Її викликає SpiritHeralds один раз, коли він загинув.
 * Нагороди в моді поки немає (її дизайн не узгоджено): підпишіться на цю подію на NeoForge.EVENT_BUS,
 * щоб видати предмети чи відкрити досягнення. owner може бути null, якщо викликач уже вийшов зі світу.
 */
public class DeathHeraldDefeatedEvent extends Event {

    private final ServerLevel level;
    private final ServerPlayer owner;
    private final Vec3 position;

    public DeathHeraldDefeatedEvent(ServerLevel level, ServerPlayer owner, Vec3 position) {
        this.level = level;
        this.owner = owner;
        this.position = position;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public ServerPlayer getOwner() {
        return owner;
    }

    public Vec3 getPosition() {
        return position;
    }
}
