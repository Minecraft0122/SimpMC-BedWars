package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.language.MatchmakingMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/** Pure grouping and formatting for the custom root /list command. */
public final class PlayerListFormatter {
    private PlayerListFormatter() { }

    public static <T> List<Group> groupPlayers(Collection<T> players,
                                                Function<T, String> playerName,
                                                Function<T, String> arenaName) {
        List<String> lobby = new ArrayList<>();
        Map<String, List<String>> arenas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (T player : players) {
            String name = playerName.apply(player);
            String arena = arenaName.apply(player);
            if (arena == null || arena.isBlank()) {
                lobby.add(name);
            } else {
                arenas.computeIfAbsent(arena, ignored -> new ArrayList<>()).add(name);
            }
        }

        Comparator<String> playerOrder = String.CASE_INSENSITIVE_ORDER;
        lobby.sort(playerOrder);
        List<Group> result = new ArrayList<>();
        result.add(new Group(null, lobby));
        arenas.forEach((name, arenaPlayers) -> {
            arenaPlayers.sort(playerOrder);
            result.add(new Group(name, arenaPlayers));
        });
        return List.copyOf(result);
    }

    public static List<String> format(List<Group> groups, int online, int max,
                                      MatchmakingMessages.Snapshot messages) {
        List<String> lines = new ArrayList<>();
        lines.add(messages.text(MatchmakingMessages.Key.LIST_HEADER,
                "{online}", String.valueOf(online), "{max}", String.valueOf(max)));
        for (Group group : groups) {
            String names = group.players().isEmpty()
                    ? messages.text(MatchmakingMessages.Key.LIST_EMPTY)
                    : String.join(", ", group.players());
            MatchmakingMessages.Key key = group.arenaName() == null
                    ? MatchmakingMessages.Key.LIST_LOBBY : MatchmakingMessages.Key.LIST_ARENA;
            lines.add(messages.text(key,
                    "{arena}", group.arenaName() == null ? "" : group.arenaName(),
                    "{count}", String.valueOf(group.players().size()),
                    "{players}", names));
        }
        return List.copyOf(lines);
    }

    public record Group(String arenaName, List<String> players) {
        public Group {
            players = List.copyOf(players);
        }
    }
}
