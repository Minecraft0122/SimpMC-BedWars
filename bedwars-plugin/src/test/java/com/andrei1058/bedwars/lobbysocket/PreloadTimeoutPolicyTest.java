/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.lobbysocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreloadTimeoutPolicyTest {

    @Test
    void legacyFiveSecondTimeoutCoversEightSecondDispatchAndHandoff() {
        assertEquals(23_000L, PreloadTimeoutPolicy.effectiveTimeoutMillis(5_000L, 8));
    }

    @Test
    void administratorTimeoutAboveMinimumIsPreserved() {
        assertEquals(30_000L, PreloadTimeoutPolicy.effectiveTimeoutMillis(30_000L, 8));
    }

    @Test
    void invalidValuesStillGetAUsableBudget() {
        assertEquals(16_000L, PreloadTimeoutPolicy.effectiveTimeoutMillis(0L, 0));
    }
}
