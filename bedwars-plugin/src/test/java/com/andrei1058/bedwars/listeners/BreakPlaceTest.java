package com.andrei1058.bedwars.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.block.BlockIgniteEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreakPlaceTest {

    @Test
    void npcProtectionIsSymmetricAroundCenteredCoordinates() {
        World arena = world("arena");
        Location npc = new Location(arena, 10.5, 64, 20.5);

        assertTrue(BreakPlace.isWithinNpcProtection(new Location(arena, 9, 64, 19), npc, 1));
        assertTrue(BreakPlace.isWithinNpcProtection(new Location(arena, 11, 64, 21), npc, 1));
        assertTrue(BreakPlace.isWithinNpcProtection(new Location(arena, 10, 63, 20), npc, 1));
        assertTrue(BreakPlace.isWithinNpcProtection(new Location(arena, 10, 66, 20), npc, 1));
    }

    @Test
    void blocksOutsideNpcAndOtherWorldsAreNotProtected() {
        World arena = world("arena");
        Location npc = new Location(arena, 10.5, 64, 20.5);

        assertFalse(BreakPlace.isWithinNpcProtection(new Location(arena, 8, 64, 20), npc, 1));
        assertFalse(BreakPlace.isWithinNpcProtection(new Location(arena, 10, 67, 20), npc, 1));
        assertFalse(BreakPlace.isWithinNpcProtection(new Location(world("lobby"), 10, 64, 20), npc, 1));
    }

    @Test
    void preventsSpreadWithoutBlockingFireballIgnition() {
        assertTrue(BreakPlace.isFireSpread(BlockIgniteEvent.IgniteCause.SPREAD));
        assertFalse(BreakPlace.isFireSpread(BlockIgniteEvent.IgniteCause.FIREBALL));
        assertFalse(BreakPlace.isFireSpread(BlockIgniteEvent.IgniteCause.EXPLOSION));
    }

    @Test
    void playerBlocksAlwaysTakePrecedenceOverMapProtection() {
        assertFalse(BreakPlace.isProtectedMapBlock(true, true, false));
        assertFalse(BreakPlace.isProtectedMapBlock(true, false, false));
        assertFalse(BreakPlace.isProtectedMapBlock(true, true, true));
    }

    @Test
    void originalMapBlocksStillRespectRegionsAndMapBreakRule() {
        assertTrue(BreakPlace.isProtectedMapBlock(false, true, true));
        assertTrue(BreakPlace.isProtectedMapBlock(false, false, false));
        assertFalse(BreakPlace.isProtectedMapBlock(false, false, true));
    }

    private static World world(String name) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "toString" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                });
    }
}
