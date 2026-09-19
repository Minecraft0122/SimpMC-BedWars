package com.andrei1058.bedwars.stats.match;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 可重放的关键写入数据，不保存连接、Bukkit 对象或编号分配结果。 */
record PendingMatchWrite(UUID operationId, Kind kind, MatchRecordSnapshot match, MatchEventSnapshot event,
                         UUID resetPlayer, Instant resetAt, List<UUID> punishedPlayers) {
    enum Kind { START, EVENT, FINISH, RESET }

    PendingMatchWrite {
        punishedPlayers = List.copyOf(punishedPlayers);
    }

    static PendingMatchWrite start(MatchRecordSnapshot match) {
        return new PendingMatchWrite(UUID.randomUUID(), Kind.START, match, null, null, null, List.of());
    }

    static PendingMatchWrite event(MatchEventSnapshot event) {
        return new PendingMatchWrite(event.eventId(), Kind.EVENT, null, event, null, null, List.of());
    }

    static PendingMatchWrite finish(MatchRecordSnapshot match, List<UUID> punishedPlayers) {
        return new PendingMatchWrite(UUID.randomUUID(), Kind.FINISH, match, null, null, null, punishedPlayers);
    }

    static PendingMatchWrite reset(UUID player, Instant at) {
        return new PendingMatchWrite(UUID.randomUUID(), Kind.RESET, null, null, player, at, List.of());
    }

    Set<UUID> orderedPlayers() {
        return switch (kind) {
            case RESET -> Set.of(resetPlayer);
            case FINISH -> match.playerStats().players().stream()
                    .map(MatchPlayerSnapshot::playerUuid).collect(Collectors.toSet());
            default -> Set.of();
        };
    }
}
