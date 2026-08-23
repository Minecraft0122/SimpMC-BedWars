package com.andrei1058.bedwars.listeners.dropshandler;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDropsTest {

    @Test
    void clearsAllInventoryForDeathsWithoutAKillerInsteadOfDroppingIt() {
        Player victim = playerWithoutWorldAccess();
        ITeam team = team(false);
        IArena arena = arenaWithoutPlayer();

        boolean handled = assertDoesNotThrow(() -> PlayerDrops.handlePlayerDrops(
                arena, victim, null, team, null, PlayerKillEvent.PlayerKillCause.UNKNOWN,
                List.of(), false));

        assertTrue(handled, "plugin-managed death drops must be cleared by the caller");
    }

    @Test
    void clearsFinalKillEnderChestWithoutSpawningGroundDrops() {
        AtomicBoolean enderChestCleared = new AtomicBoolean();
        Player victim = playerWithEnderChest(enderChestCleared);
        ITeam team = team(true);

        boolean handled = assertDoesNotThrow(() -> PlayerDrops.handlePlayerDrops(
                null, victim, null, team, null, PlayerKillEvent.PlayerKillCause.UNKNOWN_FINAL_KILL,
                List.of(), false));

        assertTrue(handled, "plugin-managed final-kill drops must be cleared by the caller");
        assertTrue(enderChestCleared.get(), "final-kill ender chest contents must be discarded");
    }

    private static IArena arenaWithoutPlayer() {
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isPlayer", "isReSpawning" -> false;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ITeam team(boolean bedDestroyed) {
        return (ITeam) Proxy.newProxyInstance(ITeam.class.getClassLoader(), new Class<?>[]{ITeam.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isBedDestroyed" -> bedDestroyed;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player playerWithoutWorldAccess() {
        return playerProxy((proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "getLocation", "getWorld" -> throw new AssertionError("death handler attempted a ground drop");
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Player playerWithEnderChest(AtomicBoolean cleared) {
        Inventory enderChest = (Inventory) Proxy.newProxyInstance(Inventory.class.getClassLoader(), new Class<?>[]{Inventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "clear" -> {
                        cleared.set(true);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return playerProxy((proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "getEnderChest" -> enderChest;
            case "getWorld", "getLocation" -> throw new AssertionError("final death attempted a ground drop");
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Player playerProxy(java.lang.reflect.InvocationHandler handler) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, handler);
    }
}
