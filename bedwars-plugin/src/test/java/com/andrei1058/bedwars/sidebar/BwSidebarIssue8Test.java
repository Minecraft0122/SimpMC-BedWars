package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BwSidebarIssue8Test {

    @Test
    void arenaPlayerCountIsSafeWhenAStaleContextHasBeenCleared() {
        assertEquals("0", BwSidebar.arenaPlayerCount(null));

        IArena arena = (IArena) Proxy.newProxyInstance(
                IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> method.getName().equals("getPlayers")
                        ? List.<Player>of(player("one"), player("two"))
                        : unsupported(method.getName()));

        assertDoesNotThrow(() -> assertEquals("2", BwSidebar.arenaPlayerCount(arena)));
    }

    private static Player player(String name) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> method.getName().equals("getName")
                        ? name
                        : unsupported(method.getName()));
    }

    private static Object unsupported(String method) {
        throw new UnsupportedOperationException(method);
    }
}
