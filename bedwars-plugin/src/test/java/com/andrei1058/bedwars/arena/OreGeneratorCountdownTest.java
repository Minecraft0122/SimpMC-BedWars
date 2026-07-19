package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreGeneratorCountdownTest {

    @Test
    void configuredDelayMatchesActualSpawnInterval() {
        int delay = 4;
        int remaining = delay;
        int spawns = 0;

        for (int second = 1; second <= 8; second++) {
            if (OreGenerator.isSpawnDue(remaining)) {
                spawns++;
                remaining = delay;
            } else {
                remaining--;
            }
        }

        assertEquals(2, spawns);
    }

    @Test
    void clampsUnsafeDelayAndSpawnsAtOneSecond() {
        assertEquals(1, OreGenerator.normalizeDelay(0));
        assertEquals(1, OreGenerator.normalizeDelay(-5));
        assertTrue(OreGenerator.isSpawnDue(1));
        assertTrue(OreGenerator.isSpawnDue(0));
        assertFalse(OreGenerator.isSpawnDue(2));
    }
}
