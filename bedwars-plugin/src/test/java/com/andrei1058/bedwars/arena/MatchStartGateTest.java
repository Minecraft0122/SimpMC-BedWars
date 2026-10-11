package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchStartGateTest {

    @AfterEach
    void leaveGateOpenForOtherTests() {
        MatchStartGate.resume();
    }

    @Test
    void blocksAndResumesOnlyOnActualStateChanges() {
        MatchStartGate.resume();
        assertTrue(MatchStartGate.isOpen());

        assertTrue(MatchStartGate.stop());
        assertFalse(MatchStartGate.isOpen());
        assertFalse(MatchStartGate.stop());

        assertTrue(MatchStartGate.resume());
        assertTrue(MatchStartGate.isOpen());
        assertFalse(MatchStartGate.resume());
    }
}
