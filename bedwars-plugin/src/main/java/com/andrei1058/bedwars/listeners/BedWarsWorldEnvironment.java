package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.GameRules;
import com.andrei1058.bedwars.arena.SetupSession;
import org.bukkit.World;

/** Selects the worlds whose daylight and weather belong to BedWars. */
public final class BedWarsWorldEnvironment {

    private BedWarsWorldEnvironment() {
    }

    /** Apply the bright-noon invariant when BedWars owns this world's environment. */
    public static void enforceBrightNoon(World world) {
        if (shouldForceBrightNoon(world)) GameRules.enforceBrightNoon(world);
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
