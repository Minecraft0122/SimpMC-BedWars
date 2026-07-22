package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.team.ITeam;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Keeps an immutable-by-identity snapshot of the teams that started a match. */
final class ArenaTeamParticipation {

    private final Set<UUID> activeTeamIds = new LinkedHashSet<>();
    private volatile Map<UUID, Integer> gameStartSizes = Map.of();

    void capture(List<ITeam> teams) {
        activeTeamIds.clear();
        Map<UUID, Integer> sizes = new LinkedHashMap<>();
        for (ITeam team : teams) {
            int size = team.getMembers().size();
            if (size <= 0) continue;
            activeTeamIds.add(team.getIdentity());
            sizes.put(team.getIdentity(), size);
        }
        gameStartSizes = Map.copyOf(sizes);
    }

    List<ITeam> activeTeams(List<ITeam> configuredTeams) {
        return configuredTeams.stream()
                .filter(team -> activeTeamIds.contains(team.getIdentity()))
                .toList();
    }

    int gameStartSize(ITeam team) {
        return team == null ? 0 : gameStartSizes.getOrDefault(team.getIdentity(), 0);
    }

    void reset() {
        activeTeamIds.clear();
        gameStartSizes = Map.of();
    }
}
