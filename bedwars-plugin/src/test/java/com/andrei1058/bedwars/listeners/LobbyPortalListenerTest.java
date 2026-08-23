package com.andrei1058.bedwars.listeners;

import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyPortalListenerTest {

    @Test
    void onlyLobbyNetherPortalsReturnToProxyLobby() {
        assertTrue(LobbyPortalListener.shouldReturnToProxyLobby(
                true, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL));
        assertFalse(LobbyPortalListener.shouldReturnToProxyLobby(
                false, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL));
        assertFalse(LobbyPortalListener.shouldReturnToProxyLobby(
                true, PlayerTeleportEvent.TeleportCause.END_PORTAL));
    }

    @Test
    void proxyContextWithoutLocalLobbyStillReturnsFromNetherPortal() {
        assertTrue(LobbyPortalListener.shouldReturnToProxyLobby(
                false, true, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL));
        assertFalse(LobbyPortalListener.shouldReturnToProxyLobby(
                false, true, PlayerTeleportEvent.TeleportCause.END_PORTAL));
    }
}
