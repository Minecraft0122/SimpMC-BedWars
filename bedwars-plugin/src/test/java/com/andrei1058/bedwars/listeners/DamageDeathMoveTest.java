package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
    void recoversSpectatorsOnlyAfterTheyEnterTheVoid() {
        assertTrue(DamageDeathMove.shouldRecoverSpectatorAfterVoid(-0.1));
        assertFalse(DamageDeathMove.shouldRecoverSpectatorAfterVoid(0));
        assertFalse(DamageDeathMove.shouldRecoverSpectatorAfterVoid(10));
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

    @Test
    void usesTheBedStateAtDeathForRespawnEligibility() {
        assertTrue(DamageDeathMove.canRespawnAfterDeath(true, true));
        assertFalse(DamageDeathMove.canRespawnAfterDeath(false, false));
        assertTrue(DamageDeathMove.canRespawnAfterDeath(null, false));
        assertFalse(DamageDeathMove.canRespawnAfterDeath(null, true));
    }

    @Test
    void avoidsASecondDelayedRespawnWhenTheArenaUsesImmediateRespawn() {
        assertFalse(DamageDeathMove.requiresManualRespawn(true));
        assertTrue(DamageDeathMove.requiresManualRespawn(false));
        assertTrue(DamageDeathMove.requiresManualRespawn(null));
    }

    @Test
    void treatsRespawnProtectionAsActiveOnlyBeforeItsExpiry() {
        assertTrue(DamageDeathMove.isRespawnProtectionActive(1_001L, 1_000L));
        assertFalse(DamageDeathMove.isRespawnProtectionActive(1_001L, 1_001L));
        assertFalse(DamageDeathMove.isRespawnProtectionActive(null, 1_000L));
    }

    @Test
    void scalesTntJumpAndRejectsNonFiniteVectors() {
        Vector normal = DamageDeathMove.calculateTntJumpVelocity(
                new Vector(0, 64, 0), new Vector(2, 64, 0), 4, 0.5, 5, 2, 1.0);
        Vector nerfed = DamageDeathMove.calculateTntJumpVelocity(
                new Vector(0, 64, 0), new Vector(2, 64, 0), 4, 0.5, 5, 2, 0.9);

        assertTrue(normal.lengthSquared() > nerfed.lengthSquared());
        assertTrue(Double.isFinite(nerfed.getX()));
        assertTrue(Double.isFinite(nerfed.getY()));
        assertTrue(Double.isFinite(nerfed.getZ()));
        assertEquals(new Vector(), DamageDeathMove.calculateTntJumpVelocity(
                new Vector(0, 64, 0), new Vector(0, 64, 0), 4, 0.5, 5, 2, 0.9));
        assertEquals(new Vector(), DamageDeathMove.calculateTntJumpVelocity(
                new Vector(Double.NaN, 64, 0), new Vector(2, 64, 0), 4, 0.5, 5, 2, 0.9));
    }

    @Test
    void sendsVoidDeathsToTheTeamHome() {
        Location death = new Location(null, 12, 40, 8, 30, 10);
        Location teamHome = new Location(null, 100, 70, -20, 90, 0);
        Location selected = DamageDeathMove.selectRespawnLocation(true, death, teamHome, null);

        assertEquals(teamHome, selected);
        assertNotSame(teamHome, selected);
    }

    @Test
    void keepsOrdinaryDeathsAtTheirDeathLocation() {
        Location death = new Location(null, 12, 40, 8, 30, 10);
        Location teamHome = new Location(null, 100, 70, -20, 90, 0);
        Location selected = DamageDeathMove.selectRespawnLocation(false, death, teamHome, null);

        assertEquals(death, selected);
        assertNotSame(death, selected);
    }

    @Test
    void fallsBackWhenTheDeathLocationIsUnavailable() {
        Location fallback = new Location(null, 0, 64, 0);
        assertEquals(fallback, DamageDeathMove.selectRespawnLocation(false, null, null, fallback));
        assertEquals(fallback, DamageDeathMove.selectRespawnLocation(true, null, null, fallback));
    }

    @Test
    void breaksInvisibilityOnlyForSuccessfulEnemyDamage() {
        Player victim = player("victim");
        Player teammate = player("teammate");
        Player opponent = player("opponent");
        Player respawningOpponent = player("respawning");
        ITeam red = team("red");
        ITeam blue = team("blue");
        IArena arena = arena(
                Set.of(victim, teammate, opponent, respawningOpponent),
                Set.of(respawningOpponent),
                Map.of(victim, red, teammate, red, opponent, blue, respawningOpponent, blue));

        assertTrue(DamageDeathMove.isEnemyAttack(arena, victim, opponent));
        assertFalse(DamageDeathMove.isEnemyAttack(arena, victim, teammate));
        assertFalse(DamageDeathMove.isEnemyAttack(arena, victim, victim));
        assertFalse(DamageDeathMove.isEnemyAttack(arena, victim, respawningOpponent));
        assertTrue(DamageDeathMove.dealtDamage(false, 0.5));
        assertFalse(DamageDeathMove.dealtDamage(true, 5));
        assertFalse(DamageDeathMove.dealtDamage(false, 0));
    }

    private static IArena arena(Set<Player> players, Set<Player> respawning, Map<Player, ITeam> teams) {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isPlayer" -> players.contains(args[0]);
                    case "isSpectator" -> false;
                    case "isReSpawning" -> respawning.stream()
                            .anyMatch(player -> player.getUniqueId().equals(args[0]));
                    case "getTeam" -> teams.get(args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ITeam team(String name) {
        return (ITeam) Proxy.newProxyInstance(ITeam.class.getClassLoader(), new Class<?>[]{ITeam.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player player(String name) {
        UUID id = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
