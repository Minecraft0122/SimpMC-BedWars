package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.team.ITeam;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Keeps an immutable-by-identity snapshot of the teams that started a match. */
final class ArenaTeamParticipation {

    private final Set<UUID> activeTeamIds = new LinkedHashSet<>();

    void capture(List<ITeam> teams) {
        activeTeamIds.clear();
        for (ITeam team : teams) {
            if (!team.getMembers().isEmpty()) activeTeamIds.add(team.getIdentity());
        }
    }

    List<ITeam> activeTeams(List<ITeam> configuredTeams) {
        return configuredTeams.stream()
                .filter(team -> activeTeamIds.contains(team.getIdentity()))
                .toList();
    }

    void reset() {
        activeTeamIds.clear();
    }
}
