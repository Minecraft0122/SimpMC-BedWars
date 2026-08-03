package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.server.ServerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedWarsWorldEnvironmentTest {

    @Test
    void dedicatedModesForceEveryLoadedWorldToBrightNoon() {
        assertTrue(shouldForce(ServerType.MULTIARENA, "lobby", "", false, false, false));
        assertTrue(shouldForce(ServerType.MULTIARENA, "arena-2", "lobby", false, false, false));
        assertTrue(shouldForce(ServerType.BUNGEE, "world", "", false, false, false));
    }

    @Test
    void sharedModeIncludesEveryBedWarsOwnedWorldKind() {
        assertTrue(shouldForce(ServerType.SHARED, "arena", "lobby", true, false, false));
        assertTrue(shouldForce(ServerType.SHARED, "restoring", "lobby", false, true, false));
        assertTrue(shouldForce(ServerType.SHARED, "setup", "lobby", false, false, true));
        assertTrue(shouldForce(ServerType.SHARED, "Lobby", "lobby", false, false, false));
    }

    @Test
    void sharedModeLeavesUnrelatedWorldsUntouched() {
        assertFalse(shouldForce(ServerType.SHARED, "survival", "lobby", false, false, false));
        assertFalse(shouldForce(ServerType.SHARED, "survival", "", false, false, false));
        assertFalse(shouldForce(null, "world", "world", true, true, true));
        assertFalse(shouldForce(ServerType.MULTIARENA, "", "world", true, true, true));
    }

    private static boolean shouldForce(ServerType serverType, String worldName, String lobbyWorldName,
                                       boolean arenaWorld, boolean queuedArenaWorld, boolean setupWorld) {
        return BedWarsWorldEnvironment.shouldForceBrightNoon(serverType, worldName, lobbyWorldName,
                arenaWorld, queuedArenaWorld, setupWorld);
    }
}
