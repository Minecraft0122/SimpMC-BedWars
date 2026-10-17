package com.andrei1058.bedwars.listeners;

import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import com.andrei1058.bedwars.arena.Misc;
import org.bukkit.PortalType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerPortalEvent;

class LobbyPortalListenerTest {

    @Test
    void endPortalContactReturnsWithoutDestinationWorldOrTeleportEvent() {
        Player player = mock(Player.class);
        try (var lobby = mockStatic(LobbyAnnouncements.class); var misc = mockStatic(Misc.class)) {
            lobby.when(() -> LobbyAnnouncements.isLobbyPlayer(player)).thenReturn(true);
            misc.when(() -> Misc.connectToProxyLobby(player)).thenReturn(true);
            var event = new EntityPortalEnterEvent(player, new Location(null, 0, 0, 0), PortalType.ENDER);
            new LobbyPortalListener().onEndPortalEnter(event);
            assertTrue(event.isCancelled());
            misc.verify(() -> Misc.connectToProxyLobby(player));
        }
    }

    @Test
    void unrelatedPlayersKeepVanillaPortalTeleport() {
        Player player = mock(Player.class);
        try (var lobby = mockStatic(LobbyAnnouncements.class); var misc = mockStatic(Misc.class)) {
            var event = new EntityPortalEnterEvent(player, new Location(null, 0, 0, 0), PortalType.ENDER);
            new LobbyPortalListener().onEndPortalEnter(event);
            assertFalse(event.isCancelled());
            misc.verifyNoInteractions();
        }
    }

    @Test
    void lobbyPlayersNeverFallThroughPortalsEvenWhenProxyFails() {
        Player player = mock(Player.class);
        try (var lobby = mockStatic(LobbyAnnouncements.class); var misc = mockStatic(Misc.class)) {
            lobby.when(() -> LobbyAnnouncements.isProxyLobbyPlayer(player)).thenReturn(true);
            misc.when(() -> Misc.connectToProxyLobby(player)).thenReturn(false);
            var event = new EntityPortalEnterEvent(player, new Location(null, 0, 0, 0), PortalType.ENDER);
            new LobbyPortalListener().onEndPortalEnter(event);
            assertTrue(event.isCancelled());
            misc.verify(() -> Misc.connectToProxyLobby(player));
        }
    }

    @Test
    void playerPortalEventCancelsVanillaEndTravelForLobbyPlayers() {
        Player player = mock(Player.class);
        try (var lobby = mockStatic(LobbyAnnouncements.class); var misc = mockStatic(Misc.class)) {
            lobby.when(() -> LobbyAnnouncements.isLobbyPlayer(player)).thenReturn(true);
            misc.when(() -> Misc.connectToProxyLobby(player)).thenReturn(false);
            var event = new PlayerPortalEvent(player, new Location(null, 0, 0, 0), new Location(null, 0, 0, 0),
                    PlayerTeleportEvent.TeleportCause.END_PORTAL);
            new LobbyPortalListener().onPortal(event);
            assertTrue(event.isCancelled());
            misc.verify(() -> Misc.connectToProxyLobby(player));
        }
    }

    @Test
    void lobbyNetherAndEndPortalsReturnToProxyLobby() {
        assertTrue(LobbyPortalListener.shouldReturnToProxyLobby(
                true, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL));
        assertFalse(LobbyPortalListener.shouldReturnToProxyLobby(
                false, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL));
        assertTrue(LobbyPortalListener.shouldReturnToProxyLobby(
                true, PlayerTeleportEvent.TeleportCause.END_PORTAL));
    }

    @Test
    void proxyContextWithoutLocalLobbyStillReturnsFromNetherPortal() {
        assertTrue(LobbyPortalListener.shouldReturnToProxyLobby(
                false, true, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL));
        assertTrue(LobbyPortalListener.shouldReturnToProxyLobby(
                false, true, PlayerTeleportEvent.TeleportCause.END_PORTAL));
    }
}
