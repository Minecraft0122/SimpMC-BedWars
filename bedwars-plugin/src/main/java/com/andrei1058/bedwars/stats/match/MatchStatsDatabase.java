package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.database.MySQL;
import com.andrei1058.bedwars.BedWars;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 对局记录的数据库连接入口；查询独立借还，SQLite 写线程复用专用连接。 */
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

    String storageIdentity() {
        return isSqlite() ? "sqlite:" + sqliteFile : Objects.toString(mysql.storageIdentity(), "mysql:unconfigured");
    }

    Path pendingWritesDirectory() {
        if (isSqlite()) return sqliteFile.resolveSibling(sqliteFile.getFileName() + ".pending");
        // 插件初始化前的单元测试不创建机器级共享目录。
        if (BedWars.plugin == null) return null;
        try {
            String target = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(storageIdentity().getBytes(StandardCharsets.UTF_8)));
            return BedWars.plugin.getDataFolder().toPath().resolve("Cache/match-pending").resolve(target);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
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

    /** SQLite 写连接由写入器关闭；MySQL 连接池归插件的旧统计数据库所有。 */
    @Override
    public void close() {
    }
}
