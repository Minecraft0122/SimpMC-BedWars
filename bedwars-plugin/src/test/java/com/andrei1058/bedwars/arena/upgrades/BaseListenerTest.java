package com.andrei1058.bedwars.arena.upgrades;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseListenerTest {

    @Test
    void crossWorldDeathLocationIsNotInsideAnArenaBase() {
        assertFalse(BaseListener.isInsideBase(
                new Location(world("lobby"), 0, 64, 0),
                new Location(world("arena"), 0, 64, 0),
                10D));
    }

    @Test
    void sameWorldLocationInsideRadiusStillMatches() {
        World arena = world("arena");
        assertTrue(BaseListener.isInsideBase(
                new Location(arena, 3, 64, 4),
                new Location(arena, 0, 64, 0),
                5D));
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
