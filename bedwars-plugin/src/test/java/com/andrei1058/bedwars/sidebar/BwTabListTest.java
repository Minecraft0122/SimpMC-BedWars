package com.andrei1058.bedwars.sidebar;

import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertSame;

class BwTabListTest {

    @Test
    void activePlayerNameUsesItsOwnTeamColor() {
        ITeam redTeam = (ITeam) Proxy.newProxyInstance(
                ITeam.class.getClassLoader(),
                new Class<?>[]{ITeam.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getColor")) return TeamColor.RED;
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        assertSame(ChatColor.RED, BwTabList.getPlayerListColor(redTeam));
        assertSame(ChatColor.WHITE, BwTabList.getPlayerListColor(null));
    }
}
