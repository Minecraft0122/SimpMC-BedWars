package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReJoinModeOwnershipTest {

    @Test
    void delegatesModeAndFlightTransitionsToTheArenaRespawnLifecycle() {
        AtomicInteger arenaRejoins = new AtomicInteger();
        Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setGameMode")
                            || method.getName().equals("setAllowFlight")
                            || method.getName().equals("setFlying")) {
                        throw new AssertionError("ReJoin must not own player mode or flight state");
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        IArena arena = (IArena) Proxy.newProxyInstance(
                IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("reJoin")) {
                        arenaRejoins.incrementAndGet();
                        assertSame(player, args[0]);
                        return true;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        assertTrue(ReJoin.resumeThroughArenaLifecycle(arena, player));
        assertEquals(1, arenaRejoins.get());
    }
}
