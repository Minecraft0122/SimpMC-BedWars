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

    @Test
    void fluidFlowOnlyMutatesTrackedPlayerBlocks() {
        assertTrue(PlacedBlockListener.shouldCancelFluidFlow(false, true, false, false));
        assertFalse(PlacedBlockListener.shouldCancelFluidFlow(true, true, false, false));
        assertTrue(PlacedBlockListener.shouldCancelFluidFlow(true, false, false, false));
        assertFalse(PlacedBlockListener.shouldCancelFluidFlow(false, false, false, true));
    }

    @Test
    void pistonsCannotMoveMapBlocks() {
        assertTrue(PlacedBlockListener.shouldCancelPiston(false, false, true, true));
        assertTrue(PlacedBlockListener.shouldCancelPiston(true, true, true, true));
        assertFalse(PlacedBlockListener.shouldCancelPiston(true, false, true, true));
        assertTrue(PlacedBlockListener.shouldCancelPiston(true, false, false, true));
        assertTrue(PlacedBlockListener.shouldCancelPiston(false, true, false, true));
        assertFalse(PlacedBlockListener.shouldCancelPiston(false, false, false, false));
    }

    @Test
    void entitiesCannotChangeOriginalMapBlocks() {
        assertTrue(PlacedBlockListener.shouldCancelEntityChange(true, false, true));
        assertFalse(PlacedBlockListener.shouldCancelEntityChange(false, false, true));
        assertFalse(PlacedBlockListener.shouldCancelEntityChange(true, true, true));
        assertFalse(PlacedBlockListener.shouldCancelEntityChange(true, false, false));
    }
}
