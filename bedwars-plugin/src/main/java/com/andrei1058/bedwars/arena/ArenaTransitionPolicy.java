package com.andrei1058.bedwars.arena;

/** Guards delayed lobby work against a newer disconnect or arena assignment. */
final class ArenaTransitionPolicy {

    private ArenaTransitionPolicy() {
    }

    static boolean shouldApplyLobbyTransition(boolean disconnect, boolean online,
                                               boolean currentlyInArena) {
        return !disconnect && online && !currentlyInArena;
    }
}
