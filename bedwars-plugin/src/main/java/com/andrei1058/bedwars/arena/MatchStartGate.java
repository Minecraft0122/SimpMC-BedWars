package com.andrei1058.bedwars.arena;

import java.util.concurrent.atomic.AtomicBoolean;

/** Process-local switch that prevents new games from entering their start countdown. */
public final class MatchStartGate {
    private static final AtomicBoolean OPEN = new AtomicBoolean(true);

    private MatchStartGate() { }

    public static boolean isOpen() {
        return OPEN.get();
    }

    /** @return true only when this call changed the gate from open to closed. */
    public static boolean stop() {
        return OPEN.compareAndSet(true, false);
    }

    /** @return true only when this call changed the gate from closed to open. */
    public static boolean resume() {
        return OPEN.compareAndSet(false, true);
    }
}
