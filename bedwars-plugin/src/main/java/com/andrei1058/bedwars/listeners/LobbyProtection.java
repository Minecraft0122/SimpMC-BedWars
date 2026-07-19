package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.server.ServerType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents lobby players from changing inventories or using vanilla items.
 * Explicit lobby build sessions remain available to administrators.
 */
public final class LobbyProtection implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && (isProtected(player) || (isLobbyWorld(player) && isInventoryDropAction(event.getAction())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && isProtected(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftPreview(PrepareItemCraftEvent event) {
        if (event.getView().getPlayer() instanceof Player player && isProtected(player)) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (isProtected(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (isProtected(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (isProtected(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        // Dropping is never part of a lobby build session. Do not reuse the
        // general interaction exception here or Q/Ctrl+Q can bypass protection.
        if (isLobbyWorld(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private static boolean isProtected(Player player) {
        return isLobbyWorld(player)
                && !BreakPlace.isBuildSession(player);
    }

    private static boolean isLobbyWorld(Player player) {
        return shouldProtectLobbyDrop(BedWars.getServerType(), player.getWorld().getName(), BedWars.getLobbyWorld());
    }

    static boolean shouldProtectLobbyDrop(ServerType serverType, String playerWorld, String lobbyWorld) {
        return (serverType == ServerType.MULTIARENA || serverType == ServerType.SHARED)
                && playerWorld != null && lobbyWorld != null && !lobbyWorld.isBlank()
                && playerWorld.equalsIgnoreCase(lobbyWorld);
    }

    static boolean isInventoryDropAction(InventoryAction action) {
        return action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT;
    }
}
