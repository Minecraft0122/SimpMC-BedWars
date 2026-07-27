/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.listeners;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Coalesces successful block placements into one client resync batch per
 * player and tick. Bukkit events and scheduler callbacks both access this on
 * the main thread, so no concurrent collection or locking is required.
 */
final class BlockPlacementResyncBuffer {

    private final Map<UUID, LinkedHashSet<BlockPosition>> pending = new HashMap<>();

    /**
     * @return true only when the caller must schedule a new flush task
     */
    boolean queue(UUID playerId, UUID worldId, int x, int y, int z) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(worldId, "worldId");

        LinkedHashSet<BlockPosition> positions = pending.get(playerId);
        boolean shouldSchedule = positions == null;
        if (shouldSchedule) {
            positions = new LinkedHashSet<>();
            pending.put(playerId, positions);
        }
        positions.add(new BlockPosition(worldId, x, y, z));
        return shouldSchedule;
    }

    List<BlockPosition> drain(UUID playerId) {
        LinkedHashSet<BlockPosition> positions = pending.remove(playerId);
        return positions == null ? List.of() : List.copyOf(positions);
    }

    void discard(UUID playerId) {
        pending.remove(playerId);
    }

    record BlockPosition(UUID worldId, int x, int y, int z) {
    }
}
