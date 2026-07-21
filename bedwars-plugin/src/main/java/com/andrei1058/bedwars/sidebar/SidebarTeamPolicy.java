package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Selects the configured teams that are relevant to the current sidebar. */
final class SidebarTeamPolicy {

    private SidebarTeamPolicy() {
    }

    static List<ITeam> displayedTeams(IArena arena) {
        GameState state = arena.getStatus();
        if (state == GameState.playing || state == GameState.restarting) {
            return arena.getActiveTeamsAtGameStart();
        }
        return arena.getTeams();
    }

    static boolean referencesHiddenTeam(String line, List<ITeam> configuredTeams, List<ITeam> displayedTeams) {
        if (line == null || !line.contains("{Team") || configuredTeams.size() == displayedTeams.size()) {
            return false;
        }

        Set<UUID> displayedIds = new HashSet<>();
        for (ITeam team : displayedTeams) displayedIds.add(team.getIdentity());
        for (ITeam team : configuredTeams) {
            if (!displayedIds.contains(team.getIdentity()) && line.contains("{Team" + team.getName())) {
                return true;
            }
        }
        return false;
    }
}
