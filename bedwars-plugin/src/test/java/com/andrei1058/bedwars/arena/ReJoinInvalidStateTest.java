package com.andrei1058.bedwars.arena;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReJoinInvalidStateTest {

    @Test
    void rejectsMismatchedPlayerAndDetachedReservationData() {
        UUID playerId = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> method.getName().equals("getUniqueId")
                        ? playerId
                        : defaultValue(method.getReturnType()));

        // A stale callback must match the reservation UUID and an exact active
        // object; UUID equality alone must not revive an old lifecycle.
        assertTrue(ReJoin.belongsToPlayer(playerId, player));
        assertFalse(ReJoin.belongsToPlayer(UUID.randomUUID(), player));
        assertFalse(ReJoin.belongsToPlayer(null, player));
        assertFalse(ReJoin.isActiveReservation(List.of(), null));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) return null;
        if (returnType == boolean.class) return false;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0F;
        if (returnType == double.class) return 0D;
        if (returnType == char.class) return '\0';
        return null;
    }
}
