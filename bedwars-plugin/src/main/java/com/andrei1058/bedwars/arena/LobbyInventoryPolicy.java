package com.andrei1058.bedwars.arena;

/** Guards lobby inventory writes against stale arena, setup, or world state. */
final class LobbyInventoryPolicy {

    private LobbyInventoryPolicy() {
    }

    static boolean shouldApply(boolean online, boolean inArena, boolean inSetup,
                               String playerWorld, String lobbyWorld) {
        return online && !inArena && !inSetup && playerWorld != null && lobbyWorld != null
                && !lobbyWorld.isBlank() && playerWorld.equalsIgnoreCase(lobbyWorld);
    }
}
