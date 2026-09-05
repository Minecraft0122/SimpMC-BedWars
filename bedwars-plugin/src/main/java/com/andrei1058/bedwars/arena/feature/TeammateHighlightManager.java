/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.arena.feature;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.server.ArenaDisableEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the global Bukkit glowing flag used by {@code /bw highlight}.
 *
 * <p>Bukkit exposes glowing as an entity-wide flag rather than a
 * viewer-specific effect. Reference counting keeps two viewers from turning
 * off one another's highlight, and the original flag is restored when the
 * last BedWars viewer releases a target.</p>
 */
public final class TeammateHighlightManager implements Listener {

    private static final TeammateHighlightManager INSTANCE = new TeammateHighlightManager();

    private final Map<UUID, Set<UUID>> highlightedByViewer = new HashMap<>();
    private final Map<UUID, IArena> arenaByViewer = new HashMap<>();
    private final Map<UUID, Integer> ownersByTarget = new HashMap<>();
    private final Map<UUID, Boolean> originalGlowing = new HashMap<>();
    private final Map<UUID, Player> knownPlayers = new HashMap<>();

    private TeammateHighlightManager() {
    }

    public static TeammateHighlightManager getInstance() {
        return INSTANCE;
    }

    public enum Outcome {
        ENABLED,
        DISABLED,
        NO_TEAMMATES,
        NOT_IN_GAME
    }

    public record ToggleResult(Outcome outcome, List<Player> teammates) {
        public ToggleResult {
            teammates = teammates == null ? List.of() : List.copyOf(teammates);
        }
    }

    /** Toggle the viewer's current-game teammate highlights. */
    public synchronized ToggleResult toggle(Player viewer, IArena arena) {
        if (!isPlayableViewer(viewer, arena)) {
            if (viewer != null) clearViewer(viewer.getUniqueId());
            return new ToggleResult(Outcome.NOT_IN_GAME, List.of());
        }

        UUID viewerId = viewer.getUniqueId();
        if (highlightedByViewer.containsKey(viewerId)) {
            clearViewer(viewerId);
            return new ToggleResult(Outcome.DISABLED, List.of());
        }

        List<Player> teammates = eligibleTeammates(arena, viewer);
        if (teammates.isEmpty()) return new ToggleResult(Outcome.NO_TEAMMATES, List.of());

        LinkedHashSet<UUID> targetIds = new LinkedHashSet<>();
        knownPlayers.put(viewerId, viewer);
        for (Player teammate : teammates) {
            UUID targetId = teammate.getUniqueId();
            if (!targetIds.add(targetId)) continue;
            knownPlayers.put(targetId, teammate);
            int owners = ownersByTarget.getOrDefault(targetId, 0);
            if (owners == 0) {
                originalGlowing.put(targetId, teammate.isGlowing());
            }
            ownersByTarget.put(targetId, owners + 1);
            if (!teammate.isGlowing()) teammate.setGlowing(true);
        }
        highlightedByViewer.put(viewerId, targetIds);
        arenaByViewer.put(viewerId, arena);
        return new ToggleResult(Outcome.ENABLED, teammates);
    }

    /**
     * Return the online, alive members of the viewer's current formal team.
     * Waiting-stage squads and historical teams are intentionally ignored.
     */
    static List<Player> eligibleTeammates(IArena arena, Player viewer) {
        if (!isPlayableViewer(viewer, arena)) return List.of();
        ITeam team = arena.getTeam(viewer);
        if (team == null || team.getMembers() == null) return List.of();

        UUID viewerId = viewer.getUniqueId();
        Set<UUID> seen = new HashSet<>();
        List<Player> result = new ArrayList<>();
        for (Player member : new ArrayList<>(team.getMembers())) {
            if (member == null || member.getUniqueId() == null
                    || viewerId.equals(member.getUniqueId()) || !seen.add(member.getUniqueId())) continue;
            if (!member.isOnline() || member.isDead() || !arena.isPlayer(member)
                    || arena.isSpectator(member.getUniqueId())
                    || arena.isReSpawning(member.getUniqueId())) continue;
            result.add(member);
        }
        return List.copyOf(result);
    }

    private static boolean isPlayableViewer(Player viewer, IArena arena) {
        return viewer != null && viewer.getUniqueId() != null && arena != null
                && arena.getStatus() == GameState.playing && arena.isPlayer(viewer)
                && !arena.isSpectator(viewer.getUniqueId())
                && !arena.isReSpawning(viewer.getUniqueId());
    }

    public synchronized void clearViewer(UUID viewerId) {
        if (viewerId == null) return;
        Set<UUID> targets = highlightedByViewer.remove(viewerId);
        arenaByViewer.remove(viewerId);
        if (targets != null) {
            for (UUID targetId : targets) releaseTarget(targetId);
        }
        if (!ownersByTarget.containsKey(viewerId)) knownPlayers.remove(viewerId);
    }

    private void releaseTarget(UUID targetId) {
        Integer owners = ownersByTarget.get(targetId);
        if (owners == null) return;
        if (owners > 1) {
            ownersByTarget.put(targetId, owners - 1);
            return;
        }

        ownersByTarget.remove(targetId);
        Boolean previous = originalGlowing.remove(targetId);
        Player target = knownPlayers.remove(targetId);
        if (previous != null && target != null) target.setGlowing(previous);
    }

    /** Remove a target from every viewer, restoring its prior glow state. */
    public synchronized void clearTarget(UUID targetId) {
        if (targetId == null) return;
        for (Iterator<Map.Entry<UUID, Set<UUID>>> iterator = highlightedByViewer.entrySet().iterator();
             iterator.hasNext(); ) {
            Map.Entry<UUID, Set<UUID>> entry = iterator.next();
            if (!entry.getValue().remove(targetId)) continue;
            releaseTarget(targetId);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
                arenaByViewer.remove(entry.getKey());
            }
        }
        if (!ownersByTarget.containsKey(targetId)) knownPlayers.remove(targetId);
    }

    public synchronized void clearArena(@NotNull IArena arena) {
        for (UUID viewerId : new ArrayList<>(arenaByViewer.keySet())) {
            if (arenaByViewer.get(viewerId) == arena) clearViewer(viewerId);
        }
    }

    public synchronized void clearArena(String arenaName, String worldName) {
        for (Map.Entry<UUID, IArena> entry : new ArrayList<>(arenaByViewer.entrySet())) {
            IArena arena = entry.getValue();
            boolean sameArena = arenaName != null && arenaName.equalsIgnoreCase(arena.getArenaName());
            boolean sameWorld = worldName != null && worldName.equalsIgnoreCase(arena.getWorldName());
            if (sameArena || sameWorld) clearViewer(entry.getKey());
        }
    }

    public synchronized void clearAll() {
        for (UUID viewerId : new ArrayList<>(highlightedByViewer.keySet())) clearViewer(viewerId);
        highlightedByViewer.clear();
        arenaByViewer.clear();
        ownersByTarget.clear();
        originalGlowing.clear();
        knownPlayers.clear();
    }

    @EventHandler
    public void onPlayerLeave(PlayerLeaveArenaEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        clearViewer(id);
        clearTarget(id);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        clearViewer(id);
        clearTarget(id);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        clearViewer(id);
        clearTarget(id);
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getNewState() != GameState.playing) clearArena(event.getArena());
    }

    @EventHandler
    public void onArenaDisable(ArenaDisableEvent event) {
        clearArena(event.getArenaName(), event.getWorldName());
    }
}
