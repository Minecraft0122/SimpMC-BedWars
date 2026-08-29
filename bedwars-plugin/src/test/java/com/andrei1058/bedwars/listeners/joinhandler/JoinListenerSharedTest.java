package com.andrei1058.bedwars.listeners.joinhandler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinListenerSharedTest {

    @Test
    void onlyRestoresAnExistingValidReservation() {
        assertTrue(JoinListenerShared.shouldAutoRejoin(true, true));
        assertFalse(JoinListenerShared.shouldAutoRejoin(true, false));
        assertFalse(JoinListenerShared.shouldAutoRejoin(false, true));
        assertFalse(JoinListenerShared.shouldAutoRejoin(false, false));
    }
}
