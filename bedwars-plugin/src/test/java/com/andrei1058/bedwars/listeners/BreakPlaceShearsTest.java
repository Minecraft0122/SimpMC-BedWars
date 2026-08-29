package com.andrei1058.bedwars.listeners;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BreakPlaceShearsTest {

    @Test
    public void allowsTheFirstShearsBreak() {
        assertTrue(BreakPlace.isShearsBreakReady(null, 1_000L));
    }

    @Test
    public void enforcesOneSecondBetweenShearsBreaks() {
        assertFalse(BreakPlace.isShearsBreakReady(1_000L, 1_999L));
        assertTrue(BreakPlace.isShearsBreakReady(1_000L, 2_000L));
    }

    @Test
    public void handlesClockMovingBackwardsConservatively() {
        assertFalse(BreakPlace.isShearsBreakReady(2_000L, 1_000L));
    }
}
