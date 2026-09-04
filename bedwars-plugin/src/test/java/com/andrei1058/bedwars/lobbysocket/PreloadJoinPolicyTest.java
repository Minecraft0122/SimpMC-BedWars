package com.andrei1058.bedwars.lobbysocket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreloadJoinPolicyTest {

    @Test
    public void explicitPlayerRequestDoesNotUseAnOldReconnectLease() {
        assertFalse(PreloadJoinPolicy.expectsRejoin("PLAYER", true));
        assertFalse(PreloadJoinPolicy.expectsRejoin("SPECTATOR", true));
    }

    @Test
    public void explicitAndLegacyRejoinRequestsAreRecognized() {
        assertTrue(PreloadJoinPolicy.expectsRejoin("REJOIN", false));
        assertTrue(PreloadJoinPolicy.expectsRejoin("AUTO", true));
        assertFalse(PreloadJoinPolicy.expectsRejoin("AUTO", false));
        assertEquals("AUTO", PreloadJoinPolicy.normalize(" "));
    }

    @Test
    public void reconnectTokenMustMatchWhenOneWasRequested() {
        assertTrue(PreloadJoinPolicy.matchesReservation("", "current"));
        assertTrue(PreloadJoinPolicy.matchesReservation("current", "current"));
        assertFalse(PreloadJoinPolicy.matchesReservation("old", "current"));
    }

    @Test
    public void explicitJoinModeCannotChangeMeaningDuringProxyHandoff() {
        assertTrue(PreloadJoinPolicy.acceptsArenaState("PLAYER", "waiting"));
        assertTrue(PreloadJoinPolicy.acceptsArenaState("PLAYER", "STARTING"));
        assertFalse(PreloadJoinPolicy.acceptsArenaState("PLAYER", "PLAYING"));
        assertTrue(PreloadJoinPolicy.acceptsArenaState("SPECTATOR", "PLAYING"));
        assertFalse(PreloadJoinPolicy.acceptsArenaState("SPECTATOR", "WAITING"));
        assertFalse(PreloadJoinPolicy.acceptsArenaState("AUTO", "RESTARTING"));
    }
}
