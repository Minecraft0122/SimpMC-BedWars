package com.andrei1058.bedwars.listeners;

import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageDeathMoveTest {

    @Test
    void detectsChunkChangesWithoutResolvingChunkObjects() {
        assertFalse(DamageDeathMove.changedChunk(
                new Location(null, 1, 64, 1), new Location(null, 15, 80, 15)));
        assertTrue(DamageDeathMove.changedChunk(
                new Location(null, 15, 64, 15), new Location(null, 16, 64, 15)));
        assertTrue(DamageDeathMove.changedChunk(
                new Location(null, -16, 64, 0), new Location(null, -17, 64, 0)));
    }

    @Test
    void distinguishesExplosionDamageFromOrdinaryCombat() {
        assertTrue(DamageDeathMove.isExplosionDamage(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, true));
        assertTrue(DamageDeathMove.isExplosionDamage(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, true));
        assertFalse(DamageDeathMove.isExplosionDamage(EntityDamageEvent.DamageCause.ENTITY_ATTACK, false));
        assertFalse(DamageDeathMove.isExplosionDamage(EntityDamageEvent.DamageCause.PROJECTILE, false));
    }

    @Test
    void acceptsOnlyLastHitsInsideTheAttributionWindow() {
        assertTrue(DamageDeathMove.isRecent(85_000, 100_000, 15_000));
        assertTrue(DamageDeathMove.isRecent(100_000, 100_000, 15_000));
        assertFalse(DamageDeathMove.isRecent(84_999, 100_000, 15_000));
        assertFalse(DamageDeathMove.isRecent(100_001, 100_000, 15_000));
    }
}
