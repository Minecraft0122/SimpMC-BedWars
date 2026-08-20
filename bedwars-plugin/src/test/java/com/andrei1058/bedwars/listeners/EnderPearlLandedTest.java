package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnderPearlLandedTest {

    @Test
    void cancelsAnArenaPearlTeleportAtTheVoidBoundary() {
        Player player = player();
        IArena arena = arena(-64);
        PlayerTeleportEvent event = new PlayerTeleportEvent(player,
                new Location(null, 0, 70, 0), new Location(null, 0, -64, 0),
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL);

        new EnderPearlLanded(ignored -> arena).onPearlTeleport(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void treatsTheConfiguredKillHeightAsVoid() {
        assertTrue(EnderPearlLanded.shouldCancelTeleport(
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL, -64, -64));
        assertTrue(EnderPearlLanded.shouldCancelTeleport(
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL, -65, -64));
    }

    @Test
    void permitsPearlsThatLandAboveTheVoidBoundary() {
        assertFalse(EnderPearlLanded.shouldCancelTeleport(
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL, -63, -64));
        assertFalse(EnderPearlLanded.shouldCancelTeleport(
                PlayerTeleportEvent.TeleportCause.ENDER_PEARL, 72, -64));
    }

    @Test
    void neverBlocksNonPearlTeleportsAtTheSameHeight() {
        assertFalse(EnderPearlLanded.shouldCancelTeleport(
                PlayerTeleportEvent.TeleportCause.PLUGIN, -80, -64));
    }

    private static Player player() {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "void-pearl-player";
                    default -> null;
                });
    }

    private static IArena arena(int killHeight) {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> method.getName().equals("getYKillHeight") ? killHeight : null);
    }
}
