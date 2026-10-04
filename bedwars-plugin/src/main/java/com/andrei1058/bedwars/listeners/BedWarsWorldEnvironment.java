package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.GameRules;
import com.andrei1058.bedwars.arena.SetupSession;
import org.bukkit.World;
import org.bukkit.Bukkit;

/** Selects the worlds whose daylight and weather belong to BedWars. */
public final class BedWarsWorldEnvironment {

    private BedWarsWorldEnvironment() {
    }

    /** Apply the bright-noon invariant when BedWars owns this world's environment. */
    public static void enforceBrightNoon(World world) {
        if (shouldForceBrightNoon(world)) GameRules.enforceBrightNoon(world);
    }

    public static void enforceLobbyEnvironment(World world) {
        if (isLobbyManagedWorld(world)) GameRules.enforceLobbyEnvironment(world);
    }

    static boolean isLobbyManagedWorld(World world) {
        if (world == null || isArenaManagedWorld(world)) return false;
        String configured = BedWars.getLobbyWorld();
        boolean lobbyLoaded = configured != null && !configured.isBlank() && Bukkit.getWorld(configured) != null;
        boolean firstWorld = !Bukkit.getWorlds().isEmpty() && Bukkit.getWorlds().getFirst().equals(world);
        return isLobbyManagedWorld(BedWars.getServerType(), BedWars.isBungeeLobby(),
                world.getName(), configured, lobbyLoaded, firstWorld);
    }

    static boolean isLobbyManagedWorld(ServerType mode, boolean dedicatedLobby, String worldName,
                                        String configured, boolean lobbyLoaded, boolean firstWorld) {
        if (mode == null || worldName == null || worldName.isBlank()) return false;
        if (mode == ServerType.BUNGEE) return dedicatedLobby;
        if (sameWorld(worldName, configured)) return true;
        // MULTIARENA actually sends joins to the first world if the configured
        // lobby is unavailable. SHARED must leave unrelated survival worlds alone.
        return mode == ServerType.MULTIARENA && !lobbyLoaded && firstWorld;
    }

    static boolean shouldForceBrightNoon(World world) {
        if (world == null) return false;
        String worldName = world.getName();
        return shouldForceBrightNoon(BedWars.getServerType(), worldName, BedWars.getLobbyWorld(),
                Arena.getArenaByIdentifier(worldName) != null,
                isQueuedArenaWorld(worldName), SetupSession.isSetupWorld(worldName));
    }

    static boolean isArenaManagedWorld(World world) {
        if (world == null) return false;
        String worldName = world.getName();
        return Arena.getArenaByIdentifier(worldName) != null
                || isQueuedArenaWorld(worldName)
                || SetupSession.isSetupWorld(worldName);
    }

    static boolean shouldForceBrightNoon(ServerType serverType, String worldName, String lobbyWorldName,
                                         boolean arenaWorld, boolean queuedArenaWorld, boolean setupWorld) {
        if (serverType == null || worldName == null || worldName.isBlank()) return false;
        if (serverType == ServerType.MULTIARENA || serverType == ServerType.BUNGEE) return true;
        return arenaWorld || queuedArenaWorld || setupWorld || sameWorld(worldName, lobbyWorldName);
    }

    private static boolean isQueuedArenaWorld(String worldName) {
        return Arena.getEnableQueue().stream()
                .anyMatch(arena -> arena.getWorldName().equalsIgnoreCase(worldName));
    }

    private static boolean sameWorld(String first, String second) {
        return second != null && !second.isBlank() && first.equalsIgnoreCase(second);
    }
}
