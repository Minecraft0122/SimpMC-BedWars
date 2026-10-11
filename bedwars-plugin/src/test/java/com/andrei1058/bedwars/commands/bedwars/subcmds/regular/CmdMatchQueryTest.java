package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.stats.MatchHistory;
import com.andrei1058.bedwars.api.stats.MatchInfo;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.language.MatchQueryMessages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CmdMatchQueryTest {
    @Test
    void routesPersistentNumberAndCanonicalUuidWithoutNarrowing() {
        MatchHistory history = mock(MatchHistory.class);
        UUID uuid = UUID.randomUUID();
        when(history.findMatch(4_000_000_000L)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(history.findMatch(uuid)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        assertTrue(CmdMatchQuery.findMatch(history, "4000000000").join().isEmpty());
        assertTrue(CmdMatchQuery.findMatch(history, uuid.toString().toUpperCase()).join().isEmpty());
        verify(history).findMatch(4_000_000_000L);
        verify(history).findMatch(uuid);
    }

    @Test
    void rejectsInvalidIdentifiersBeforeDatabaseWork() {
        MatchHistory history = mock(MatchHistory.class);
        for (String value : new String[]{"0", "-1", "1-1-1-1-1", "map-1", "9999999999999999999999"}) {
            assertThrows(IllegalArgumentException.class, () -> CmdMatchQuery.findMatch(history, value), value);
        }
        verifyNoInteractions(history);
    }

    @Test
    void boundsPageArithmetic() {
        assertEquals(0, CmdMatchQuery.pageOffset("1"));
        assertEquals(20, CmdMatchQuery.pageOffset("3"));
        for (String value : new String[]{"0", "-1", "one", "2147483647"}) {
            assertThrows(IllegalArgumentException.class, () -> CmdMatchQuery.pageOffset(value));
        }
    }

    @Test
    void offlineUuidNeverRequiresAnOnlinePlayerAndNameLookupIsAsync() {
        MatchHistory history = mock(MatchHistory.class);
        CommandSender sender = mock(CommandSender.class);
        UUID uuid = UUID.randomUUID();
        try (var bukkit = mockStatic(Bukkit.class)) {
            assertEquals(uuid, CmdMatchQuery.playerId(uuid.toString(), sender, history).join());
            bukkit.verifyNoInteractions();
            CompletableFuture<Optional<UUID>> lookup = new CompletableFuture<>();
            when(history.findPlayerUuid("Alice")).thenReturn(lookup);
            var result = CmdMatchQuery.playerId("Alice", sender, history);
            assertFalse(result.isDone());
            lookup.complete(Optional.of(uuid));
            assertEquals(uuid, result.join());
        }
    }

    @Test
    void allHistoryUsesGlobalPagingNotAPlayerNamedAll() {
        ParentCommand parent = mock(ParentCommand.class);
        when(parent.getName()).thenReturn("bw");
        CmdMatchQuery command = new CmdMatchQuery(parent, "history");
        MatchHistory history = mock(MatchHistory.class);
        when(history.getMatches(10, 20)).thenReturn(CompletableFuture.completedFuture(java.util.List.of()));
        var lines = command.history(new String[]{"all", "3"}, mock(CommandSender.class), history).join();
        assertTrue(lines.getFirst().contains("全部记录"));
        verify(history).getMatches(10, 20);
        verifyNoMoreInteractions(history);
    }

    @Test
    void allHistoryShowsOlderNumbersAboveNewerNumbersWithoutChangingApiPage() {
        ParentCommand parent = mock(ParentCommand.class);
        when(parent.getName()).thenReturn("bw");
        CmdMatchQuery command = new CmdMatchQuery(parent, "history");
        MatchHistory history = mock(MatchHistory.class);
        List<MatchInfo> descendingPage = java.util.stream.LongStream.iterate(5, number -> number - 1)
                .limit(5).mapToObj(this::match).toList();
        when(history.getMatches(10, 0)).thenReturn(CompletableFuture.completedFuture(descendingPage));

        List<String> lines = command.history(new String[]{"all"}, mock(CommandSender.class), history,
                MatchQueryMessages.defaults(false)).join();

        assertTrue(lines.getFirst().contains("All matches"));
        for (int index = 1; index <= 5; index++) {
            assertTrue(lines.get(index).contains("#" + index + " "), lines.get(index));
        }
        assertEquals(List.of(5L, 4L, 3L, 2L, 1L),
                descendingPage.stream().map(MatchInfo::matchNumber).toList());
        verify(history).getMatches(10, 0);
    }

    @Test
    void playerHistoryKeepsItsExistingNewestFirstOrder() {
        ParentCommand parent = mock(ParentCommand.class);
        when(parent.getName()).thenReturn("bw");
        CmdMatchQuery command = new CmdMatchQuery(parent, "history");
        MatchHistory history = mock(MatchHistory.class);
        org.bukkit.entity.Player sender = mock(org.bukkit.entity.Player.class);
        UUID playerId = UUID.randomUUID();
        when(sender.getUniqueId()).thenReturn(playerId);
        when(history.getPlayerMatches(playerId, 10, 0)).thenReturn(
                CompletableFuture.completedFuture(List.of(match(2), match(1))));

        List<String> lines = command.history(new String[0], sender, history,
                MatchQueryMessages.defaults(false)).join();

        assertTrue(lines.get(1).contains("#2 "));
        assertTrue(lines.get(2).contains("#1 "));
    }

    private MatchInfo match(long number) {
        return new MatchInfo(number, UUID.randomUUID(), "Map", "Map", "solo", "local",
                "FINISHED", Instant.parse("2026-10-10T00:00:00Z"), Instant.parse("2026-10-10T00:05:00Z"), null);
    }
}
