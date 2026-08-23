package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.arena.Misc;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Sends lobby nether-portal users back to the proxy network lobby. */
public final class LobbyPortalListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (!shouldReturnToProxyLobby(player, event.getCause())) return;

        // Send the proxy request while the portal event is still on the main
        // thread. If the channel/configuration is unavailable, preserve the
        // prior cancellation state so another protection plugin still wins.
        if (Misc.connectToProxyLobby(player)) event.setCancelled(true);
    }

    static boolean shouldReturnToProxyLobby(Player player, PlayerTeleportEvent.TeleportCause cause) {
        return player != null && shouldReturnToProxyLobby(
                LobbyAnnouncements.isLobbyPlayer(player),
                LobbyAnnouncements.isProxyLobbyPlayer(player), cause);
    }

    static boolean shouldReturnToProxyLobby(boolean lobbyPlayer, PlayerTeleportEvent.TeleportCause cause) {
        return shouldReturnToProxyLobby(lobbyPlayer, false, cause);
    }

    static boolean shouldReturnToProxyLobby(boolean lobbyPlayer, boolean proxyServer,
                                            PlayerTeleportEvent.TeleportCause cause) {
        return (lobbyPlayer || proxyServer) && cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL;
    }
}
