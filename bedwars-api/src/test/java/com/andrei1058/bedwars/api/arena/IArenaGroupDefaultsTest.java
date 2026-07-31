package com.andrei1058.bedwars.api.arena;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IArenaGroupDefaultsTest {

    @Test
    void keepsPublishedMultiGroupMethodsAsSingleGroupCompatibilityBridges() {
        String[] primary = {"Solo"};
        IArena arena = (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class}, (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    return switch (method.getName()) {
                        case "getGroup" -> primary[0];
                        case "setGroup" -> {
                            primary[0] = (String) args[0];
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });

        assertEquals(List.of("Solo"), arena.getGroups());
        assertTrue(arena.isInGroup("solo"));
        assertFalse(arena.isInGroup("Doubles"));

        arena.setGroups(List.of("Doubles", "Featured"));
        assertEquals("Doubles", arena.getGroup());
        assertEquals(List.of("Doubles"), arena.getGroups());
        assertFalse(arena.isInGroup("Featured"));

        arena.setGroups(List.of(" ", "Featured"));
        assertEquals("Featured", arena.getGroup());
    }
}
