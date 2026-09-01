package com.andrei1058.bedwars.lobbysocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RejoinLeasePolicyTest {

    @Test
    void expiredLeaseIsNotDispatchable() {
        LobbySocketServer.RejoinMessage lease = lease("reservation-1", 10_000L);

        assertTrue(LobbyArenaDispatcher.isActiveLease(lease, 9_999L));
        assertFalse(LobbyArenaDispatcher.isActiveLease(lease, 10_000L));
    }

    @Test
    void delayedRemoveCannotDeleteAReplacedLease() {
        LobbySocketServer.RejoinMessage current = lease("new", 20_000L);

        assertFalse(LobbyArenaDispatcher.matchesRemoval(current,
                new LobbySocketServer.RejoinRemoveMessage(current.uuid(), "old", current.sessionId())));
        assertTrue(LobbyArenaDispatcher.matchesRemoval(current,
                new LobbySocketServer.RejoinRemoveMessage(current.uuid(), "new", current.sessionId())));
        assertTrue(LobbyArenaDispatcher.matchesRemoval(current,
                new LobbySocketServer.RejoinRemoveMessage(current.uuid(), "", current.sessionId())));
        assertFalse(LobbyArenaDispatcher.matchesRemoval(current,
                new LobbySocketServer.RejoinRemoveMessage(current.uuid(), "new", "other-session")));
    }

    private static LobbySocketServer.RejoinMessage lease(String reservationId, long expiresAt) {
        return new LobbySocketServer.RejoinMessage(
                "00000000-0000-0000-0000-000000000001", "arena_world", "arena-01",
                "arena-01", reservationId, expiresAt, "session-1");
    }
}
