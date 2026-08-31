/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.lobbysocket;

final class PreloadTimeoutPolicy {

    private static final long HANDOFF_GRACE_MILLIS = 15_000L;

    private PreloadTimeoutPolicy() {
    }

    /**
     * The legacy timeout is expressed in milliseconds. Ensure it covers the
     * lobby confirmation timeout plus enough time for proxy login handoff.
     */
    static long effectiveTimeoutMillis(long configuredMillis, int dispatchTimeoutSeconds) {
        long configured = Math.max(1_000L, configuredMillis);
        long dispatchMillis = Math.max(1, dispatchTimeoutSeconds) * 1_000L;
        return Math.max(configured, dispatchMillis + HANDOFF_GRACE_MILLIS);
    }
}
