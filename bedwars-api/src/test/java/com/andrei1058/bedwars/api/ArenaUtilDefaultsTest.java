package com.andrei1058.bedwars.api;

import com.andrei1058.bedwars.api.arena.IArena;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaUtilDefaultsTest {

    @Test
    void exposesNullSafeLookupsAndImmutableSnapshots() {
        IArena arena = (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class}, (proxy, method, args) -> null);
        LinkedList<IArena> registry = new LinkedList<>(List.of(arena));

        BedWars.ArenaUtil util = (BedWars.ArenaUtil) Proxy.newProxyInstance(
                BedWars.ArenaUtil.class.getClassLoader(),
                new Class<?>[]{BedWars.ArenaUtil.class},
                (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    return switch (method.getName()) {
                        case "getArenas" -> registry;
                        case "getArenaByName" -> "demo".equals(args[0]) ? arena : null;
                        case "getArenaByIdentifier", "getArenaByPlayer" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                }
        );

        assertTrue(util.findArenaByName("demo").isPresent());
        assertTrue(util.findArenaByName("missing").isEmpty());
        List<IArena> snapshot = util.getArenasSnapshot();
        assertEquals(List.of(arena), snapshot);
        registry.clear();
        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(arena));
    }
}
