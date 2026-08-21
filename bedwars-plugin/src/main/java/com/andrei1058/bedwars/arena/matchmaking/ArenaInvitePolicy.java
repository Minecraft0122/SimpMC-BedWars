/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena.matchmaking;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;

/** Pure validation rules shared by arena invite commands and transitions. */
public final class ArenaInvitePolicy {

    private ArenaInvitePolicy() {
    }

    public static boolean isPreGame(IArena arena) {
        return arena != null && (arena.getStatus() == GameState.waiting
                || arena.getStatus() == GameState.starting);
    }

    /**
     * A countdown at one second (or a missing task during a transition) must
     * not accept a new player even though the status is still {@code starting}.
     */
    public static boolean canAcceptPlayer(IArena arena) {
        if (arena == null) return false;
        if (arena.getStatus() == GameState.waiting) return true;
        if (arena.getStatus() != GameState.starting || arena.getStartingTask() == null) return false;
        return arena.getStartingTask().getCountdown() > 1;
    }

    public static boolean hasRoom(IArena arena) {
        return arena != null && arena.getPlayers().size() < arena.getMaxPlayers();
    }

    /**
     * Invite targets are either in the proxy/server lobby or in a different,
     * still joinable pre-game arena. A player already in the inviter's arena
     * must not receive a self-transfer invitation.
     */
    public static boolean canInviteTarget(IArena inviterArena, IArena targetArena,
                                          boolean targetIsLobby) {
        if (!canAcceptPlayer(inviterArena) || !hasRoom(inviterArena)) return false;
        if (targetIsLobby) return targetArena == null;
        return targetArena != null && targetArena != inviterArena
                && canAcceptPlayer(targetArena);
    }

    public static boolean canAcceptTarget(IArena inviterArena, IArena targetArena,
                                          boolean targetIsLobby) {
        return canAcceptPlayer(inviterArena) && hasRoom(inviterArena)
                && (targetIsLobby ? targetArena == null
                : targetArena != null && targetArena != inviterArena && canAcceptPlayer(targetArena));
    }

    public static boolean canAcceptFrom(IArena inviterArena, IArena targetArena,
                                        boolean targetIsLobby) {
        return canAcceptTarget(inviterArena, targetArena, targetIsLobby);
    }
}
