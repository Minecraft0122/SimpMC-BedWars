package com.andrei1058.bedwars.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireballListenerTest {

    @Test
    void calculatesIndependentKnockbackAwayFromExplosion() {
        Vector explosion = new Vector(0, 65, 0);

        Vector east = FireballListener.calculateKnockback(
                explosion, new Vector(2, 64, 0), 1.25, 0.8);
        Vector west = FireballListener.calculateKnockback(
                explosion, new Vector(-2, 64, 0), 1.25, 0.8);

        assertTrue(east.getX() > 0);
        assertTrue(west.getX() < 0);
        assertTrue(east.getY() > 0);
        assertEquals(new Vector(0, 65, 0), explosion, "calculation must not mutate the explosion position");
    }

    @Test
    void includesPlayersOnTheSphericalExplosionBoundary() {
        assertTrue(FireballListener.isWithinExplosionRadius(
                new Vector(0D, 64D, 0D), new Vector(3D, 68D, 0D), 5D));
    }

    @Test
    void excludesPlayersInTheBoundingCubeOutsideTheSphericalRadius() {
        assertFalse(FireballListener.isWithinExplosionRadius(
                new Vector(0D, 64D, 0D), new Vector(3D, 68D, 3D), 5D));
    }

    @Test
    void rejectsInvalidExplosionRadiiAndNonFinitePositions() {
        Vector explosion = new Vector(0D, 64D, 0D);
        Vector player = new Vector(0D, 64D, 0D);

        assertFalse(FireballListener.isWithinExplosionRadius(explosion, player, -1D));
        assertFalse(FireballListener.isWithinExplosionRadius(explosion, player, Double.NaN));
        assertFalse(FireballListener.isWithinExplosionRadius(
                explosion, new Vector(Double.POSITIVE_INFINITY, 64D, 0D), 5D));
        assertFalse(FireballListener.isWithinExplosionRadius(null, player, 5D));
        assertFalse(FireballListener.isWithinExplosionRadius(explosion, null, 5D));
    }

    @Test
    void usesOneBoundedExplosionSizeForAllFireballPaths() {
        assertEquals(0.1D, FireballListener.normalizeExplosionSize(Double.NaN));
        assertEquals(0.1D, FireballListener.normalizeExplosionSize(-5D));
        assertEquals(16D, FireballListener.normalizeExplosionSize(99D));
        assertEquals(3.25D, FireballListener.normalizeExplosionSize(3.25D));
    }

    @Test
    void overlappingFireballAndPlayerProduceFiniteVerticalKnockback() {
        Vector knockback = FireballListener.calculateKnockback(
                new Vector(3D, 64D, -2D), new Vector(3D, 64D, -2D), 1.25D, 0.8D);

        assertEquals(new Vector(0D, 1.2D, 0D), knockback);
        assertTrue(Double.isFinite(knockback.getX()));
        assertTrue(Double.isFinite(knockback.getY()));
        assertTrue(Double.isFinite(knockback.getZ()));
    }

    @Test
    void nonFiniteKnockbackStrengthsCannotReachBukkitVelocity() {
        Vector knockback = FireballListener.calculateKnockback(
                new Vector(0D, 64D, 0D), new Vector(1D, 64D, 0D),
                Double.NaN, Double.POSITIVE_INFINITY);

        assertEquals(new Vector(0D, 0D, 0D), knockback);
    }

    @Test
    void excessiveFiniteKnockbackStrengthsRemainFinite() {
        Vector knockback = FireballListener.calculateKnockback(
                new Vector(0D, 64D, 0D), new Vector(1D, 64D, 0D),
                Double.MAX_VALUE, Double.MAX_VALUE);

        assertTrue(Double.isFinite(knockback.getX()));
        assertTrue(Double.isFinite(knockback.getY()));
        assertTrue(Double.isFinite(knockback.getZ()));
    }

    @Test
    void creditsOnlyEnemyPlayers() {
        assertTrue(FireballListener.shouldCreditShooter(false, false));
        assertFalse(FireballListener.shouldCreditShooter(true, true));
        assertFalse(FireballListener.shouldCreditShooter(false, true));
    }

    @Test
    void teammateFireballsNeverDealDamage() {
        assertFalse(FireballListener.shouldDamageTarget(false, true));
        assertTrue(FireballListener.shouldDamageTarget(false, false));
        assertFalse(FireballListener.shouldDamageTarget(true, false));
    }

    @Test
    void cancelsAllNativeDamageButKeepsTheMarkedCustomExplosion() {
        assertTrue(FireballListener.shouldCancelDirectHit(false, false));
        assertTrue(FireballListener.shouldCancelDirectHit(true, false));
        assertTrue(FireballListener.shouldCancelDirectHit(false, true));
        assertFalse(FireballListener.shouldCancelDirectHit(true, true));
    }

    @Test
    void cancelsNativeContactBeforeTheCombatListenerCanAttributeIt() throws NoSuchMethodException {
        EventHandler handler = FireballListener.class
                .getDeclaredMethod("fireballDirectHit", EntityDamageByEntityEvent.class)
                .getAnnotation(EventHandler.class);

        assertEquals(EventPriority.LOWEST, handler.priority());
    }
}
