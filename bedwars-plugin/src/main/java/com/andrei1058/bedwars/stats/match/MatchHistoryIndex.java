package com.andrei1058.bedwars.stats.match;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

/**
 * Installs the match-number access path used by player history queries.
 *
 * <p>The migration deliberately leaves the column nullable. This makes an
 * interrupted startup harmless: a later startup can add the index and repeat
 * the backfill without deleting or rewriting any player statistics.</p>
 */
public final class MatchHistoryIndex {
    public static final String PLAYER_MATCH_INDEX = "idx_bw_match_players_player_match_no";
    public static final String TOTALS_TABLE = "bw_player_match_totals";

    private MatchHistoryIndex() {
    }

    /**
     * Add and backfill {@code bw_match_players.match_no}, then create the
     * player-history index. The operation is idempotent and safe to retry
     * after a connection or process failure.
     *
     * @param connection open database connection
     * @param sqlite whether the connection targets SQLite rather than MySQL
     */
    public static void initialize(Connection connection, boolean sqlite) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("对局历史迁移必须在自动提交的独立连接上执行");
        }
        // MySQL DDL implicitly commits; all DDL precedes transactional data migration.
        addMatchNumberColumn(connection, sqlite);
        addTotalsAppliedColumn(connection, sqlite);
        createTotalsTables(connection, sqlite);
        createPlayerMatchIndex(connection, sqlite);
        createUnappliedTotalsIndex(connection);
        createPendingMatchIndex(connection);
        backfillMatchNumbers(connection, sqlite);
        backfillTotals(connection, sqlite);
    }

    private static void addMatchNumberColumn(Connection connection, boolean sqlite) throws SQLException {
        String type = sqlite ? "INTEGER" : "BIGINT";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE bw_match_players ADD COLUMN match_no " + type + " NULL");
        } catch (SQLException exception) {
            if (!isAlreadyExists(exception)) throw exception;
        }
    }

    private static void addTotalsAppliedColumn(Connection connection, boolean sqlite) throws SQLException {
        String type = sqlite ? "INTEGER" : "TINYINT";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE bw_match_players ADD COLUMN totals_applied " + type
                    + " NOT NULL DEFAULT 0");
        } catch (SQLException exception) {
            if (!isAlreadyExists(exception)) throw exception;
        }
    }

    private static void backfillMatchNumbers(Connection connection, boolean sqlite) throws SQLException {
        String sql = sqlite
                ? "UPDATE bw_match_players SET match_no=(SELECT m.match_no FROM bw_matches m "
                + "WHERE m.match_uuid=bw_match_players.match_uuid) WHERE match_no IS NULL "
                + "AND EXISTS (SELECT 1 FROM bw_matches m WHERE m.match_uuid=bw_match_players.match_uuid)"
                : "UPDATE bw_match_players p INNER JOIN bw_matches m ON m.match_uuid=p.match_uuid "
                + "SET p.match_no=m.match_no WHERE p.match_no IS NULL";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void createPlayerMatchIndex(Connection connection, boolean sqlite) throws SQLException {
        String sql = "CREATE INDEX " + PLAYER_MATCH_INDEX
                + " ON bw_match_players (player_uuid, match_no)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            // 允许多服并发升级，只有重复列或索引可以忽略。
            if (!isAlreadyExists(exception)) throw exception;
        }
    }

    private static void createUnappliedTotalsIndex(Connection connection) throws SQLException {
        String indexName = "idx_bw_match_players_player_totals_applied";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE INDEX " + indexName
                    + " ON bw_match_players (player_uuid, totals_applied)");
        } catch (SQLException exception) {
            if (!isAlreadyExists(exception)) throw exception;
        }
    }

    private static void createPendingMatchIndex(Connection connection) throws SQLException {
        String indexName = "idx_bw_match_players_pending_match";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE INDEX " + indexName
                    + " ON bw_match_players (totals_applied, match_uuid)");
        } catch (SQLException exception) {
            if (!isAlreadyExists(exception)) throw exception;
        }
    }

    private static void createTotalsTables(Connection connection, boolean sqlite) throws SQLException {
        String countType = sqlite ? "INTEGER" : "BIGINT";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TOTALS_TABLE + " ("
                    + "player_uuid " + (sqlite ? "TEXT" : "CHAR(36)") + " PRIMARY KEY, "
                    + "matches_played " + countType + " NOT NULL DEFAULT 0, "
                    + "normal_kills " + countType + " NOT NULL DEFAULT 0, "
                    + "final_kills " + countType + " NOT NULL DEFAULT 0, "
                    + "deaths " + countType + " NOT NULL DEFAULT 0, "
                    + "beds_destroyed " + countType + " NOT NULL DEFAULT 0"
                    + (sqlite ? ")" : ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"));

        }
    }

    /** 调用方已锁定父对局行；累计值和标记必须与最终结算在同一事务提交。 */
    static void recordFinish(Connection connection, boolean sqlite,
                             MatchRecordSnapshot snapshot) throws SQLException {
        if ("FINISHED".equals(snapshot.status())) applyTotals(connection, sqlite, snapshot.matchUuid());
    }

    private static void applyTotals(Connection connection, boolean sqlite, UUID matchUuid) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("对局累计值必须在结算事务内更新");
        String prefix = "INSERT INTO " + TOTALS_TABLE
                + " (player_uuid, matches_played, normal_kills, final_kills, deaths, beds_destroyed) "
                + "SELECT player_uuid, 1, normal_kills, final_kills, deaths, beds_destroyed "
                + "FROM bw_match_players WHERE match_uuid=? AND totals_applied=0 ORDER BY player_uuid ";
        String suffix = sqlite
                ? "ON CONFLICT(player_uuid) DO UPDATE SET "
                + "matches_played=matches_played+excluded.matches_played, normal_kills=normal_kills+excluded.normal_kills, "
                + "final_kills=final_kills+excluded.final_kills, deaths=deaths+excluded.deaths, "
                + "beds_destroyed=beds_destroyed+excluded.beds_destroyed"
                : "ON DUPLICATE KEY UPDATE matches_played=bw_player_match_totals.matches_played+VALUES(matches_played), "
                + "normal_kills=bw_player_match_totals.normal_kills+VALUES(normal_kills), final_kills=bw_player_match_totals.final_kills+VALUES(final_kills), "
                + "deaths=bw_player_match_totals.deaths+VALUES(deaths), beds_destroyed=bw_player_match_totals.beds_destroyed+VALUES(beds_destroyed)";
        try (PreparedStatement totals = connection.prepareStatement(prefix + suffix);
             PreparedStatement mark = connection.prepareStatement(
                     "UPDATE bw_match_players SET totals_applied=1 WHERE match_uuid=? AND totals_applied=0")) {
            totals.setString(1, matchUuid.toString());
            totals.executeUpdate();
            mark.setString(1, matchUuid.toString());
            mark.executeUpdate();
        }
    }

    private static void backfillTotals(Connection connection, boolean sqlite) throws SQLException {
        String rows = "SELECT DISTINCT p.match_uuid FROM bw_match_players p INNER JOIN bw_matches m "
                + "ON m.match_uuid=p.match_uuid WHERE m.status='FINISHED' AND p.totals_applied=0 LIMIT 100";
        while (true) {
            List<UUID> matches = new ArrayList<>();
            try (Statement query = connection.createStatement(); ResultSet result = query.executeQuery(rows)) {
                while (result.next()) matches.add(UUID.fromString(result.getString(1)));
            }
            if (matches.isEmpty()) return;
            for (UUID matchUuid : matches) {
                connection.setAutoCommit(false);
                try {
                    backfillMatch(connection, sqlite, matchUuid);
                    connection.commit();
                } catch (SQLException exception) {
                    try { connection.rollback(); } catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    private static void backfillMatch(Connection connection, boolean sqlite, UUID matchUuid) throws SQLException {
        String statusSql = "SELECT status FROM bw_matches WHERE match_uuid=?" + (sqlite ? "" : " FOR UPDATE");
        try (PreparedStatement status = connection.prepareStatement(statusSql)) {
            status.setString(1, matchUuid.toString());
            try (ResultSet result = status.executeQuery()) {
                if (!result.next() || !"FINISHED".equals(result.getString(1))) return;
            }
        }
        applyTotals(connection, sqlite, matchUuid);
    }

    private static boolean isAlreadyExists(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            String state = current.getSQLState();
            String message = current.getMessage();
            String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
            if (current.getErrorCode() == 1060 || current.getErrorCode() == 1061
                    || "42S21".equals(state) || "42S11".equals(state)
                    || lower.contains("duplicate column") || lower.contains("duplicate key name")
                    || lower.contains("already exists")) return true;
        }
        return false;
    }
}
