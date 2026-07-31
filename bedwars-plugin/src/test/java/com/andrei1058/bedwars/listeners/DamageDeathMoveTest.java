package com.andrei1058.bedwars.listeners;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageDeathMoveTest {

    @Test
    void detectsChunkChangesWithoutResolvingChunkObjects() {
        assertFalse(DamageDeathMove.changedChunk(
                new Location(null, 1, 64, 1), new Location(null, 15, 80, 15)));
        assertTrue(DamageDeathMove.changedChunk(
                new Location(null, 15, 64, 15), new Location(null, 16, 64, 15)));
        assertTrue(DamageDeathMove.changedChunk(
                new Location(null, -16, 64, 0), new Location(null, -17, 64, 0)));
    }
}
