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
    private volatile Map<UUID, ITeam> gameStartTeams = Map.of();

    void capture(List<ITeam> teams) {
        activeTeamIds.clear();
        Map<UUID, Integer> sizes = new LinkedHashMap<>();
        Map<UUID, ITeam> playerTeams = new LinkedHashMap<>();
        for (ITeam team : teams) {
            int size = team.getMembers().size();
            if (size <= 0) continue;
            activeTeamIds.add(team.getIdentity());
            sizes.put(team.getIdentity(), size);
            team.getMembers().forEach(player -> playerTeams.put(player.getUniqueId(), team));
        }
        gameStartSizes = Map.copyOf(sizes);
        gameStartTeams = Map.copyOf(playerTeams);
    }

    List<ITeam> activeTeams(List<ITeam> configuredTeams) {
        return configuredTeams.stream()
                .filter(team -> activeTeamIds.contains(team.getIdentity()))
                .toList();
    }

    int gameStartSize(ITeam team) {
        return team == null ? 0 : gameStartSizes.getOrDefault(team.getIdentity(), 0);
    }

    ITeam gameStartTeam(UUID playerId) {
        return playerId == null ? null : gameStartTeams.get(playerId);
    }

    void reset() {
        activeTeamIds.clear();
        gameStartSizes = Map.of();
        gameStartTeams = Map.of();
    }
}
