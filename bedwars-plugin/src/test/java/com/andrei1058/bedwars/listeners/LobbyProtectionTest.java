package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.CommandItemAction;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyProtectionTest {

    @Test
    void blocksDropsInMultiArenaAndSharedLobbies() {
        assertTrue(LobbyProtection.shouldProtectLobbyDrop(ServerType.MULTIARENA, "Lobby", "lobby", true, true));
        assertTrue(LobbyProtection.shouldProtectLobbyDrop(ServerType.SHARED, "world", "world", false, false));
    }

    @Test
    void doesNotTreatArenaOrProxyWorldAsLobby() {
        assertFalse(LobbyProtection.shouldProtectLobbyDrop(ServerType.MULTIARENA, "arena", "lobby", true, true));
        assertFalse(LobbyProtection.shouldProtectLobbyDrop(ServerType.BUNGEE, "lobby", "lobby", false, false));
    }

    @Test
    void protectsFallbackLobbyWhenConfiguredWorldIsMissing() {
        assertTrue(LobbyProtection.shouldProtectLobbyDrop(ServerType.MULTIARENA, "world", "", false, false));
        assertFalse(LobbyProtection.shouldProtectLobbyDrop(ServerType.MULTIARENA, "summer", "", false, true));
    }

    @Test
    void recognizesEveryInventoryDropAction() {
        assertTrue(LobbyProtection.isInventoryDropAction(InventoryAction.DROP_ALL_CURSOR));
        assertTrue(LobbyProtection.isInventoryDropAction(InventoryAction.DROP_ONE_CURSOR));
        assertTrue(LobbyProtection.isInventoryDropAction(InventoryAction.DROP_ALL_SLOT));
        assertTrue(LobbyProtection.isInventoryDropAction(InventoryAction.DROP_ONE_SLOT));
        assertFalse(LobbyProtection.isInventoryDropAction(InventoryAction.PICKUP_ALL));
    }

    @Test
    void protectsOnlyNonOperatorsInLobbyInventories() {
        assertTrue(LobbyProtection.shouldProtectLobbyInventory(false, true));
        assertFalse(LobbyProtection.shouldProtectLobbyInventory(true, true));
        assertFalse(LobbyProtection.shouldProtectLobbyInventory(false, false));
        assertFalse(LobbyProtection.shouldProtectLobbyInventory(true, false));
    }

    @Test
    void allowsOnlyRightClickOfTheTaggedProxyReturnItem() {
        assertTrue(LobbyProtection.isProxyLobbyTarget(Action.RIGHT_CLICK_AIR,
                CommandItemAction.Target.PROXY_LOBBY));
        assertTrue(LobbyProtection.isProxyLobbyTarget(Action.RIGHT_CLICK_BLOCK,
                CommandItemAction.Target.PROXY_LOBBY));
        assertFalse(LobbyProtection.isProxyLobbyTarget(Action.LEFT_CLICK_BLOCK,
                CommandItemAction.Target.PROXY_LOBBY));
        assertFalse(LobbyProtection.isProxyLobbyTarget(Action.RIGHT_CLICK_AIR,
                CommandItemAction.Target.ARENA_LOBBY));
        assertFalse(LobbyProtection.isProxyLobbyTarget(Action.RIGHT_CLICK_AIR, null));
    }
}
