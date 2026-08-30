package com.andrei1058.bedwars.listeners;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BreakPlaceFireballTest {

    @Test
    public void protectsEndStoneAndLegacyTerracottaFromFireballs() {
        assertTrue(BreakPlace.shouldProtectFireballBlock(true, Material.ENDER_STONE));
        assertTrue(BreakPlace.shouldProtectFireballBlock(true, Material.HARD_CLAY));
        assertTrue(BreakPlace.shouldProtectFireballBlock(true, Material.STAINED_CLAY));
    }

    @Test
    public void leavesOtherExplosionsAndBreakableBlocksAlone() {
        assertFalse(BreakPlace.shouldProtectFireballBlock(false, Material.ENDER_STONE));
        assertFalse(BreakPlace.shouldProtectFireballBlock(true, Material.WOOL));
        assertFalse(BreakPlace.shouldProtectFireballBlock(true, null));
    }
}
