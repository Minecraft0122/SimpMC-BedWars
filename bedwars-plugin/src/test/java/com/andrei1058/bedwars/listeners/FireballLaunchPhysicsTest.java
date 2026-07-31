package com.andrei1058.bedwars.listeners;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireballLaunchPhysicsTest {

    @Test
    void acceleratesOnlySneakingLaunches() {
        assertEquals(1.6D, FireballLaunchPhysics.launchSpeed(16D, false, 1.5D), 1.0E-9D);
        assertEquals(2.4D, FireballLaunchPhysics.launchSpeed(16D, true, 1.5D), 1.0E-9D);
    }

    @Test
    void buildsExplicitVelocityAndVanillaAccelerationFromAimDirection() {
        Vector aim = new Vector(4D, 0D, 3D);

        Vector regular = FireballLaunchPhysics.launchVelocity(aim, 16D, false, 1.5D);
        Vector sneaking = FireballLaunchPhysics.launchVelocity(aim, 16D, true, 1.5D);
        Vector acceleration = FireballLaunchPhysics.launchAcceleration(aim);

        assertEquals(1.6D, regular.length(), 1.0E-9D);
        assertEquals(2.4D, sneaking.length(), 1.0E-9D);
        assertEquals(FireballLaunchPhysics.DEFAULT_ACCELERATION, acceleration.length(), 1.0E-9D);
        assertEquals(aim, new Vector(4D, 0D, 3D));
    }

    @Test
    void defaultsCoverTheRequestedOpenAreaDistanceWithoutChangingAcceleration() {
        double regularDistance = unobstructedDistance(1.6D, 80);
        double sneakingDistance = unobstructedDistance(2.4D, 80);

        assertTrue(regularDistance >= 100D && regularDistance <= 200D);
        assertTrue(sneakingDistance > regularDistance);
        assertTrue(sneakingDistance <= 200D);
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

    private static double unobstructedDistance(double initialVelocity, int ticks) {
        double velocity = initialVelocity;
        double distance = 0D;
        for (int tick = 0; tick < ticks; tick++) {
            distance += velocity;
            velocity = (velocity + FireballLaunchPhysics.DEFAULT_ACCELERATION) * 0.95D;
        }
        return distance;
    }
}
