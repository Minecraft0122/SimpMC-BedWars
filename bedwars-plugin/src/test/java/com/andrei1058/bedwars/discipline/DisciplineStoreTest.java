/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.discipline;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisciplineStoreTest {

    private static final Instant EXISTING_UNTIL = Instant.parse("2026-01-01T01:00:00Z");

    @Test
    void replacesCooldownMetadataOnlyWhenNewCooldownExpiresLater() {
        assertTrue(DisciplineStore.shouldReplaceCooldown(
                EXISTING_UNTIL, EXISTING_UNTIL.plusSeconds(1)));

        assertFalse(DisciplineStore.shouldReplaceCooldown(
                EXISTING_UNTIL, EXISTING_UNTIL));
        assertFalse(DisciplineStore.shouldReplaceCooldown(
                EXISTING_UNTIL, EXISTING_UNTIL.minusSeconds(1)));
        assertFalse(DisciplineStore.shouldReplaceCooldown(EXISTING_UNTIL, null));
        assertTrue(DisciplineStore.shouldReplaceCooldown(null, EXISTING_UNTIL));
    }
}
