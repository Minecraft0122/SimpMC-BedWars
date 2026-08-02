package com.andrei1058.bedwars.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyInventoryPolicyTest {

    @Test
    void acceptsOnlyTheCurrentUnassignedLobbyContext() {
        assertTrue(LobbyInventoryPolicy.shouldApply(true, false, false, "lobby", "Lobby"));
        assertFalse(LobbyInventoryPolicy.shouldApply(false, false, false, "lobby", "lobby"));
        assertFalse(LobbyInventoryPolicy.shouldApply(true, true, false, "lobby", "lobby"));
        assertFalse(LobbyInventoryPolicy.shouldApply(true, false, true, "lobby", "lobby"));
        assertFalse(LobbyInventoryPolicy.shouldApply(true, false, false, "arena", "lobby"));
        assertFalse(LobbyInventoryPolicy.shouldApply(true, false, false, "lobby", ""));
    }
}
