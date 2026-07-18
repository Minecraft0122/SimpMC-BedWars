package com.andrei1058.bedwars.arena;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBlockTrackerTest {

    @Test
    void duplicatePlacementCreatesOnlyOneOwnershipMarker() {
        PlacedBlockTracker tracker = new PlacedBlockTracker();

        assertTrue(tracker.add(12, 64, -8));
        assertFalse(tracker.add(12, 64, -8));
        assertEquals(1, tracker.size());

        assertTrue(tracker.remove(12, 64, -8));
        assertFalse(tracker.contains(12, 64, -8));
        assertFalse(tracker.remove(12, 64, -8));
    }

    @Test
    void legacyListIsDetachedFromInternalOwnershipState() {
        PlacedBlockTracker tracker = new PlacedBlockTracker();
        tracker.add(1, 2, 3);

        LinkedList<Vector> snapshot = tracker.snapshot();
        snapshot.clear();
        snapshot.add(new Vector(9, 9, 9));

        assertTrue(tracker.contains(1, 2, 3));
        assertFalse(tracker.contains(9, 9, 9));
        assertEquals(1, tracker.size());
    }

    @Test
    void publicSnapshotIsImmutableAndDetached() {
        PlacedBlockTracker tracker = new PlacedBlockTracker();
        tracker.add(-30_000_000, -64, 30_000_000);

        Set<Vector> snapshot = tracker.immutableSnapshot();

        assertEquals(Set.of(new Vector(-30_000_000, -64, 30_000_000)), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new Vector()));
        tracker.clear();
        assertEquals(1, snapshot.size());
        assertEquals(0, tracker.size());
    }
}
