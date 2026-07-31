package com.andrei1058.bedwars.api.arena;

import com.andrei1058.bedwars.api.arena.generator.IGenerator;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IArenaSnapshotDefaultsTest {

    @Test
    void collectionSnapshotsAreDetachedAndReadOnly() {
        List<Player> players = new ArrayList<>();
        List<Player> spectators = new ArrayList<>();
        List<ITeam> teams = new ArrayList<>();
        List<Block> signs = new ArrayList<>();
        List<IGenerator> generators = new ArrayList<>();
        IArena arena = (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class}, (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    return switch (method.getName()) {
                        case "getPlayers" -> players;
                        case "getSpectators" -> spectators;
                        case "getTeams" -> teams;
                        case "getSigns" -> signs;
                        case "getOreGenerators" -> generators;
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });

        List<Player> playerSnapshot = arena.getPlayersSnapshot();
        players.add(null);
        assertEquals(0, playerSnapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> playerSnapshot.clear());
        assertThrows(UnsupportedOperationException.class, () -> arena.getSpectatorsSnapshot().clear());
        assertThrows(UnsupportedOperationException.class, () -> arena.getTeamsSnapshot().clear());
        assertThrows(UnsupportedOperationException.class, () -> arena.getSignsSnapshot().clear());
        assertThrows(UnsupportedOperationException.class, () -> arena.getOreGeneratorsSnapshot().clear());
    }

    @Test
    void thirdPartyArenasKeepTheBedWars1058MinimumPlayerDefault() {
        IArena arena = (IArena) Proxy.newProxyInstance(IArena.class.getClassLoader(),
                new Class<?>[]{IArena.class}, (proxy, method, args) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    throw new UnsupportedOperationException(method.getName());
                });

        assertEquals(2, arena.getMinPlayers());
        assertEquals(1, arena.getMinInTeam());
    }
}
