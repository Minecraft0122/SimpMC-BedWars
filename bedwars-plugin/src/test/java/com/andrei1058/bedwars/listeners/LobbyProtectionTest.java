package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.server.ServerType;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyProtectionTest {

    @Test
    void blocksDropsInMultiArenaAndSharedLobbies() {
        assertTrue(LobbyProtection.shouldProtectLobbyDrop(ServerType.MULTIARENA, "Lobby", "lobby"));
        assertTrue(LobbyProtection.shouldProtectLobbyDrop(ServerType.SHARED, "world", "world"));
    }

    @Test
    void doesNotTreatArenaOrProxyWorldAsLobby() {
        assertFalse(LobbyProtection.shouldProtectLobbyDrop(ServerType.MULTIARENA, "arena", "lobby"));
        assertFalse(LobbyProtection.shouldProtectLobbyDrop(ServerType.BUNGEE, "lobby", "lobby"));
        assertFalse(LobbyProtection.shouldProtectLobbyDrop(ServerType.MULTIARENA, "world", ""));
    }

    @Test
    void recognizesEveryInventoryDropAction() {
        assertTrue(LobbyProtection.isInventoryDropAction(InventoryAction.DROP_ALL_CURSOR));
        assertTrue(LobbyProtection.isInventoryDropAction(InventoryAction.DROP_ONE_CURSOR));
        assertTrue(LobbyProtection.isInventoryDropAction(InventoryAction.DROP_ALL_SLOT));
        assertTrue(LobbyProtection.isInventoryDropAction(InventoryAction.DROP_ONE_SLOT));
        assertFalse(LobbyProtection.isInventoryDropAction(InventoryAction.PICKUP_ALL));
    }
}
