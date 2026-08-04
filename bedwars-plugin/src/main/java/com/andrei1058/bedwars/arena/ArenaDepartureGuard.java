package com.andrei1058.bedwars.arena;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** Keeps arena departure de-duplication scoped to a player's current lifecycle. */
final class ArenaDepartureGuard {

    private ArenaDepartureGuard() {
    }

    static boolean tryBegin(List<Player> leaving, Player player) {
        UUID playerId = player.getUniqueId();
        if (contains(leaving, playerId)) return false;
        leaving.add(player);
        return true;
    }

    static void restore(List<Player> leaving, Player player) {
        UUID playerId = player.getUniqueId();
        leaving.removeIf(candidate -> candidate != null
                && playerId.equals(candidate.getUniqueId()));
    }

    static boolean contains(List<Player> leaving, Player player) {
        return contains(leaving, player.getUniqueId());
    }

    private static boolean contains(List<Player> leaving, UUID playerId) {
        for (Player candidate : leaving) {
            if (candidate != null && playerId.equals(candidate.getUniqueId())) return true;
        }
        return false;
    }
}
