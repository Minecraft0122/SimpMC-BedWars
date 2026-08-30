package com.andrei1058.bedwars.listeners;

import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FireballListenerTest {

    @Test
    public void calculatesIndependentKnockbackAwayFromExplosion() {
        Vector explosion = new Vector(0, 65, 0);

        Vector east = FireballListener.calculateKnockback(
                explosion, new Vector(2, 64, 0), 1.25, 0.8);
        Vector west = FireballListener.calculateKnockback(
                explosion, new Vector(-2, 64, 0), 1.25, 0.8);

        assertTrue(east.getX() > 0);
        assertTrue(west.getX() < 0);
        assertTrue(east.getY() > 0);
        assertEquals(new Vector(0, 65, 0), explosion);
    }

    @Test
    public void overlappingPlayersReceiveFiniteVerticalKnockback() {
        Vector knockback = FireballListener.calculateKnockback(
                new Vector(3D, 64D, -2D), new Vector(3D, 64D, -2D), 1.25D, 0.8D);

        assertEquals(new Vector(0D, 1.2D, 0D), knockback);
        assertTrue(Double.isFinite(knockback.getY()));
    }

    @Test
    public void usesSphericalRadiusForDamageAndKnockback() {
        Vector explosion = new Vector(0D, 64D, 0D);
        assertTrue(FireballListener.isWithinExplosionRadius(
                explosion, new Vector(1D, 64D, 0D), 3.25D));
        assertTrue(FireballListener.isWithinExplosionRadius(
                explosion, new Vector(2D, 65D, 0D), 3.25D));
        assertFalse(FireballListener.isWithinExplosionRadius(
                explosion, new Vector(3D, 64D, 3D), 3.25D));
    }

    @Test
    public void cancelsNativeFireballDamageBeforeOtherListeners() throws NoSuchMethodException {
        EventHandler handler = FireballListener.class
                .getDeclaredMethod("fireballDirectHit", org.bukkit.event.entity.EntityDamageByEntityEvent.class)
                .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.LOWEST, handler.priority());
    }

    @Test
    public void customKnockbackRunsAfterNativeExplosion() throws NoSuchMethodException {
        assertEquals(EntityExplodeEvent.class,
                FireballListener.class.getDeclaredMethod("fireballExplode", EntityExplodeEvent.class)
                        .getParameterTypes()[0]);
    }
}
