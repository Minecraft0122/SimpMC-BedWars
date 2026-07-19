package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmdUpgradesTest {

    @Test
    void rejectsWaitingPlayersWithoutAnAssignedTeam() {
        assertFalse(CmdUpgrades.canOpenMenu(GameState.waiting, true, null, null));
        assertFalse(CmdUpgrades.canOpenMenu(GameState.playing, true, null, null));
    }

    @Test
    void onlyOpensNearTheTeamUpgradeNpcInTheSameWorld() {
        World arena = world("arena");
        Location upgrades = new Location(arena, 10, 64, 10);
        ITeam team = team(upgrades);

        assertTrue(CmdUpgrades.canOpenMenu(GameState.playing, true, team,
                new Location(arena, 12, 64, 10)));
        assertFalse(CmdUpgrades.canOpenMenu(GameState.playing, true, team,
                new Location(world("lobby"), 12, 64, 10)));
        assertFalse(CmdUpgrades.canOpenMenu(GameState.playing, false, team,
                new Location(arena, 12, 64, 10)));
    }

    private static ITeam team(Location upgrades) {
        return (ITeam) Proxy.newProxyInstance(ITeam.class.getClassLoader(), new Class<?>[]{ITeam.class},
                (proxy, method, args) -> method.getName().equals("getTeamUpgrades") ? upgrades : null);
    }

    private static World world(String name) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName", "toString" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                });
    }
}
