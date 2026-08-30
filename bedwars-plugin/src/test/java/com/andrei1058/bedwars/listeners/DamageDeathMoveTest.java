package com.andrei1058.bedwars.listeners;

import org.bukkit.Location;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DamageDeathMoveTest {

    @Test
    public void sendsVoidDeathsToTheTeamHome() {
        Location death = new Location(null, 12, 40, 8, 30, 10);
        Location teamHome = new Location(null, 100, 70, -20, 90, 0);
        Location selected = DamageDeathMove.selectRespawnLocation(true, death, teamHome, null);

        assertEquals(teamHome, selected);
        assertNotSame(teamHome, selected);
    }

    @Test
    public void keepsOrdinaryDeathsAtTheirDeathLocation() {
        Location death = new Location(null, 12, 40, 8, 30, 10);
        Location teamHome = new Location(null, 100, 70, -20, 90, 0);
        Location selected = DamageDeathMove.selectRespawnLocation(false, death, teamHome, null);

        assertEquals(death, selected);
        assertNotSame(death, selected);
    }

    @Test
    public void fallsBackWhenTheDeathLocationIsUnavailable() {
        Location fallback = new Location(null, 0, 64, 0);
        assertEquals(fallback, DamageDeathMove.selectRespawnLocation(false, null, null, fallback));
        assertEquals(fallback, DamageDeathMove.selectRespawnLocation(true, null, null, fallback));
    }

    @Test
    public void recoversSpectatorsOnlyAfterTheyEnterTheVoid() {
        assertTrue(DamageDeathMove.shouldRecoverSpectatorAfterVoid(-0.1));
        assertFalse(DamageDeathMove.shouldRecoverSpectatorAfterVoid(0));
        assertFalse(DamageDeathMove.shouldRecoverSpectatorAfterVoid(10));
    }
}
