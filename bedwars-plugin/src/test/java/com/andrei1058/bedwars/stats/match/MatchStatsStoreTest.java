/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchStatsStoreTest {

    @Test
    void widensOnlyLegacyOutcomeColumns() {
        assertTrue(MatchStatsStore.needsOutcomeColumnWidening(16L));
        assertFalse(MatchStatsStore.needsOutcomeColumnWidening(24L));
        assertFalse(MatchStatsStore.needsOutcomeColumnWidening(64L));
        assertFalse(MatchStatsStore.needsOutcomeColumnWidening(-1L));
        assertFalse(MatchStatsStore.outcomeColumnIsCompatible(false, -1L));
        assertFalse(MatchStatsStore.outcomeColumnIsCompatible(true, 16L));
        assertTrue(MatchStatsStore.outcomeColumnIsCompatible(true, 24L));
    }

    @Test
    void currentSummaryViewContractIncludesDisciplineColumns() {
        assertTrue(MatchStatsStore.expectedSummaryViewColumns().contains("afk_removed"));
        assertTrue(MatchStatsStore.expectedSummaryViewColumns().contains("violation_removed"));
        assertTrue(MatchStatsStore.expectedSummaryViewColumns().contains("effective_vl"));
        assertEquals(23, MatchStatsStore.expectedSummaryViewColumns().size());
    }
}
