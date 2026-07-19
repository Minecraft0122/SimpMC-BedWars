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

/**
 * Central start requirements shared by the waiting and starting phases.
 * Arena configuration controls team capacity, but not the minimum number of
 * opponents: every round always requires at least two players and two teams.
 */
public final class ArenaStartPolicy {

    public static final int MINIMUM_PLAYERS = 2;

    private ArenaStartPolicy() {
    }

    public static boolean hasEnoughPlayers(int playerCount) {
        return playerCount >= MINIMUM_PLAYERS;
    }

    public static boolean hasEnoughActiveTeams(long activeTeamCount) {
        return activeTeamCount >= 2;
    }
}
