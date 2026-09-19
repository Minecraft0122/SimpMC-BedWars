package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MatchHistoryServiceTest {
    @TempDir Path directory;

    @Test
    void propagatesStorageErrorsInsteadOfInventingAnEmptyHistory() {
        try (MatchHistoryService service = new MatchHistoryService(
                MatchStatsDatabase.sqlite(directory.resolve("not-initialized.db")), null)) {
            ExecutionException exception = assertThrows(ExecutionException.class,
                    () -> service.getPlayerTotals(UUID.randomUUID()).get(5, TimeUnit.SECONDS));
            assertInstanceOf(SQLException.class, exception.getCause());
            assertTrue(service.getCurrentMatch(null).isEmpty());
        }
    }

    @Test
    void closedQueriesFailAndPaginationIsValidated() {
        MatchHistoryService service = new MatchHistoryService(
                MatchStatsDatabase.sqlite(directory.resolve("closed.db")), null);
        service.close();
        assertFalse(service.isEnabled());
        assertTrue(service.findMatch(1).isCompletedExceptionally());
        assertTrue(service.getPlayerMatches(UUID.randomUUID(), 101, 0).isCompletedExceptionally());
        assertTrue(service.findMatch(0).isCompletedExceptionally());
    }
}
