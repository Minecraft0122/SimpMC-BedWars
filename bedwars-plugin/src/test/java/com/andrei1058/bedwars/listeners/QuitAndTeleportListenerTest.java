package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuitAndTeleportListenerTest {

    @Test
    void kickedPlayerIsRemovedBeforeBeingMarkedAsAbandoned() {
        List<String> calls = new ArrayList<>();
        Player player = player();
        IArena arena = arena(calls);

        QuitAndTeleportListener.removeArenaPlayerOnQuit(
                arena, player, PlayerQuitEvent.QuitReason.KICKED);

        assertEquals(List.of("remove:true", "abandon"), calls);
    }

    @Test
    void recoverableDisconnectKeepsTheReconnectReservation() {
        List<String> calls = new ArrayList<>();

        QuitAndTeleportListener.removeArenaPlayerOnQuit(
                arena(calls), player(), PlayerQuitEvent.QuitReason.TIMED_OUT);

        assertEquals(List.of("remove:true"), calls);
    }

    private static IArena arena(List<String> calls) {
        return (IArena) Proxy.newProxyInstance(
                IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "removePlayer" -> {
                        calls.add("remove:" + args[1]);
                        yield null;
                    }
                    case "abandonGame" -> {
                        calls.add("abandon");
                        yield null;
                    }
                    case "toString" -> "arena";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player player() {
        UUID playerId = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> playerId;
                    case "toString" -> playerId.toString();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
