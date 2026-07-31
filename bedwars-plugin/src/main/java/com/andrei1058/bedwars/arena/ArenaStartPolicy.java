/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena;

import java.util.Collection;

/**
 * Central start requirements shared by countdown eligibility and the final
 * assignment check. {@code minPlayers} is an arena-wide threshold, while
 * {@code maxInTeam} is the capacity of one configured team.
 */
public final class ArenaStartPolicy {

    public static final int ABSOLUTE_MINIMUM_PLAYERS = 2;

    private ArenaStartPolicy() {
    }

    public static boolean hasEnoughPlayers(int playerCount, int minimumPlayers,
                                           int configuredTeamCount, int maximumInTeam) {
        if (configuredTeamCount < 2 || maximumInTeam < 1) return false;
        long arenaCapacity = (long) configuredTeamCount * maximumInTeam;
        return playerCount >= Math.max(ABSOLUTE_MINIMUM_PLAYERS, minimumPlayers)
                && playerCount <= arenaCapacity;
    }

    public static boolean hasEnoughActiveTeams(long activeTeamCount) {
        return activeTeamCount >= 2;
    }

    public static boolean canStartWithActiveTeams(long activeTeamCount, boolean allowSingleTeamDebugStart) {
        return hasEnoughActiveTeams(activeTeamCount)
                || (allowSingleTeamDebugStart && activeTeamCount == 1);
    }

    public static boolean canStartWithTeamSizes(Collection<Integer> teamSizes, int minimumPlayers,
                                                int maximumInTeam, boolean allowSingleTeamDebugStart) {
        if (teamSizes == null || maximumInTeam < 1) return false;

        long activeTeams = 0;
        long playerCount = 0;
        for (Integer size : teamSizes) {
            if (size == null || size <= 0) continue;
            if (size > maximumInTeam) return false;
            activeTeams++;
            playerCount += size;
        }

        if (!canStartWithActiveTeams(activeTeams, allowSingleTeamDebugStart)) return false;
        return allowSingleTeamDebugStart
                || playerCount >= Math.max(ABSOLUTE_MINIMUM_PLAYERS, minimumPlayers);
    }
}
