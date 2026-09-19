package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.database.MySQL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

/** 对局记录的数据库连接入口；每次操作都独立借用并关闭连接。 */
public final class MatchStatsDatabase implements AutoCloseable {
    private final MySQL mysql;
    private final Path sqliteFile;
    private boolean sqliteInitialized;

    private MatchStatsDatabase(MySQL mysql, Path sqliteFile) {
        this.mysql = mysql;
        this.sqliteFile = sqliteFile;
    }

    public static MatchStatsDatabase mysql(MySQL database) {
        return new MatchStatsDatabase(Objects.requireNonNull(database, "database"), null);
    }

    /** 使用独立的本地对局文件，不占用旧统计系统的 shop.db 连接。 */
    public static MatchStatsDatabase sqlite(Path file) {
        return new MatchStatsDatabase(null, Objects.requireNonNull(file, "file").toAbsolutePath().normalize());
    }

    public boolean isSqlite() {
        return sqliteFile != null;
    }

    public Connection openConnection() throws SQLException {
        if (!isSqlite()) return mysql.openConnection();
        initializeSqlite();
        return openSqliteConnection();
    }

    private synchronized void initializeSqlite() throws SQLException {
        if (sqliteInitialized) return;
        try {
            Files.createDirectories(sqliteFile.getParent());
            Class.forName("org.sqlite.JDBC");
        } catch (IOException | ClassNotFoundException exception) {
            throw new SQLException("无法初始化本地对局数据库 " + sqliteFile, exception);
        }
        try (Connection connection = openSqliteConnection(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
        }
        sqliteInitialized = true;
    }

    private Connection openSqliteConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("busy_timeout", "5000");
        // 事务开始即获取写锁，避免先读取再升级写锁时丢失并发结算。
        properties.setProperty("transaction_mode", "IMMEDIATE");
        return DriverManager.getConnection("jdbc:sqlite:" + sqliteFile, properties);
    }

    /** 连接随各次操作关闭；MySQL 连接池归插件的旧统计数据库所有。 */
    @Override
    public void close() {
    }
}
