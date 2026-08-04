package com.andrei1058.bedwars.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBlockListenerTest {

    @Test
    void cancelledOrRejectedPlacementRestoresPreviousOwnership() {
        assertFalse(PlacedBlockListener.shouldTrackAfterPlacement(false, true, true, false, false));
        assertTrue(PlacedBlockListener.shouldTrackAfterPlacement(true, true, true, false, false));
        assertFalse(PlacedBlockListener.shouldTrackAfterPlacement(false, false, false, false, false));
        assertTrue(PlacedBlockListener.shouldTrackAfterPlacement(true, false, false, false, false));
    }

    @Test
    void successfulPlacementUsesTheFinalWorldState() {
        assertTrue(PlacedBlockListener.shouldTrackAfterPlacement(false, false, true, false, false));
        assertFalse(PlacedBlockListener.shouldTrackAfterPlacement(false, false, true, true, false));
        assertFalse(PlacedBlockListener.shouldTrackAfterPlacement(false, false, true, false, true));
        assertTrue(PlacedBlockListener.shouldTrackAfterPlacement(true, false, true, false, true));
    }

    @Test
    void destructionRetainsOwnershipOnlyWhenFinalStateMatchesOriginal() {
        assertTrue(PlacedBlockListener.shouldRetainAfterDestruction(true));
        assertFalse(PlacedBlockListener.shouldRetainAfterDestruction(false));
    }
}
