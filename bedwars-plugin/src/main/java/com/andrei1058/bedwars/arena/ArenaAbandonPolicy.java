package com.andrei1058.bedwars.arena;

/** Decides whether removing one reconnect reservation eliminates its team. */
final class ArenaAbandonPolicy {

    private ArenaAbandonPolicy() {
    }

    static boolean eliminatesTeam(int activeMembers, boolean hasOtherPendingTeammate) {
        return activeMembers == 0 && !hasOtherPendingTeammate;
    }
}
