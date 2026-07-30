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
 * Central start requirements shared by the waiting and starting phases.
 * Arena configuration controls both team capacity and the minimum population
 * of every active team. Normal rounds always require at least two teams.
 * A deliberate administrator debug start may use one active team, but never zero.
 */
public final class ArenaStartPolicy {

    public static final int MINIMUM_PLAYERS = 2;

    private ArenaStartPolicy() {
    }

    public static boolean hasEnoughPlayers(int playerCount) {
        return playerCount >= MINIMUM_PLAYERS;
    }

    public static boolean hasEnoughPlayers(int playerCount, int configuredTeamCount,
                                           int minimumInTeam, int maximumInTeam) {
        return maximumFeasibleActiveTeams(playerCount, configuredTeamCount,
                minimumInTeam, maximumInTeam) >= 2;
    }

    public static int maximumFeasibleActiveTeams(int playerCount, int configuredTeamCount,
                                                  int minimumInTeam, int maximumInTeam) {
        if (playerCount < 1 || configuredTeamCount < 1 || minimumInTeam < 1
                || maximumInTeam < minimumInTeam) {
            return 0;
        }
        int maximumByMinimum = playerCount / minimumInTeam;
        int minimumByCapacity = divideRoundingUp(playerCount, maximumInTeam);
        int maximum = Math.min(configuredTeamCount, maximumByMinimum);
        return maximum >= Math.max(2, minimumByCapacity) ? maximum : 0;
    }

    public static boolean isFeasibleActiveTeamCount(int playerCount, int activeTeamCount,
                                                     int minimumInTeam, int maximumInTeam) {
        return activeTeamCount >= 2 && minimumInTeam >= 1 && maximumInTeam >= minimumInTeam
                && playerCount >= (long) activeTeamCount * minimumInTeam
                && playerCount <= (long) activeTeamCount * maximumInTeam;
    }

    public static boolean hasEnoughActiveTeams(long activeTeamCount) {
        return activeTeamCount >= 2;
    }

    public static boolean canStartWithActiveTeams(long activeTeamCount, boolean allowSingleTeamDebugStart) {
        return hasEnoughActiveTeams(activeTeamCount)
                || (allowSingleTeamDebugStart && activeTeamCount == 1);
    }

    public static boolean canStartWithTeamSizes(Collection<Integer> teamSizes, int minimumInTeam,
                                                boolean allowSingleTeamDebugStart) {
        return canStartWithTeamSizes(teamSizes, minimumInTeam, Integer.MAX_VALUE,
                allowSingleTeamDebugStart);
    }

    public static boolean canStartWithTeamSizes(Collection<Integer> teamSizes, int minimumInTeam,
                                                int maximumInTeam, boolean allowSingleTeamDebugStart) {
        if (teamSizes == null) return false;
        long activeTeams = teamSizes.stream().filter(size -> size != null && size > 0).count();
        if (maximumInTeam < 1 || !canStartWithActiveTeams(activeTeams, allowSingleTeamDebugStart)) return false;
        return (allowSingleTeamDebugStart || minimumInTeam >= 1) && teamSizes.stream()
                .filter(size -> size != null && size > 0)
                .allMatch(size -> size <= maximumInTeam
                        && (allowSingleTeamDebugStart || size >= minimumInTeam));
    }

    public static int debugActiveTeamCount(int playerCount, int configuredTeamCount, int maximumInTeam) {
        if (playerCount < 1 || configuredTeamCount < 1 || maximumInTeam < 1) return 0;
        int required = divideRoundingUp(playerCount, maximumInTeam);
        return required <= configuredTeamCount ? Math.max(1, required) : 0;
    }

    private static int divideRoundingUp(int value, int divisor) {
        return value / divisor + (value % divisor == 0 ? 0 : 1);
    }
}
