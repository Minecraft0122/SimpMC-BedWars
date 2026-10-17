package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.arena.Misc;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.PortalType;

/** 大厅传送门使用代理返回通道，不依赖本服加载末地。 */
public final class LobbyPortalListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEndPortalEnter(EntityPortalEnterEvent event) {
        if (event.getPortalType() != PortalType.ENDER || !(event.getEntity() instanceof Player player)
                || !shouldReturnToProxyLobby(player, PlayerTeleportEvent.TeleportCause.END_PORTAL)) return;
        // 大厅玩家不进入原版末地流程：接触事件先无条件取消，再提交代理返回。
        // 即便代理不可用或 allow-end=true，玩家也不会被送进末地或卡在门内。
        event.setCancelled(true);
        Misc.connectToProxyLobby(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (!shouldReturnToProxyLobby(player, event.getCause())) return;

        // 原版下界/末地跳转对大厅玩家一律取消；代理连接失败也必须留在本服。
        // 提示由代理连接器的统一入口负责，失败时不会执行任何原版传送。
        event.setCancelled(true);
        Misc.connectToProxyLobby(player);
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
        return (lobbyPlayer || proxyServer)
                && (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_PORTAL);
    }
}
