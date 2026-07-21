package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyReturnItemTest {

    @Test
    void multiArenaLobbyBedUsesTheProxyLobbyAction() {
        assertTrue(Interact.shouldConnectToProxyLobby("bw leave", true, "bw"));
        assertTrue(Interact.shouldConnectToProxyLobby(" BW LEAVE ", true, "bw"));
        assertFalse(Interact.shouldConnectToProxyLobby("bw leave", false, "bw"));
        assertFalse(Interact.shouldConnectToProxyLobby("bw gui", true, "bw"));
    }
}
