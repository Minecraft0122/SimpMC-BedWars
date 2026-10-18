package com.andrei1058.bedwars.listeners;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FluidFlowTest {

    @Test
    public void protectsMapBlocksWhenTheSourceIsNotPlayerPlaced() {
        assertTrue(BreakPlace.shouldCancelFluidFlow(false, false, false, false));
    }

    @Test
    public void allowsPlayerPlacedFluidToFlowIntoAir() {
        assertFalse(BreakPlace.shouldCancelFluidFlow(true, false, true, false));
    }

    @Test
    public void allowsFlowIntoTrackedBlocksAndWhenMapBreakIsEnabled() {
        assertFalse(BreakPlace.shouldCancelFluidFlow(false, true, false, false));
        assertFalse(BreakPlace.shouldCancelFluidFlow(false, false, false, true));
    }
}
