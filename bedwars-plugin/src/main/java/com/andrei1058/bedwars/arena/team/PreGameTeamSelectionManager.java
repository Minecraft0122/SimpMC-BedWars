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

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.sidebar.SidebarService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Stores best-effort, arena-local team choices made before a round starts. */
public final class PreGameTeamSelectionManager implements Listener {

    public enum Result {
        SELECTED,
        CLEARED,
        NOT_IN_PRE_GAME,
        DIFFERENT_ARENA,
        TEAM_FULL
    }

    private static final PreGameTeamSelectionManager INSTANCE = new PreGameTeamSelectionManager();

    private final Map<UUID, Selection> selections = new HashMap<>();

    private PreGameTeamSelectionManager() {
    }

    public static PreGameTeamSelectionManager getInstance() {
        return INSTANCE;
    }

    public Result select(@NotNull Player player, @NotNull ITeam team) {
        IArena arena = preGameArena(player);
        if (arena == null) return Result.NOT_IN_PRE_GAME;
        if (team.getArena() != arena || !arena.getTeams().contains(team)) return Result.DIFFERENT_ARENA;

        List<Player> assignmentGroup = assignmentGroup(player, arena);
        int alreadySelected = selectedCount(team, assignmentGroup);
        if (alreadySelected + assignmentGroup.size() > arena.getMaxInTeam()) return Result.TEAM_FULL;

        Selection selection = new Selection(arena, team.getIdentity());
        assignmentGroup.forEach(member -> selections.put(member.getUniqueId(), selection));
        refreshPlayerList(arena, assignmentGroup);
        return Result.SELECTED;
    }

    public Result clear(@NotNull Player player) {
        IArena arena = preGameArena(player);
        if (arena == null) return Result.NOT_IN_PRE_GAME;
        List<Player> assignmentGroup = assignmentGroup(player, arena);
        assignmentGroup.forEach(member -> selections.remove(member.getUniqueId()));
        refreshPlayerList(arena, assignmentGroup);
        return Result.CLEARED;
    }

    public @Nullable ITeam getSelection(@NotNull IArena arena, @NotNull Player player) {
        Selection selection = selections.get(player.getUniqueId());
        if (selection == null || selection.arena != arena) return null;
        return arena.getTeams().stream()
                .filter(team -> team.getIdentity().equals(selection.teamIdentity))
                .findFirst()
                .orElse(null);
    }

    public int selectedCount(@NotNull ITeam team) {
        return selectedCount(team, List.of());
    }

    public void clearArena(@NotNull IArena arena) {
        selections.entrySet().removeIf(entry -> entry.getValue().arena == arena);
    }

    @EventHandler
    public void onPlayerLeave(PlayerLeaveArenaEvent event) {
        selections.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        selections.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onArenaDisable(ArenaDisableEvent event) {
        selections.entrySet().removeIf(entry ->
                entry.getValue().arena.getWorldName().equals(event.getWorldName()));
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getNewState() == GameState.playing || event.getNewState() == GameState.restarting) {
            clearArena(event.getArena());
        }
    }

    private int selectedCount(ITeam team, List<Player> excluded) {
        Set<UUID> excludedPlayers = new HashSet<>();
        excluded.forEach(player -> excludedPlayers.add(player.getUniqueId()));
        return (int) selections.entrySet().stream()
                .filter(entry -> !excludedPlayers.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(selection -> selection.arena == team.getArena()
                        && selection.teamIdentity.equals(team.getIdentity()))
                .count();
    }

    private static List<Player> assignmentGroup(Player player, IArena arena) {
        return PreGameSquadManager.getInstance().getAssignmentGroups(arena).stream()
                .filter(group -> group.contains(player))
                .findFirst()
                .orElse(List.of(player));
    }

    private static IArena preGameArena(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.isPlayer(player)) return null;
        return arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting ? arena : null;
    }

    private static void refreshPlayerList(@NotNull IArena arena, @NotNull List<Player> players) {
        SidebarService sidebarService = SidebarService.getInstance();
        if (sidebarService != null) sidebarService.handlePreGameTeamSelection(arena, players);
    }

    private record Selection(IArena arena, UUID teamIdentity) {
    }
}
