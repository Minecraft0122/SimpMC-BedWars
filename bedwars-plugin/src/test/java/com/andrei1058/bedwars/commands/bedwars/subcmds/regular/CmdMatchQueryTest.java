package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.api.stats.MatchHistory;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
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
}
