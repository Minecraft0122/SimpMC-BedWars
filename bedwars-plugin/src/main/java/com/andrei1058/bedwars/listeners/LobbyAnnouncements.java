package com.andrei1058.bedwars.listeners;

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

    static boolean isLobbyContext(ServerType serverType, String playerWorld, String lobbyWorld,
                                  boolean inArena, boolean inSetup) {
        return serverType != ServerType.BUNGEE && !inArena && !inSetup
                && playerWorld != null && lobbyWorld != null && !lobbyWorld.isBlank()
                && playerWorld.equalsIgnoreCase(lobbyWorld);
    }

    public static void playerEntered(Player player) {
        if (!isLobbyPlayer(player) || !PRESENT.add(player.getUniqueId())) return;
        broadcast("§e[BW] §f玩家 §b" + player.getName() + " §f加入了游戏", null);
    }

    public static void playerEnteredArena(Player player) {
        if (player != null) PRESENT.remove(player.getUniqueId());
    }

    public static void playerQuit(Player player, boolean wasInLobby) {
        if (player == null) return;
        boolean wasTracked = PRESENT.remove(player.getUniqueId());
        if (wasInLobby || wasTracked) {
            broadcast("§e[BW] §f玩家 §b" + player.getName() + " §f离开了游戏", player);
        }
    }

    private static void broadcast(String message, Player excluded) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(excluded) || !isLobbyPlayer(viewer)) continue;
            viewer.sendMessage(message);
        }
    }
}
