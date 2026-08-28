package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.CommandItemAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents non-operator lobby players from changing inventories or using
 * vanilla items. Operators and explicit lobby build sessions remain usable.
 */
public final class LobbyProtection implements Listener {

    private static final Set<UUID> pendingInventoryRefresh = ConcurrentHashMap.newKeySet();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && (isProtected(player)
                || (shouldProtectLobbyInventory(player.isOp(), isLobbyWorld(player))
                && isInventoryDropAction(event.getAction())))) {
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
        if (isProtected(event.getPlayer()) && !isCommandItemClick(event.getAction(), event.getItem())) {
            event.setCancelled(true);
        }
    }

    /**
     * Command items are not build interactions. They must reach
     * {@link Interact#onItemCommand(PlayerInteractEvent)} even though the
     * rest of the lobby is protected.
     */
    static boolean isCommandItemClick(Action action, ItemStack item) {
        return shouldAllowCommandItem(action, CommandItemAction.isCommandItem(item));
    }

    static boolean shouldAllowCommandItem(Action action, boolean commandItem) {
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;
        return commandItem;
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
        Player player = event.getPlayer();
        if (shouldProtectLobbyInventory(player.isOp(), isLobbyWorld(player))) {
            event.setCancelled(true);
            // Paper normally restores a cancelled drop immediately. Refresh on
            // the next tick as well so Q/Ctrl+Q cannot leave a client-side gap.
            if (pendingInventoryRefresh.add(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                    pendingInventoryRefresh.remove(player.getUniqueId());
                    if (player.isOnline()
                            && shouldProtectLobbyInventory(player.isOp(), isLobbyWorld(player))) {
                        player.updateInventory();
                    }
                });
            }
        }
    }

    private static boolean isProtected(Player player) {
        return shouldProtectLobbyInventory(player.isOp(), isLobbyWorld(player))
                && !BreakPlace.isBuildSession(player);
    }

    static boolean isLobbyWorld(Player player) {
        String playerWorld = player.getWorld().getName();
        return shouldProtectLobbyDrop(BedWars.getServerType(), playerWorld, BedWars.getLobbyWorld(),
                Arena.getArenaByPlayer(player) != null, Arena.getArenaByIdentifier(playerWorld) != null);
    }

    static boolean shouldProtectLobbyDrop(ServerType serverType, String playerWorld, String lobbyWorld,
                                          boolean assignedToArena, boolean arenaWorld) {
        if (serverType != ServerType.MULTIARENA && serverType != ServerType.SHARED) return false;
        if (playerWorld == null || playerWorld.isBlank()) return false;
        if (lobbyWorld != null && !lobbyWorld.isBlank() && playerWorld.equalsIgnoreCase(lobbyWorld)) return true;
        // Join handling falls back to the server's first world if lobbyLoc is
        // missing or temporarily unavailable. Treat any non-arena player in a
        // non-arena world as a lobby player so that fallback remains protected.
        return !assignedToArena && !arenaWorld;
    }

    static boolean isInventoryDropAction(InventoryAction action) {
        return action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT;
    }

    static boolean shouldProtectLobbyInventory(boolean operator, boolean lobbyWorld) {
        return lobbyWorld && !operator;
    }
}
