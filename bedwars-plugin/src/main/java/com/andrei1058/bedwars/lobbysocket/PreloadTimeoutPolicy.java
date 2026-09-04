/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.lobbysocket;

final class PreloadTimeoutPolicy {

    private static final long HANDOFF_GRACE_MILLIS = 15_000L;

    private PreloadTimeoutPolicy() {
    }

    /**
     * bwp-time-out 历史上使用毫秒。确保它覆盖大厅确认和代理切服时间。
     */
    static long effectiveTimeoutMillis(long configuredMillis, int dispatchTimeoutSeconds) {
        long configured = Math.max(1_000L, configuredMillis);
        long dispatchMillis = Math.max(1, dispatchTimeoutSeconds) * 1_000L;
        return Math.max(configured, dispatchMillis + HANDOFF_GRACE_MILLIS);
    }
}
