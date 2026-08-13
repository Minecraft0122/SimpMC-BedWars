package com.andrei1058.bedwars.shop.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecallScrollCountdownTest {

    @Test
    void advancesOneSecondAtATimeAndCompletesAfterFiveAdvances() {
        RecallScrollCountdown countdown = new RecallScrollCountdown(5);

        assertEquals(5, countdown.secondsRemaining());
        assertFalse(countdown.advance());
        assertEquals(4, countdown.secondsRemaining());
        assertFalse(countdown.advance());
        assertFalse(countdown.advance());
        assertFalse(countdown.advance());
        assertTrue(countdown.advance());
        assertEquals(0, countdown.secondsRemaining());
    }

    @Test
    void rejectsInvalidDurationsAndAdvancingCompletedCountdown() {
        assertThrows(IllegalArgumentException.class, () -> new RecallScrollCountdown(0));
        RecallScrollCountdown countdown = new RecallScrollCountdown(1);
        assertTrue(countdown.advance());
        assertThrows(IllegalStateException.class, countdown::advance);
    }
}
