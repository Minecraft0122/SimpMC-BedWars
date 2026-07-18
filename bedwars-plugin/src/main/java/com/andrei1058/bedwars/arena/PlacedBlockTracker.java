package com.andrei1058.bedwars.arena;

import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Arena-local index of blocks created during the current game.
 *
 * <p>Coordinates are unique and lookup is constant-time. The tracker deliberately
 * does not expose its backing set because an add-on must not be able to mark map
 * blocks as player-placed by mutating a returned collection.</p>
 */
final class PlacedBlockTracker {

    private final Set<BlockPosition> positions = new HashSet<>();

    boolean add(int x, int y, int z) {
        return positions.add(new BlockPosition(x, y, z));
    }

    boolean remove(int x, int y, int z) {
        return positions.remove(new BlockPosition(x, y, z));
    }

    boolean contains(int x, int y, int z) {
        return positions.contains(new BlockPosition(x, y, z));
    }

    int size() {
        return positions.size();
    }

    LinkedList<Vector> snapshot() {
        LinkedList<Vector> result = new LinkedList<>();
        for (BlockPosition position : positions) {
            result.add(new Vector(position.x(), position.y(), position.z()));
        }
        return result;
    }

    Set<Vector> immutableSnapshot() {
        Set<Vector> result = new HashSet<>(positions.size());
        for (BlockPosition position : positions) {
            result.add(new Vector(position.x(), position.y(), position.z()));
        }
        return Set.copyOf(result);
    }

    void clear() {
        positions.clear();
    }

    private record BlockPosition(int x, int y, int z) {
    }
}
