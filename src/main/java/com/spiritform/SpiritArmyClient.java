package com.spiritform;

import java.util.ArrayList;
import java.util.List;

/** Клієнтська копія сховища армії (лише для відображення; усе вирішує сервер). */
public final class SpiritArmyClient {

    private static List<ArmySyncPayload.Entry> souls = new ArrayList<>();
    private static int order;

    private SpiritArmyClient() {
    }

    public static void update(ArmySyncPayload payload) {
        souls = new ArrayList<>(payload.souls());
        order = payload.order();
    }

    public static List<ArmySyncPayload.Entry> souls() {
        return souls;
    }

    public static int order() {
        return order;
    }
}
