package com.andrei1058.bedwars.arena.matchmaking;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.tasks.StartingTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaInvitePolicyTest {

    @Test
    void acceptsLobbyAndDifferentPreGameArenaTargets() {
        IArena waiting = arena(GameState.waiting, 0, 4, 0);
        IArena otherWaiting = arena(GameState.waiting, 2, 4, 0);

        assertTrue(ArenaInvitePolicy.canInviteTarget(waiting, null, true));
        assertTrue(ArenaInvitePolicy.canInviteTarget(waiting, otherWaiting, false));
        assertFalse(ArenaInvitePolicy.canInviteTarget(waiting, waiting, false));
    }

    @Test
    void rejectsStartedFullAndLastSecondArenas() {
        IArena playing = arena(GameState.playing, 0, 4, 0);
        IArena full = arena(GameState.waiting, 4, 4, 0);
        IArena lastSecond = arena(GameState.starting, 2, 4, 1);
        IArena waiting = arena(GameState.waiting, 0, 4, 0);

        assertFalse(ArenaInvitePolicy.canAcceptPlayer(playing));
        assertFalse(ArenaInvitePolicy.hasRoom(full));
        assertFalse(ArenaInvitePolicy.canAcceptPlayer(lastSecond));
        assertFalse(ArenaInvitePolicy.canInviteTarget(waiting, lastSecond, false));
        assertTrue(ArenaInvitePolicy.canInviteTarget(waiting, full, false));
        assertTrue(ArenaInvitePolicy.canAcceptFrom(waiting, full, false));
        assertFalse(ArenaInvitePolicy.canAcceptFrom(waiting, lastSecond, false));
        assertFalse(ArenaInvitePolicy.canInviteTarget(lastSecond, null, true));
    }

    private static IArena arena(GameState state, int players, int maxPlayers, int countdown) {
        List<Object> members = new ArrayList<>();
        for (int index = 0; index < players; index++) members.add(new Object());
        StartingTask startingTask = countdown < 0 ? null : (StartingTask) Proxy.newProxyInstance(
                StartingTask.class.getClassLoader(), new Class<?>[]{StartingTask.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCountdown" -> countdown;
                    case "getBukkitTask" -> null;
                    case "getArena" -> proxy;
                    case "getTask" -> 0;
                    case "isSingleTeamDebugStart" -> false;
                    case "cancel" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getStatus" -> state;
                    case "getPlayers" -> members;
                    case "getMaxPlayers" -> maxPlayers;
                    case "getStartingTask" -> startingTask;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
