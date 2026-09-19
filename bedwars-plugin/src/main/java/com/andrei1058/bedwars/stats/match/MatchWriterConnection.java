package com.andrei1058.bedwars.stats.match;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** 单个统计写入线程的连接入口；SQLite 复用空闲连接，MySQL 每次向连接池借还。 */
final class MatchWriterConnection implements AutoCloseable {
    private final MatchStatsDatabase database;
    private final boolean sqlite;
    private Connection idleConnection;
    private boolean inUse;
    private boolean closed;

    MatchWriterConnection(MatchStatsDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.sqlite = database.isSqlite();
    }

    /** 执行初始化或恢复操作；回调自行管理的事务必须在返回前完成。 */
    <T> T withConnection(SqlOperation<T> operation) throws SQLException {
        return execute(operation, false);
    }

    /** 成功时提交，失败时回滚；只有提交及连接清理均完成后才返回结果。 */
    <T> T inTransaction(SqlOperation<T> operation) throws SQLException {
        return execute(operation, true);
    }

    private <T> T execute(SqlOperation<T> operation, boolean transaction) throws SQLException {
        Objects.requireNonNull(operation, "operation");
        Connection connection;
        synchronized (this) {
            if (closed) throw new SQLException("对局写连接已关闭");
            if (inUse) throw new IllegalStateException("对局写连接只能由单个写入任务使用");
            inUse = true;
            connection = idleConnection;
            idleConnection = null;
        }
        Throwable failure = null;
        try {
            if (connection == null || connection.isClosed()) connection = database.openConnection();
            if (transaction) connection.setAutoCommit(false);
            T result = operation.execute(connection);
            if (transaction) connection.commit();
            if (!connection.getAutoCommit()) {
                // sqlite-jdbc commit() immediately opens the next IMMEDIATE
                // transaction. Restore autocommit before caching the connection
                // so an idle writer does not keep SQLite's write lock.
                if (!transaction) connection.rollback();
                connection.setAutoCommit(true);
            }
            return result;
        } catch (SQLException | RuntimeException | Error exception) {
            failure = exception;
            if (connection != null) {
                try {
                    if (!connection.getAutoCommit()) connection.rollback();
                } catch (SQLException | RuntimeException rollback) {
                    exception.addSuppressed(rollback);
                }
            }
            throw exception;
        } finally {
            boolean keep;
            synchronized (this) {
                keep = sqlite && failure == null && connection != null && !closed;
                if (keep) idleConnection = connection;
                inUse = false;
            }
            if (!keep && connection != null) {
                try {
                    connection.close();
                } catch (SQLException | RuntimeException close) {
                    if (failure != null) failure.addSuppressed(close);
                    else throw close;
                }
            }
        }
    }

    /** 关闭空闲连接；若操作仍在进行，由该操作的 finally 归还时关闭。 */
    @Override
    public void close() throws SQLException {
        Connection connection;
        synchronized (this) {
            closed = true;
            connection = idleConnection;
            idleConnection = null;
        }
        if (connection != null) connection.close();
    }

    @FunctionalInterface
    interface SqlOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
}
