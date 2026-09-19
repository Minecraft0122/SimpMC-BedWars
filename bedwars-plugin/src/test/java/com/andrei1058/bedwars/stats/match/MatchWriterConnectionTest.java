package com.andrei1058.bedwars.stats.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MatchWriterConnectionTest {
    @TempDir
    Path directory;

    @Test
    void reusesAnIdleSqliteConnectionAndReleasesTheWriteLockAfterCommit() throws Exception {
        MatchStatsDatabase database = MatchStatsDatabase.sqlite(directory.resolve("matches.db"));
        try (MatchWriterConnection writer = new MatchWriterConnection(database)) {
            AtomicReference<Connection> first = new AtomicReference<>();
            writer.withConnection(connection -> {
                first.set(connection);
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE sample (value INTEGER NOT NULL)");
                }
                return null;
            });

            AtomicReference<Connection> reused = new AtomicReference<>();
            writer.inTransaction(connection -> {
                reused.set(connection);
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO sample(value) VALUES (7)");
                }
                return null;
            });

            assertSame(first.get(), reused.get(), "SQLite writer connection should be reused");
            assertTrue(reused.get().getAutoCommit(), "commit must restore autocommit and release IMMEDIATE lock");
            try (Connection writerProbe = database.openConnection()) {
                writerProbe.setAutoCommit(false);
                try (Statement statement = writerProbe.createStatement()) {
                    statement.executeUpdate("INSERT INTO sample(value) VALUES (8)");
                }
                writerProbe.commit();
                writerProbe.setAutoCommit(true);
            }
        } finally {
            database.close();
        }
    }

    @Test
    void dropsAConnectionAfterAFailedTransaction() throws Exception {
        MatchStatsDatabase database = MatchStatsDatabase.sqlite(directory.resolve("failed.db"));
        try (MatchWriterConnection writer = new MatchWriterConnection(database)) {
            AtomicReference<Connection> failed = new AtomicReference<>();
            assertThrows(SQLException.class, () -> writer.inTransaction(connection -> {
                failed.set(connection);
                throw new SQLException("synthetic write failure");
            }));

            AtomicReference<Connection> replacement = new AtomicReference<>();
            writer.withConnection(connection -> {
                replacement.set(connection);
                return null;
            });
            assertNotSame(failed.get(), replacement.get(), "failed SQLite connection must not be cached");
        } finally {
            database.close();
        }
    }

    @Test
    void closeRejectsFurtherOperationsAndClosesAnIdleConnection() throws Exception {
        MatchStatsDatabase database = MatchStatsDatabase.sqlite(directory.resolve("closed.db"));
        MatchWriterConnection writer = new MatchWriterConnection(database);
        AtomicReference<Connection> connection = new AtomicReference<>();
        writer.withConnection(opened -> {
            connection.set(opened);
            return null;
        });
        writer.close();
        assertTrue(connection.get().isClosed());
        assertThrows(SQLException.class, () -> writer.withConnection(ignored -> null));
        database.close();
    }
}
