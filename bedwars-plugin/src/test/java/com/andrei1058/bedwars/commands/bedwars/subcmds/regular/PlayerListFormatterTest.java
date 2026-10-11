package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.language.MatchmakingMessages;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerListFormatterTest {

    @Test
    void groupsLobbyFirstAndArenaPlayersBySortedArenaAndPlayerName() {
        List<OnlinePlayer> online = List.of(
                new OnlinePlayer("zeta", null),
                new OnlinePlayer("Bob", "MapB"),
                new OnlinePlayer("alice", "mapA"),
                new OnlinePlayer("Alice2", "MapA"));

        List<PlayerListFormatter.Group> groups = PlayerListFormatter.groupPlayers(online,
                OnlinePlayer::name, OnlinePlayer::arena);

        assertEquals(List.of(
                new PlayerListFormatter.Group(null, List.of("zeta")),
                new PlayerListFormatter.Group("mapA", List.of("alice", "Alice2")),
                new PlayerListFormatter.Group("MapB", List.of("Bob"))), groups);
    }

    @Test
    void formatsSeparateLocalizedLobbyAndArenaRows() {
        List<PlayerListFormatter.Group> groups = List.of(
                new PlayerListFormatter.Group(null, List.of("Ari", "Bo")),
                new PlayerListFormatter.Group("MapA", List.of("Cy")));

        assertEquals(List.of(
                "&6在线玩家 &7(3/100)",
                "&e大厅 &7(2)&f：Ari, Bo",
                "&b竞技场 MapA &7(1)&f：Cy"),
                PlayerListFormatter.format(groups, 3, 100, MatchmakingMessages.defaults(true)));
    }

    private record OnlinePlayer(String name, String arena) { }
}
