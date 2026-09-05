/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.database;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;

/**
 * Serializes schema upgrades shared by Lobby and Arena nodes.
 *
 * <p>The named lock is connection-scoped and does not lock application
 * tables. A short session metadata-lock timeout also prevents an online DDL
 * request from waiting behind a long transaction while blocking later DML.</p>
 */
public final class MySqlSchemaCoordinator {

    private static final int MAX_LOCK_NAME_LENGTH = 64;

    private MySqlSchemaCoordinator() {
    }

    public static Lease acquire(Connection connection, String component,
                                int coordinationWaitSeconds,
                                int metadataLockWaitSeconds) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        if (coordinationWaitSeconds < 0) {
            throw new IllegalArgumentException("coordinationWaitSeconds must not be negative");
        }
        if (metadataLockWaitSeconds < 1) {
            throw new IllegalArgumentException("metadataLockWaitSeconds must be positive");
        }

        String databaseName = currentDatabase(connection);
        String lockName = buildLockName(component, databaseName);
        if (!acquireNamedLock(connection, lockName, coordinationWaitSeconds)) {
            throw new SQLTransientException("Timed out waiting for MySQL schema coordinator " + lockName);
        }

        long previousMetadataTimeout = -1L;
        try {
            previousMetadataTimeout = readMetadataLockTimeout(connection);
            setMetadataLockTimeout(connection, metadataLockWaitSeconds);
            return new Lease(connection, lockName, previousMetadataTimeout);
        } catch (SQLException exception) {
            try {
                releaseNamedLock(connection, lockName);
            } catch (SQLException releaseFailure) {
                exception.addSuppressed(releaseFailure);
            }
            throw exception;
        }
    }

    public static boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND TABLE_TYPE='BASE TABLE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    static String buildLockName(String component, String databaseName) {
        String normalizedComponent = component == null || component.isBlank() ? "schema" : component.trim();
        String normalizedDatabase = databaseName == null || databaseName.isBlank() ? "unknown" : databaseName.trim();
        String readable = normalizedComponent + ':' + normalizedDatabase;
        String fingerprint = UUID.nameUUIDFromBytes(readable.getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        String componentSlug = normalizedComponent.replaceAll("[^A-Za-z0-9_-]", "-");
        if (componentSlug.length() > 18) componentSlug = componentSlug.substring(0, 18);
        String lockName = "simpmc-bw:" + componentSlug + ':' + fingerprint;
        return lockName.substring(0, Math.min(MAX_LOCK_NAME_LENGTH, lockName.length()));
    }

    private static String currentDatabase(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT DATABASE()")) {
            if (!result.next()) throw new SQLException("MySQL did not return the current database");
            String databaseName = result.getString(1);
            if (databaseName == null || databaseName.isBlank()) {
                throw new SQLException("No MySQL database is selected");
            }
            return databaseName;
        }
    }

    private static boolean acquireNamedLock(Connection connection, String lockName,
                                            int waitSeconds) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, lockName);
            statement.setInt(2, waitSeconds);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return false;
                int acquired = result.getInt(1);
                return !result.wasNull() && acquired == 1;
            }
        }
    }

    private static long readMetadataLockTimeout(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT @@SESSION.lock_wait_timeout")) {
            if (!result.next()) throw new SQLException("MySQL did not return lock_wait_timeout");
            return result.getLong(1);
        }
    }

    private static void setMetadataLockTimeout(Connection connection, long seconds) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET SESSION lock_wait_timeout=" + Math.max(1L, seconds));
        }
    }

    private static void releaseNamedLock(Connection connection, String lockName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, lockName);
            statement.executeQuery();
        }
    }

    public static final class Lease implements AutoCloseable {

        private final Connection connection;
        private final String lockName;
        private final long previousMetadataTimeout;
        private boolean closed;

        private Lease(Connection connection, String lockName, long previousMetadataTimeout) {
            this.connection = connection;
            this.lockName = lockName;
            this.previousMetadataTimeout = previousMetadataTimeout;
        }

        @Override
        public void close() throws SQLException {
            if (closed) return;
            closed = true;
            SQLException failure = null;
            try {
                if (previousMetadataTimeout >= 1L) {
                    setMetadataLockTimeout(connection, previousMetadataTimeout);
                }
            } catch (SQLException exception) {
                failure = exception;
            }
            try {
                releaseNamedLock(connection, lockName);
            } catch (SQLException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
            if (failure != null) throw failure;
        }
    }
}
