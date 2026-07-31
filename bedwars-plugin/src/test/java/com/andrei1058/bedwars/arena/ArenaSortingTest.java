package com.andrei1058.bedwars.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArenaSortingTest {

    @Test
    void prioritizesJoinableStatesThenPopulationAndName() {
        IArena waitingSmall = arena("beta", GameState.waiting, 1);
        IArena restarting = arena("reset", GameState.restarting, 8);
        IArena startingSmall = arena("alpha", GameState.starting, 2);
        IArena waitingLarge = arena("gamma", GameState.waiting, 4);
        IArena startingLarge = arena("delta", GameState.starting, 5);
        IArena playing = arena("play", GameState.playing, 8);

        assertEquals(List.of("delta", "alpha", "gamma", "beta", "play", "reset"),
                names(ArenaSorting.sorted(List.of(
                        restarting, waitingSmall, playing, startingSmall, waitingLarge, startingLarge))));
    }

    @Test
    void providesAComparatorContractForEqualRestartingArenas() {
        IArena first = arena("a", GameState.restarting, 0);
        IArena second = arena("b", GameState.restarting, 0);

        assertEquals(List.of("a", "b"), names(ArenaSorting.sorted(List.of(second, first))));
    }

    private IArena arena(String name, GameState state, int players) {
        List<Object> playerCount = new ArrayList<>();
        for (int index = 0; index < players; index++) playerCount.add(new Object());
        return (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(), new Class<?>[]{IArena.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getArenaName" -> name;
                    case "getStatus" -> state;
                    case "getPlayers" -> playerCount;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private List<String> names(List<IArena> arenas) {
        return arenas.stream().map(IArena::getArenaName).toList();
    }
}
