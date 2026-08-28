/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena.team;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Player-selectable pre-game colors were removed. This class is
 * retained as an inert binary-compatibility bridge for extensions that
 * incorrectly referenced plugin implementation classes instead of the API.
 */
@Deprecated
public final class PreGameTeamSelectionManager implements Listener {

    public enum Result {
        SELECTED,
        CLEARED,
        NOT_IN_PRE_GAME,
        DIFFERENT_ARENA,
        TEAM_FULL
    }

    private static final PreGameTeamSelectionManager INSTANCE = new PreGameTeamSelectionManager();

    private PreGameTeamSelectionManager() {
    }

    public static PreGameTeamSelectionManager getInstance() {
        return INSTANCE;
    }

    /** Selection is no longer supported and never changes arena state. */
    public Result select(@NotNull Player player, @NotNull ITeam team) {
        return Result.NOT_IN_PRE_GAME;
    }

    public Result clear(@NotNull Player player) {
        return Result.CLEARED;
    }

    public @Nullable ITeam getSelection(@NotNull IArena arena, @NotNull Player player) {
        return null;
    }

    public int selectedCount(@NotNull ITeam team) {
        return 0;
    }

    public void clearArena(@NotNull IArena arena) {
    }

    @EventHandler
    public void onPlayerLeave(PlayerLeaveArenaEvent event) {
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    public void onArenaDisable(ArenaDisableEvent event) {
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
    }
}
