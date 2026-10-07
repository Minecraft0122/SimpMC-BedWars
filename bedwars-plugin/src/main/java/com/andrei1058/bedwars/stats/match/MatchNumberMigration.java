package com.andrei1058.bedwars.stats.match;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** 一次性压紧历史编号；UUID 和统计不变，原编号永久保存在映射表中。 */
final class MatchNumberMigration {
    static final String ID = "dense-history-numbers-v1";

    private MatchNumberMigration() { }

    static void migrate(Connection connection, boolean sqlite) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("历史编号迁移需要独立的自动提交连接");
        try (Statement ddl = connection.createStatement()) {
            ddl.executeUpdate("CREATE TABLE IF NOT EXISTS bw_match_number_migration ("
                    + "match_uuid VARCHAR(36) PRIMARY KEY, old_match_no BIGINT NOT NULL, new_match_no BIGINT NOT NULL)"
                    + (sqlite ? "" : " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"));
        }
        connection.setAutoCommit(false);
        try {
            // 与新版本写入器共用序列锁；多子服同时启动时只允许一个迁移者。
            try (Statement lock = connection.createStatement(); ResultSet row = lock.executeQuery(
                    "SELECT last_match_no FROM bw_match_number_sequence WHERE sequence_id=1"
                            + (sqlite ? "" : " FOR UPDATE"))) {
                if (!row.next()) throw new SQLException("历史编号迁移前必须初始化序列");
            }
            try (PreparedStatement check = connection.prepareStatement(
                    "SELECT migration_id FROM bw_match_schema WHERE migration_id=?" + (sqlite ? "" : " FOR UPDATE"))) {
                check.setString(1, ID);
                try (ResultSet row = check.executeQuery()) {
                    if (row.next()) { connection.commit(); return; }
                }
            }
            try (Statement statement = connection.createStatement(); ResultSet invalid = statement.executeQuery(
                    "SELECT match_no FROM bw_matches WHERE match_no<=0 LIMIT 1")) {
                if (invalid.next()) throw new SQLException("历史记录存在非正数编号，拒绝自动重编号");
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO bw_match_number_migration (match_uuid, old_match_no, new_match_no) VALUES (?, ?, ?)")) {
                // 使用有序游标逐行分配，兼容仍在使用的 MySQL 5.7（不依赖窗口函数）。
                try (Statement source = connection.createStatement(); ResultSet rows = source.executeQuery(
                        "SELECT match_uuid, match_no FROM bw_matches ORDER BY match_no")) {
                    long next = 1;
                    while (rows.next()) {
                        insert.setString(1, rows.getString(1));
                        insert.setLong(2, rows.getLong(2));
                        insert.setLong(3, next++);
                        insert.addBatch();
                    }
                }
                insert.executeBatch();
            }
            try (Statement statement = connection.createStatement()) {
                // 先移到负数空间，防止唯一索引与尚未处理的旧编号冲突；事务失败整体回滚。
                statement.executeUpdate("UPDATE bw_matches SET match_no=-match_no");
                if (sqlite) {
                    statement.executeUpdate("UPDATE bw_matches SET match_no=(SELECT new_match_no FROM bw_match_number_migration n "
                            + "WHERE n.match_uuid=bw_matches.match_uuid)");
                    statement.executeUpdate("UPDATE bw_match_players SET match_no=(SELECT new_match_no FROM bw_match_number_migration n "
                            + "WHERE n.match_uuid=bw_match_players.match_uuid) WHERE EXISTS (SELECT 1 FROM bw_match_number_migration n "
                            + "WHERE n.match_uuid=bw_match_players.match_uuid)");
                } else {
                    statement.executeUpdate("UPDATE bw_matches m JOIN bw_match_number_migration n ON n.match_uuid=m.match_uuid "
                            + "SET m.match_no=n.new_match_no");
                    statement.executeUpdate("UPDATE bw_match_players p JOIN bw_match_number_migration n ON n.match_uuid=p.match_uuid "
                            + "SET p.match_no=n.new_match_no");
                }
                statement.executeUpdate("UPDATE bw_match_number_sequence SET last_match_no="
                        + "(SELECT COALESCE(MAX(new_match_no),0) FROM bw_match_number_migration) WHERE sequence_id=1");
            }
            try (PreparedStatement mark = connection.prepareStatement("INSERT INTO bw_match_schema (migration_id) VALUES (?)")) {
                mark.setString(1, ID);
                mark.executeUpdate();
            }
            connection.commit();
        } catch (SQLException exception) {
            try { connection.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
            throw exception;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
