package com.andrei1058.bedwars.listeners.joinhandler;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JoinListenerSharedTest {

    @Test
    public void onlyRestoresAnExistingValidReservation() {
        assertTrue(JoinListenerShared.shouldAutoRejoin(true, true));
        assertFalse(JoinListenerShared.shouldAutoRejoin(true, false));
        assertFalse(JoinListenerShared.shouldAutoRejoin(false, true));
        assertFalse(JoinListenerShared.shouldAutoRejoin(false, false));
    }
}
