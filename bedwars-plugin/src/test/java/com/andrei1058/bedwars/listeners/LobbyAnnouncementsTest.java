package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.server.ServerType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyAnnouncementsTest {

    @Test
    void announcesAgainAfterLeavingAndReenteringLobby() {
        UUID playerId = UUID.randomUUID();

        assertTrue(LobbyAnnouncements.beginLobbyPresence(playerId));
        assertFalse(LobbyAnnouncements.beginLobbyPresence(playerId));
        assertTrue(LobbyAnnouncements.endLobbyPresence(playerId));
        assertTrue(LobbyAnnouncements.beginLobbyPresence(playerId));
        assertTrue(LobbyAnnouncements.endLobbyPresence(playerId));
    }

    @Test
    void onlyTreatsUnassignedPlayersInTheConfiguredLobbyAsLobbyAudience() {
        assertTrue(LobbyAnnouncements.isLobbyContext(ServerType.MULTIARENA,
                "Lobby", "lobby", false, false));
        assertTrue(LobbyAnnouncements.isLobbyContext(ServerType.SHARED,
                "world", "world", false, false));
        assertFalse(LobbyAnnouncements.isLobbyContext(ServerType.MULTIARENA,
                "lobby", "lobby", true, false));
        assertFalse(LobbyAnnouncements.isLobbyContext(ServerType.MULTIARENA,
                "lobby", "lobby", false, true));
        assertFalse(LobbyAnnouncements.isLobbyContext(ServerType.BUNGEE,
                "lobby", "lobby", false, false));
    }

    @Test
    void recognizesFallbackLobbyOnlyWhenConfiguredWorldIsUnavailable() {
        assertTrue(LobbyAnnouncements.isFallbackLobbyContext(
                "world", "lobby", false, false, false, false));
        assertTrue(LobbyAnnouncements.isFallbackLobbyContext(
                "world", "", false, false, false, false));
        assertFalse(LobbyAnnouncements.isFallbackLobbyContext(
                "world", "lobby", true, false, false, false));
        assertFalse(LobbyAnnouncements.isFallbackLobbyContext(
                "arena-solo", "lobby", false, false, false, true));
        assertFalse(LobbyAnnouncements.isFallbackLobbyContext(
                "world", "lobby", false, true, false, false));
        assertFalse(LobbyAnnouncements.isFallbackLobbyContext(
                "world", "lobby", false, false, true, false));
    }

    @Test
    void onlyAPlayerWhoActuallyQuitFromTheLobbyIsAnnounced() {
        assertTrue(LobbyAnnouncements.shouldAnnounceQuit(true));
        assertFalse(LobbyAnnouncements.shouldAnnounceQuit(false));
    }
}
