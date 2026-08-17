package com.andrei1058.bedwars.listeners;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireballLaunchPhysicsTest {

    @Test
    void givesSneakingLaunchesAStrongInitialSpeedAdvantage() {
        assertEquals(1.5D, FireballLaunchPhysics.launchSpeed(15D, false, 1.6D), 1.0E-9D);
        assertEquals(2.4D, FireballLaunchPhysics.launchSpeed(15D, true, 1.6D), 1.0E-9D);
    }

    @Test
    void buildsExplicitVelocityAndVanillaAccelerationFromAimDirection() {
        Vector aim = new Vector(4D, 0D, 3D);

        Vector regular = FireballLaunchPhysics.launchVelocity(aim, 15D, false, 1.6D);
        Vector sneaking = FireballLaunchPhysics.launchVelocity(aim, 15D, true, 1.6D);
        Vector regularAcceleration = FireballLaunchPhysics.launchAcceleration(aim, false, 2D);
        Vector sneakingAcceleration = FireballLaunchPhysics.launchAcceleration(aim, true, 2D);

        assertEquals(1.5D, regular.length(), 1.0E-9D);
        assertEquals(2.4D, sneaking.length(), 1.0E-9D);
        assertEquals(FireballLaunchPhysics.DEFAULT_ACCELERATION, regularAcceleration.length(), 1.0E-9D);
        assertEquals(0.2D, sneakingAcceleration.length(), 1.0E-9D);
        assertEquals(aim, new Vector(4D, 0D, 3D));
    }

    @Test
    void samplesEachDefaultFlightDistanceInsideTheConfiguredRange() {
        Random random = new Random(1058L);
        for (int sample = 0; sample < 1_000; sample++) {
            double distance = FireballLaunchPhysics.randomFlightDistance(200D, 300D, random);
            assertTrue(distance >= 200D && distance < 300D);
        }
    }

    @Test
    void normalizesReversedAndInvalidFlightRanges() {
        assertEquals(new FireballLaunchPhysics.FlightRange(200D, 300D),
                FireballLaunchPhysics.normalizeFlightRange(300D, 200D));
        assertEquals(new FireballLaunchPhysics.FlightRange(200D, 300D),
                FireballLaunchPhysics.normalizeFlightRange(Double.NaN, Double.POSITIVE_INFINITY));
        assertEquals(new FireballLaunchPhysics.FlightRange(240D, 240D),
                FireballLaunchPhysics.normalizeFlightRange(240D, 240D));
        assertEquals(240D,
                FireballLaunchPhysics.randomFlightDistance(240D, 240D, new Random(1L)));
    }

    @Test
    void accumulatesTheTravelledPathInsteadOfTicksOrStraightLineDisplacement() {
        double travelled = FireballLaunchPhysics.accumulateTravelledDistance(
                0D, 0D, 0D, 0D, 3D, 0D, 0D);
        travelled = FireballLaunchPhysics.accumulateTravelledDistance(
                travelled, 3D, 0D, 0D, 3D, 4D, 0D);

        assertEquals(7D, travelled, 1.0E-9D);
    }

    @Test
    void recoilPointsAwayFromTheShotAndStaysSmall() {
        Vector direction = new Vector(1D, -0.5D, 0D);
        Vector recoil = FireballLaunchPhysics.sneakRecoil(direction, 0.1D);

        assertTrue(recoil.getX() < 0D);
        assertTrue(recoil.getY() > 0D);
        assertEquals(-1D, recoil.clone().normalize().dot(direction.clone().normalize()), 1.0E-9D);
        assertEquals(0.1D, recoil.length(), 1.0E-9D);
    }

    @Test
    void capsConfiguredRecoilAndHandlesVerticalShots() {
        assertEquals(FireballLaunchPhysics.MAX_SNEAK_RECOIL,
                FireballLaunchPhysics.sneakRecoil(new Vector(0D, 0D, 1D), 4D).length(), 1.0E-9D);
        assertEquals(new Vector(0D, 0.1D, 0D),
                FireballLaunchPhysics.sneakRecoil(new Vector(0D, -1D, 0D), 0.1D));
    }

    @Test
    void allowsTwoAndAHalfFireballsPerSecondAtTheDefaultCooldown() {
        assertFalse(FireballLaunchPhysics.cooldownElapsed(10_399L, 10_000L, 0.4D));
        assertTrue(FireballLaunchPhysics.cooldownElapsed(10_400L, 10_000L, 0.4D));
        assertTrue(FireballLaunchPhysics.cooldownElapsed(10_000L, 10_000L, 0D));
    }

}
