package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyReturnItemTest {

    @Test
    void defaultLobbyBedUsesTheInternalReturnAction() {
        assertTrue(Interact.isLobbyReturnCommand("bw leave", true, "bw"));
        assertTrue(Interact.isLobbyReturnCommand(" BW LEAVE ", true, "bw"));
        assertFalse(Interact.isLobbyReturnCommand("bw leave", false, "bw"));
        assertFalse(Interact.isLobbyReturnCommand("bw gui", true, "bw"));
    }
}
