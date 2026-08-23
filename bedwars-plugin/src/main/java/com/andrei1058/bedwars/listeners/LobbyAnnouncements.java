package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.SetupSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Sends lobby-scoped presence messages without leaking them into arenas. */
public final class LobbyAnnouncements {

    private static final Set<UUID> PRESENT = new HashSet<>();

    private LobbyAnnouncements() {
    }

    public static boolean isLobbyPlayer(Player player) {
        if (player == null || player.getWorld() == null) return false;
        return isLobbyContext(BedWars.getServerType(), player.getWorld().getName(), BedWars.getLobbyWorld(),
                Arena.isInArena(player), SetupSession.isInSetupSession(player.getUniqueId()));
    }

    /**
     * Returns whether a player is in a context where a leave action should go
     * to the proxy's group lobby. BUNGEE mode has no local lobby world, so the
     * regular lobby-world predicate intentionally cannot be reused there.
     */
    public static boolean isProxyLobbyPlayer(Player player) {
        if (player == null || player.getWorld() == null) return false;
        boolean inArena = Arena.isInArena(player);
        boolean inSetup = SetupSession.isInSetupSession(player.getUniqueId());
        if (inArena || inSetup) return false;
        return BedWars.getServerType() == ServerType.BUNGEE
                || isLobbyPlayer(player)
                // Match the fallback used by lobby protection and join
                // handling when lobbyLoc is absent or its world is unloaded.
                || LobbyProtection.isLobbyWorld(player);
    }

    static boolean isLobbyContext(ServerType serverType, String playerWorld, String lobbyWorld,
                                  boolean inArena, boolean inSetup) {
        return serverType != ServerType.BUNGEE && !inArena && !inSetup
                && playerWorld != null && lobbyWorld != null && !lobbyWorld.isBlank()
                && playerWorld.equalsIgnoreCase(lobbyWorld);
    }

    public static void playerEntered(Player player) {
        if (!isLobbyPlayer(player) || !beginLobbyPresence(player.getUniqueId())) return;
        broadcast("§e[BW] §f玩家 §b" + player.getName() + " §f加入了游戏", null);
    }

    /** Remove stale lobby presence whenever a player enters an arena, setup world or another world. */
    public static void playerLeftLobby(Player player) {
        if (player != null) endLobbyPresence(player.getUniqueId());
    }

    public static void playerEnteredArena(Player player) {
        playerLeftLobby(player);
    }

    public static void playerQuit(Player player, boolean wasInLobby) {
        if (player == null) return;
        endLobbyPresence(player.getUniqueId());
        if (shouldAnnounceQuit(wasInLobby)) {
            broadcast("§e[BW] §f玩家 §b" + player.getName() + " §f离开了游戏", player);
        }
    }

    static boolean shouldAnnounceQuit(boolean wasInLobby) {
        return wasInLobby;
    }

    static boolean beginLobbyPresence(UUID playerId) {
        return playerId != null && PRESENT.add(playerId);
    }

    static boolean endLobbyPresence(UUID playerId) {
        return playerId != null && PRESENT.remove(playerId);
    }

    private static void broadcast(String message, Player excluded) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(excluded) || !isLobbyPlayer(viewer)) continue;
            AdventureText.send(viewer, message);
        }
    }
}
