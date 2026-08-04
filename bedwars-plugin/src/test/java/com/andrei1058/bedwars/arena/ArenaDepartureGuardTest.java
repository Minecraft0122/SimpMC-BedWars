package com.andrei1058.bedwars.arena;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDepartureGuardTest {

    @Test
    void successfulRejoinClearsThePreviousPlayerWrapperByUuid() {
        UUID playerId = UUID.randomUUID();
        Player disconnected = player(playerId, "disconnected");
        Player reconnected = player(playerId, "reconnected");
        List<Player> leaving = new ArrayList<>();

        assertTrue(ArenaDepartureGuard.tryBegin(leaving, disconnected));
        assertFalse(ArenaDepartureGuard.tryBegin(leaving, reconnected));
        assertSame(disconnected, leaving.getFirst());

        ArenaDepartureGuard.restore(leaving, reconnected);

        assertTrue(leaving.isEmpty());
        assertTrue(ArenaDepartureGuard.tryBegin(leaving, reconnected));
        assertSame(reconnected, leaving.getFirst());
    }

    private static Player player(UUID playerId, String label) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> playerId;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> label;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
