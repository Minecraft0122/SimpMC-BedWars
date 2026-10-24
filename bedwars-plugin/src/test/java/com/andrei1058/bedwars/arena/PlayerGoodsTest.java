package com.andrei1058.bedwars.arena;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class PlayerGoodsTest {

    @Test
    void offlineRestoreReleasesVaultWhenNoReconnectIsPending() {
        UUID playerId = UUID.randomUUID();
        Player player = createPlayer(playerId);
        PlayerGoods goods = new PlayerGoods(player, false);
        assertTrue(PlayerGoods.hasGoods(player));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ReJoin> rejoin = mockStatic(ReJoin.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(null);
            rejoin.when(() -> ReJoin.hasPendingForPlayer(playerId)).thenReturn(false);
            goods.restore();
        }

        assertFalse(PlayerGoods.hasGoods(player));
    }

    @Test
    void offlineRestoreKeepsVaultWhileReconnectIsPending() {
        UUID playerId = UUID.randomUUID();
        Player player = createPlayer(playerId);
        PlayerGoods goods = new PlayerGoods(player, false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ReJoin> rejoin = mockStatic(ReJoin.class)) {
            bukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(null);
            rejoin.when(() -> ReJoin.hasPendingForPlayer(playerId)).thenReturn(true);
            goods.restore();
        }

        assertTrue(PlayerGoods.hasGoods(player));
        PlayerGoods.discard(playerId);
        assertFalse(PlayerGoods.hasGoods(player));
    }

    private static Player createPlayer(UUID playerId) {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        Inventory enderChest = mock(Inventory.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getEnderChest()).thenReturn(enderChest);
        when(inventory.iterator()).thenReturn(List.<ItemStack>of().listIterator());
        when(enderChest.iterator()).thenReturn(List.<ItemStack>of().listIterator());
        when(player.getActivePotionEffects()).thenReturn(List.of());
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        return player;
    }
}
