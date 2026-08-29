package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreakPlaceShearsTest {

    @Test
    void allowsTheFirstShearsBreak() {
        assertTrue(BreakPlace.isShearsBreakReady(null, 1_000L));
    }

    @Test
    void enforcesOneSecondBetweenShearsBreaks() {
        assertFalse(BreakPlace.isShearsBreakReady(1_000L, 1_999L));
        assertTrue(BreakPlace.isShearsBreakReady(1_000L, 2_000L));
    }

    @Test
    void handlesClockMovingBackwardsConservatively() {
        assertFalse(BreakPlace.isShearsBreakReady(2_000L, 1_000L));
    }
}
