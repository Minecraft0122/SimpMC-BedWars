package com.andrei1058.bedwars.listeners;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Random;

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
