package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.api.stats.MatchInfo;
import com.andrei1058.bedwars.api.stats.MatchPlayerResult;
import com.andrei1058.bedwars.api.stats.PlayerMatchTotals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** 持久化对局记录的同步查询实现。调用方必须在异步线程执行。 */
public final class MatchHistoryReader {
    private static final String MATCH_COLUMNS = "m.match_no, m.match_uuid, m.template_name, m.runtime_arena, "
            + "m.arena_group, m.server_id, m.status, m.started_at, m.ended_at, m.winner_team, m.arena_timezone";
    private final MatchStatsDatabase database;

    public MatchHistoryReader(MatchStatsDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public Optional<MatchInfo> findMatch(long matchNumber) throws SQLException {
        if (matchNumber <= 0) return Optional.empty();
        return findMatch("m.match_no", matchNumber);
    }

    public Optional<MatchInfo> findMatch(UUID matchUuid) throws SQLException {
        return findMatch("m.match_uuid", Objects.requireNonNull(matchUuid, "matchUuid").toString());
    }

    private Optional<MatchInfo> findMatch(String column, Object value) throws SQLException {
        String sql = "SELECT " + MATCH_COLUMNS + " FROM bw_matches m WHERE " + column + "=?";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readMatch(result)) : Optional.empty();
            }
        }
    }

    public List<MatchPlayerResult> getMatchPlayers(UUID matchUuid) throws SQLException {
        Objects.requireNonNull(matchUuid, "matchUuid");
        String sql = "SELECT player_uuid, player_name, team_id, normal_kills, final_kills, deaths, beds_destroyed, outcome "
                + "FROM bw_match_players WHERE match_uuid=? ORDER BY player_uuid";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, matchUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                List<MatchPlayerResult> players = new ArrayList<>();
                while (result.next()) {
                    players.add(new MatchPlayerResult(UUID.fromString(result.getString("player_uuid")),
                            result.getString("player_name"), result.getString("team_id"),
                            result.getLong("normal_kills"), result.getLong("final_kills"),
                            result.getLong("deaths"), result.getLong("beds_destroyed"), result.getString("outcome")));
                }
                return List.copyOf(players);
            }
        }
    }

    /** 直接汇总明细表，不依赖数据库创建视图的权限。 */
    public PlayerMatchTotals getPlayerTotals(UUID playerUuid) throws SQLException {
        Objects.requireNonNull(playerUuid, "playerUuid");
        String sql = "SELECT COUNT(*) AS matches_played, COALESCE(SUM(p.normal_kills), 0) AS normal_kills, "
                + "COALESCE(SUM(p.final_kills), 0) AS final_kills, COALESCE(SUM(p.deaths), 0) AS deaths, "
                + "COALESCE(SUM(p.beds_destroyed), 0) AS beds_destroyed "
                + "FROM bw_match_players p INNER JOIN bw_matches m ON m.match_uuid=p.match_uuid "
                + "WHERE p.player_uuid=? AND m.status='FINISHED'";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return new PlayerMatchTotals(playerUuid, 0, 0, 0, 0, 0);
                return new PlayerMatchTotals(playerUuid, result.getLong("matches_played"),
                        result.getLong("normal_kills"), result.getLong("final_kills"),
                        result.getLong("deaths"), result.getLong("beds_destroyed"));
            }
        }
    }

    public List<MatchInfo> getPlayerMatches(UUID playerUuid, int limit, int offset) throws SQLException {
        Objects.requireNonNull(playerUuid, "playerUuid");
        if (limit < 1 || limit > 100 || offset < 0) {
            throw new IllegalArgumentException("每页数量必须为 1 至 100，偏移量不能为负数");
        }
        String sql = "SELECT " + MATCH_COLUMNS + " FROM bw_matches m INNER JOIN bw_match_players p "
                + "ON p.match_uuid=m.match_uuid WHERE p.player_uuid=? ORDER BY m.match_no DESC LIMIT ? OFFSET ?";
        try (Connection connection = database.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            try (ResultSet result = statement.executeQuery()) {
                List<MatchInfo> matches = new ArrayList<>();
                while (result.next()) matches.add(readMatch(result));
                return List.copyOf(matches);
            }
        }
    }

    private static MatchInfo readMatch(ResultSet result) throws SQLException {
        try {
            ZoneId zone = ZoneId.of(result.getString("arena_timezone"));
            return new MatchInfo(result.getLong("match_no"), UUID.fromString(result.getString("match_uuid")),
                    result.getString("template_name"), result.getString("runtime_arena"),
                    result.getString("arena_group"), result.getString("server_id"), result.getString("status"),
                    readTime(result.getString("started_at"), zone), readTime(result.getString("ended_at"), zone),
                    result.getString("winner_team"));
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new SQLException("对局记录包含无效的 UUID 或时间", exception);
        }
    }

    private static Instant readTime(String value, ZoneId zone) {
        if (value == null) return null;
        return LocalDateTime.parse(value.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(zone).toInstant();
    }
}
