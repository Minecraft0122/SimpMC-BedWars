package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbandonmentPenaltyStateTest {

    @Test
    void kickedDepartureCannotBeCountedAgainByReconnectTimeout() {
        AbandonmentPenaltyState state = new AbandonmentPenaltyState();

        assertTrue(state.tryRecord(), "KICKED should claim the departure penalty");
        assertFalse(state.tryRecord(), "DISCONNECT_TIMEOUT must not count the same departure again");
    }
}
