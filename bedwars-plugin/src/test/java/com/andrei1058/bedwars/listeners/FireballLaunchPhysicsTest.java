package com.andrei1058.bedwars.listeners;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireballLaunchPhysicsTest {

    @Test
    void acceleratesOnlySneakingLaunches() {
        assertEquals(11D, FireballLaunchPhysics.launchSpeed(11D, false, 1.25D));
        assertEquals(13.75D, FireballLaunchPhysics.launchSpeed(11D, true, 1.25D));
    }

    @Test
    void recoilPointsAwayFromTheShotAndStaysSmall() {
        Vector recoil = FireballLaunchPhysics.sneakRecoil(new Vector(1D, -0.5D, 0D), 0.05D);

        assertTrue(recoil.getX() < 0D);
        assertEquals(0D, recoil.getY());
        assertEquals(0.05D, recoil.length(), 1.0E-9D);
    }

    @Test
    void capsConfiguredRecoilAndHandlesVerticalShots() {
        assertEquals(FireballLaunchPhysics.MAX_SNEAK_RECOIL,
                FireballLaunchPhysics.sneakRecoil(new Vector(0D, 0D, 1D), 4D).length(), 1.0E-9D);
        assertEquals(new Vector(), FireballLaunchPhysics.sneakRecoil(new Vector(0D, -1D, 0D), 0.05D));
    }
}
