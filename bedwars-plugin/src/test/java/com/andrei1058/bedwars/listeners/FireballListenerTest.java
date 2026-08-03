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
    void creditsOnlyEnemyPlayers() {
        assertTrue(FireballListener.shouldCreditShooter(false, false));
        assertFalse(FireballListener.shouldCreditShooter(true, true));
        assertFalse(FireballListener.shouldCreditShooter(false, true));
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
