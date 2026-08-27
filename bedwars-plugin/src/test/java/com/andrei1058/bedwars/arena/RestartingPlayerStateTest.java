package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartingPlayerStateTest {

    @Test
    void closesRespawnAndFirstPersonModesBeforeGivingReturnItems() {
        PlayerState winnerState = new PlayerState("Winner", GameMode.SPECTATOR);
        PlayerState spectatorState = new PlayerState("Spectator", GameMode.SPECTATOR);
        Player winner = player(winnerState);
        Player spectator = player(spectatorState);
        ConcurrentHashMap<Player, Integer> respawns = new ConcurrentHashMap<>();
        respawns.put(winner, 3);
        ConcurrentHashMap<Player, Integer> showTime = new ConcurrentHashMap<>();
        showTime.put(winner, 5);
        List<Player> itemRecipients = new ArrayList<>();
        IArena arena = arena(List.of(winner), List.of(spectator), respawns, showTime,
                spectator, itemRecipients);

        RestartingPlayerState.prepare(arena, (preparedArena, preparedPlayer) -> {
            assertSame(arena, preparedArena);
            assertSame(winner, preparedPlayer);
            winnerState.invisibilityRemoved = true;
            preparedArena.getShowTime().remove(preparedPlayer);
        });

        assertTrue(respawns.isEmpty());
        assertTrue(showTime.isEmpty());
        assertInteractiveWinner(winnerState);
        assertInteractiveSpectator(spectatorState);
        assertEquals(List.of(winner, spectator), itemRecipients);
    }

    @Test
    void stillPreparesReturnItemWhenNoActivePlayerRemains() {
        PlayerState spectatorState = new PlayerState("Spectator", GameMode.ADVENTURE);
        Player spectator = player(spectatorState);
        List<Player> itemRecipients = new ArrayList<>();
        IArena arena = arena(List.of(), List.of(spectator), new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                spectator, itemRecipients);

        RestartingPlayerState.prepare(arena);

        assertEquals(List.of(spectator), itemRecipients);
        assertInteractiveSpectator(spectatorState);
    }

    @Test
    void clearsTrackedInvisibilityForAWinningPlayerWhoWasNotRespawning() {
        PlayerState winnerState = new PlayerState("Winner", GameMode.SPECTATOR);
        Player winner = player(winnerState);
        ConcurrentHashMap<Player, Integer> showTime = new ConcurrentHashMap<>();
        showTime.put(winner, 12);
        List<Player> itemRecipients = new ArrayList<>();
        IArena arena = arena(List.of(winner), List.of(), new ConcurrentHashMap<>(), showTime,
                null, itemRecipients);

        RestartingPlayerState.prepare(arena, (preparedArena, preparedPlayer) -> {
            winnerState.invisibilityRemoved = true;
            preparedArena.getShowTime().remove(preparedPlayer);
        });

        assertTrue(showTime.isEmpty());
        assertInteractiveWinner(winnerState);
        assertEquals(List.of(winner), itemRecipients);
    }

    private static void assertInteractiveWinner(PlayerState state) {
        assertEquals(GameMode.ADVENTURE, state.gameMode);
        assertTrue(state.canPickupItems);
        assertFalse(state.allowFlight);
        assertFalse(state.flying);
        assertEquals(Boolean.TRUE, state.collidable);
        assertTrue(state.invisibilityRemoved);
    }

    private static void assertInteractiveSpectator(PlayerState state) {
        assertEquals(GameMode.ADVENTURE, state.gameMode);
        assertTrue(state.canPickupItems);
        assertTrue(state.allowFlight);
        assertTrue(state.flying);
        assertEquals(Boolean.FALSE, state.collidable);
    }

    private static IArena arena(List<Player> players, List<Player> spectators,
                                ConcurrentHashMap<Player, Integer> respawns,
                                ConcurrentHashMap<Player, Integer> showTime,
                                Player spectator, List<Player> itemRecipients) {
        return (IArena) Proxy.newProxyInstance(
                IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPlayers" -> players;
                    case "getSpectators" -> spectators;
                    case "getPlayersSnapshot" -> players;
                    case "getSpectatorsSnapshot" -> spectators;
                    case "getRespawnSessions" -> respawns;
                    case "getShowTime" -> showTime;
                    case "isSpectator" -> args[0] == spectator;
                    case "sendSpectatorCommandItems" -> {
                        itemRecipients.add((Player) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Player player(PlayerState state) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> state.uniqueId;
                    case "getName" -> state.name;
                    case "isOnline" -> true;
                    case "getGameMode" -> state.gameMode;
                    case "setGameMode" -> {
                        state.gameMode = (GameMode) args[0];
                        yield null;
                    }
                    case "setCanPickupItems" -> {
                        state.canPickupItems = (boolean) args[0];
                        yield null;
                    }
                    case "setAllowFlight" -> {
                        state.allowFlight = (boolean) args[0];
                        yield null;
                    }
                    case "setFlying" -> {
                        state.flying = (boolean) args[0];
                        yield null;
                    }
                    case "setCollidable" -> {
                        state.collidable = (boolean) args[0];
                        yield null;
                    }
                    case "hashCode" -> state.uniqueId.hashCode();
                    case "equals" -> proxy == args[0];
                    case "toString" -> state.name;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class PlayerState {
        private final String name;
        private final UUID uniqueId;
        private GameMode gameMode;
        private boolean canPickupItems;
        private boolean allowFlight;
        private boolean flying;
        private Boolean collidable;
        private boolean invisibilityRemoved;

        private PlayerState(String name, GameMode gameMode) {
            this.name = name;
            this.uniqueId = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
            this.gameMode = gameMode;
        }
    }
}
