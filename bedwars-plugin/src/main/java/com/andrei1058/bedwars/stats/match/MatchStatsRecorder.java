/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.stats.match;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent;
import com.andrei1058.bedwars.api.events.player.PlayerJoinArenaEvent;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.player.PlayerReJoinEvent;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.LastHit;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.database.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Collects one immutable event stream and one counter set per playing arena. */
public final class MatchStatsRecorder implements Listener, AutoCloseable {

    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private final BedWars plugin;
    private final MatchStatsStore store;
    private final ZoneId zone;
    private final String timezoneId;
    private final String serverId;
    private final long reportIntervalTicks;
    private final long finishGraceTicks;
    private final boolean violationTracking;
    private final int matchLeaveVlThreshold;
    private final IllegalTeamDetector violationDetector;

    /* IArena has a legacy equals implementation without a matching hashCode. */
    private final Map<IArena, MatchRecord> records =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private final Set<MatchRecord> gameEndAnnounced =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<MatchRecord, FinishRequest> pendingFinishes =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private final Set<MatchRecord> finishScheduled =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ViolationEjectionKey> violationEjections =
            Collections.synchronizedSet(new HashSet<>());
    private final Map<MatchRecord, Set<UUID>> punishmentResets =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private BukkitTask reportTask;
    private boolean closed;

    public MatchStatsRecorder(BedWars plugin, MySQL database) {
        this.plugin = plugin;
        String configuredTimezone = BedWars.config.getYml().getString(
                ConfigPath.MATCH_STATISTICS_TIMEZONE, DEFAULT_TIMEZONE);
        ZoneId parsedZone;
        try {
            parsedZone = ZoneId.of(configuredTimezone == null || configuredTimezone.isBlank()
                    ? DEFAULT_TIMEZONE : configuredTimezone);
        } catch (DateTimeException exception) {
            parsedZone = ZoneId.of(DEFAULT_TIMEZONE);
            plugin.getLogger().warning("无效的对局统计时区 " + configuredTimezone
                    + "，已回退为 " + DEFAULT_TIMEZONE + "。");
        }
        this.zone = parsedZone;
        this.timezoneId = parsedZone.getId();
        this.serverId = resolveServerId();

        int intervalSeconds = Math.max(1, BedWars.config.getYml().getInt(
                ConfigPath.MATCH_STATISTICS_REPORT_INTERVAL_SECONDS, 300));
        this.reportIntervalTicks = Math.max(20L, intervalSeconds * 20L);
        this.finishGraceTicks = Math.max(0L, BedWars.config.getYml().getLong(
                ConfigPath.MATCH_STATISTICS_FINISH_GRACE_TICKS, 40L));
        this.violationTracking = BedWars.config.getYml().getBoolean(
                ConfigPath.MATCH_STATISTICS_VIOLATIONS_ENABLED, true);
        this.matchLeaveVlThreshold = Math.max(0, BedWars.config.getYml().getInt(
                ConfigPath.MATCH_STATISTICS_VIOLATIONS_MATCH_LEAVE_THRESHOLD, 25));
        boolean crossTeamItemTransfer = BedWars.config.getYml().getBoolean(
                ConfigPath.MATCH_STATISTICS_VIOLATIONS_CROSS_TEAM_ITEM_TRANSFER, true);
        List<Integer> warningThresholds = BedWars.config.getYml().getIntegerList(
                ConfigPath.MATCH_STATISTICS_VIOLATIONS_WARNING_THRESHOLDS);

        int queueCapacity = Math.max(100, BedWars.config.getYml().getInt(
                ConfigPath.MATCH_STATISTICS_QUEUE_CAPACITY, 10000));
        int retryDelay = Math.max(1, BedWars.config.getYml().getInt(
                ConfigPath.MATCH_STATISTICS_RETRY_DELAY_SECONDS, 5));
        this.store = new MatchStatsStore(database, zone, serverId, queueCapacity, retryDelay,
                warningThresholds);
        this.violationDetector = new IllegalTeamDetector(plugin, this, violationTracking,
                matchLeaveVlThreshold, crossTeamItemTransfer);
    }

    public void start() {
        store.start();
        violationDetector.start();
        reportTask = Bukkit.getScheduler().runTaskTimer(plugin, this::reportRunningMatches,
                reportIntervalTicks, reportIntervalTicks);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getNewState() != GameState.playing) return;
        IArena arena = event.getArena();
        synchronized (records) {
            MatchRecord current = records.get(arena);
            if (current != null && !current.isFinished()) return;

            Instant startedAt = arena.getStartTime() == null ? Instant.now() : arena.getStartTime();
            MatchRecord record = new MatchRecord(
                    UUID.randomUUID(),
                    serverId,
                    textOrFallback(arena.getArenaName(), "unknown-template"),
                    textOrFallback(arena.getWorldName(), arena.getArenaName()),
                    textOrFallback(arena.getGroup(), "Default"),
                    timezoneId,
                    startedAt
            );
            records.put(arena, record);
            for (Player player : arena.getPlayersSnapshot()) {
                registerPlayer(record, arena, player);
            }
            store.enqueueStart(record.startSnapshot(Instant.now()));
            enqueueEvent(record, "MATCH_START", null, null, null);
            violationDetector.matchStarted(arena, record);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArenaJoin(PlayerJoinArenaEvent event) {
        if (event.isCancelled()) return;
        MatchRecord record = getRecord(event.getArena());
        if (record == null || event.isSpectator()) return;
        registerPlayer(record, event.getArena(), event.getPlayer());
        enqueueEvent(record, "PLAYER_JOIN", event.getPlayer().getUniqueId(), null, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArenaRejoin(PlayerReJoinEvent event) {
        if (event.isCancelled()) return;
        MatchRecord record = getRecord(event.getArena());
        if (record == null) return;
        Player player = event.getPlayer();
        MatchPlayerStats stats = registerPlayer(record, event.getArena(), player);
        stats.recordReconnect();
        setOutcomeIfUnknown(stats, MatchPlayerOutcome.UNKNOWN);
        enqueueEvent(record, "RECONNECT", player.getUniqueId(), null,
                "respawn=" + event.getRespawnTime());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(PlayerKillEvent event) {
        MatchRecord record = getRecord(event.getArena());
        if (record == null) return;

        Player victim = event.getVictim();
        String victimTeam = teamId(event.getVictimTeam(), event.getArena(), victim.getUniqueId());
        MatchPlayerStats victimStats = record.getStats().registerPlayer(victim.getUniqueId(),
                victim.getName(), victimTeam);
        victimStats.recordDeath();

        Player killer = event.getKiller();
        String killerTeam = killer == null ? null :
                teamId(event.getKillerTeam(), event.getArena(), killer.getUniqueId());
        boolean enemy = killer != null && !killer.getUniqueId().equals(victim.getUniqueId())
                && victimTeam != null && killerTeam != null
                && !sameTeam(victimTeam, killerTeam);
        if (enemy) {
            record.getStats().registerPlayer(killer.getUniqueId(), killer.getName(), killerTeam)
                    .recordKill(event.getCause().isFinalKill());
        }

        String type = event.getCause().isPvpLogOut() ? "DISCONNECT_KILL" : "PLAYER_KILL";
        enqueueEvent(record, type, killer == null ? null : killer.getUniqueId(),
                victim.getUniqueId(), "cause=" + event.getCause().name()
                        + ";final=" + event.getCause().isFinalKill());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedBreak(PlayerBedBreakEvent event) {
        MatchRecord record = getRecord(event.getArena());
        if (record == null) return;
        Player player = event.getPlayer();
        String team = teamId(event.getPlayerTeam(), event.getArena(), player.getUniqueId());
        record.getStats().registerPlayer(player.getUniqueId(), player.getName(), team).recordBedBreak();
        String victimTeam = teamId(event.getVictimTeam(), event.getArena(), null);
        enqueueEvent(record, "BED_BREAK", player.getUniqueId(), null,
                victimTeam == null ? null : "victim_team=" + victimTeam);
    }

    /** Capture the arena association before QuitAndTeleportListener removes it. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        MatchRecord record = arena == null ? null : getRecord(arena);
        if (record == null) return;
        MatchPlayerStats stats = registerPlayer(record, arena, player);
        stats.recordDisconnect();
        setOutcomeIfUnknown(stats, MatchPlayerOutcome.DISCONNECTED);
        enqueueEvent(record, "DISCONNECT", player.getUniqueId(), null,
                "reason=" + event.getReason().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArenaLeave(PlayerLeaveArenaEvent event) {
        MatchRecord record = getRecord(event.getArena());
        if (record == null) return;
        Player player = event.getPlayer();
        MatchPlayerStats stats = record.getStats().getPlayer(player.getUniqueId()).orElse(null);
        /* Spectators are not match participants and may never have joined the
         * player registry. Do not create a phantom player row for them. */
        if (stats == null && event.isSpectator()) return;
        if (stats == null) stats = registerPlayer(record, event.getArena(), player);
        if (!event.isSpectator() && !gameEndAnnounced.contains(record)
                && event.getArena().getStatus() == GameState.playing) {
            MatchPlayerSnapshot snapshot = stats.snapshot();
            setOutcomeIfUnknown(stats, snapshot.disconnects() > 0
                    ? MatchPlayerOutcome.DISCONNECTED : MatchPlayerOutcome.ABANDONED);
        }
        Player lastDamager = event.getLastDamager();
        String details = "spectator=" + event.isSpectator();
        if (lastDamager != null) details += ";last_damager=" + lastDamager.getUniqueId();
        enqueueEvent(record, "PLAYER_LEAVE", player.getUniqueId(), null,
                details);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(GameEndEvent event) {
        MatchRecord record = getRecord(event.getArena());
        if (record == null || gameEndAnnounced.contains(record)) return;

        List<UUID> winners = new ArrayList<>(event.getWinners());
        Set<UUID> winnerSet = new HashSet<>(winners);
        Set<UUID> loserSet = new HashSet<>(event.getLosers());
        for (UUID uuid : winners) {
            record.getStats().registerPlayer(uuid, playerName(uuid), teamId(event.getArena(), uuid));
            record.getStats().getPlayer(uuid).ifPresent(stats -> stats.setOutcome(MatchPlayerOutcome.WIN));
            enqueueEvent(record, "PLAYER_WIN", uuid, null, null);
        }
        for (UUID uuid : loserSet) {
            if (winnerSet.contains(uuid)) continue;
            MatchPlayerStats stats = record.getStats().registerPlayer(uuid, playerName(uuid), teamId(event.getArena(), uuid));
            preserveDepartureOutcome(stats);
            enqueueEvent(record, "PLAYER_LOSS", uuid, null, null);
        }
        for (MatchPlayerStats stats : record.getStats().getPlayers()) {
            if (winnerSet.contains(stats.getPlayerUuid())) {
                stats.setOutcome(MatchPlayerOutcome.WIN);
            } else if (!loserSet.contains(stats.getPlayerUuid())) {
                preserveDepartureOutcome(stats);
                enqueueEvent(record, "PLAYER_LOSS", stats.getPlayerUuid(), null, null);
            }
        }

        Instant endedAt = Instant.now();
        FinishRequest request = new FinishRequest(
                event.getTeamWinner() == null ? null : event.getTeamWinner().getName(),
                event.getTeamWinner() == null ? "NO_WINNER" : "WINNER", endedAt);
        gameEndAnnounced.add(record);
        pendingFinishes.put(record, request);
        enqueueEvent(record, "GAME_END", null, null,
                request.winnerTeam() == null ? request.endReason() : "winner_team=" + request.winnerTeam());
        scheduleFinish(record);
    }

    /** Public hook for integrations that provide an additional illegal-team signal. */
    public void addIllegalTeamVl(IArena arena, UUID playerUuid, int amount) {
        recordPositiveViolation(arena, playerUuid, amount, false, "API_ILLEGAL_TEAM", null);
    }

    /** Public hook for integrations that provide an additional kill-boosting signal. */
    public void addKillBoostingVl(IArena arena, UUID playerUuid, int amount) {
        recordPositiveViolation(arena, playerUuid, amount, true, "API_KILL_BOOSTING", null);
    }

    /** Called by the built-in detector; all Bukkit mutations remain on the main thread. */
    void addDetectedViolation(IArena arena, UUID playerUuid, int amount, boolean killBoosting,
                              String rule, String details) {
        recordPositiveViolation(arena, playerUuid, amount, killBoosting, rule, details);
    }

    /** Store signed exclusion evidence without changing the positive VL fields. */
    void addDetectedEvidence(IArena arena, UUID playerUuid, int amount, String rule,
                             String details) {
        if (!violationTracking || arena == null || playerUuid == null || amount == 0) return;
        MatchRecord record = getRecord(arena);
        if (!isOpenForViolation(arena, record)) return;
        MatchPlayerStats stats = record.getStats().registerPlayer(playerUuid,
                playerName(playerUuid), teamId(arena, playerUuid));
        stats.adjustEvidence(amount);
        MatchPlayerSnapshot snapshot = stats.snapshot();
        String eventDetails = "rule=" + rule + ";amount=" + amount + ";effective_vl="
                + snapshot.totalVl() + (details == null ? "" : ";" + details);
        enqueueEvent(record, "VIOLATION_EVIDENCE", playerUuid, null, eventDetails);
        violationDetector.scoreChanged(arena, playerUuid, snapshot.totalVl(), amount, rule);
    }

    /** Bukkit listener for the built-in evidence-based violation detector. */
    public IllegalTeamDetector getViolationDetector() {
        return violationDetector;
    }

    /** Return the UUID of the currently recorded match for an arena. */
    @Nullable
    public UUID matchUuid(IArena arena) {
        MatchRecord record = getRecord(arena);
        return record == null ? null : record.getMatchUuid();
    }

    /** Mark a voluntary or expired departure without changing the arena lifecycle. */
    public void markAbandonment(IArena arena, UUID playerUuid, String reason) {
        MatchRecord record = getRecord(arena);
        if (!isOpenForMatch(arena, record) || playerUuid == null) return;
        MatchPlayerStats stats = record.getStats().registerPlayer(playerUuid,
                playerName(playerUuid), teamId(arena, playerUuid));
        stats.setOutcome(MatchPlayerOutcome.ABANDONED);
        enqueueEvent(record, "PLAYER_ABANDONED", playerUuid, null,
                reason == null ? null : "reason=" + reason);
    }

    /** Mark a player removed by the AFK policy. */
    public void markAfkRemoved(IArena arena, UUID playerUuid) {
        MatchRecord record = getRecord(arena);
        if (!isOpenForMatch(arena, record) || playerUuid == null) return;
        MatchPlayerStats stats = record.getStats().registerPlayer(playerUuid,
                playerName(playerUuid), teamId(arena, playerUuid));
        stats.setOutcome(MatchPlayerOutcome.AFK_REMOVED);
        enqueueEvent(record, "AFK_REMOVE", playerUuid, null, null);
    }

    @Nullable
    MatchRecord recordForDetector(IArena arena) {
        return getRecord(arena);
    }

    boolean isOpenForViolation(IArena arena, @Nullable MatchRecord record) {
        return violationTracking && isOpenForMatch(arena, record);
    }

    boolean isOpenForMatch(IArena arena, @Nullable MatchRecord record) {
        return arena != null && record != null && !record.isFinished()
                && !gameEndAnnounced.contains(record) && arena.getStatus() == GameState.playing;
    }

    int matchLeaveVlThreshold() {
        return matchLeaveVlThreshold;
    }

    void ejectForViolation(IArena arena, UUID playerUuid, int currentVl, String rule) {
        if (arena == null || playerUuid == null) return;
        MatchRecord record = getRecord(arena);
        if (!isOpenForViolation(arena, record)) return;
        ViolationEjectionKey key = new ViolationEjectionKey(record.getMatchUuid(), playerUuid);
        if (!violationEjections.add(key)) return;
        synchronized (punishmentResets) {
            punishmentResets.computeIfAbsent(record, ignored -> new HashSet<>()).add(playerUuid);
        }
        MatchPlayerStats stats = record.getStats().getPlayer(playerUuid).orElse(null);
        if (stats != null) stats.setOutcome(MatchPlayerOutcome.VIOLATION_REMOVED);
        enqueueEvent(record, "VIOLATION_EJECT", playerUuid, null,
                "rule=" + rule + ";effective_vl=" + currentVl + ";threshold=" + matchLeaveVlThreshold);
        if (plugin.getDisciplineService() != null) {
            plugin.getDisciplineService().markViolation(arena, playerUuid, rule);
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            MatchRecord current = getRecord(arena);
            Player player = Bukkit.getPlayer(playerUuid);
            if (!isOpenForViolation(arena, current) || player == null || !player.isOnline()
                    || !arena.isPlayer(player)) return;
            AdventureText.send(player, com.andrei1058.bedwars.api.language.Language.getMsg(
                    player, Messages.DISCIPLINE_VIOLATION_REMOVED));
            LastHit lastHit = LastHit.getLastHit(player);
            if (lastHit != null) lastHit.remove();
            arena.removePlayer(player, false);
        });
    }

    /** Queue a punishment-threshold reset while retaining the crime record. */
    public boolean resetPunishmentVl(UUID playerUuid) {
        if (playerUuid == null || closed) return false;
        return store.enqueueResetPunishmentVl(playerUuid);
    }

    private void reportRunningMatches() {
        List<MatchRecord> active;
        synchronized (records) {
            active = new ArrayList<>(records.values());
        }
        Instant now = Instant.now();
        for (MatchRecord record : active) {
            if (!record.isFinished()) store.enqueueReport(record.reportSnapshot(now));
        }
    }

    private void scheduleFinish(MatchRecord record) {
        synchronized (finishScheduled) {
            if (!finishScheduled.add(record)) return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> finish(record), finishGraceTicks);
    }

    private void finish(MatchRecord record) {
        FinishRequest request = pendingFinishes.get(record);
        String status = request == null ? "ABORTED" : "FINISHED";
        MatchRecordSnapshot snapshot = record.finish(status,
                request == null ? null : request.winnerTeam(),
                request == null ? "PLUGIN_DISABLE" : request.endReason(),
                request == null ? Instant.now() : request.endedAt());
        if (store.enqueueFinish(snapshot, punishedPlayers(record))) {
            violationDetector.matchFinished(record);
            removeRecord(record);
            pendingFinishes.remove(record);
            punishmentResets.remove(record);
            gameEndAnnounced.remove(record);
            synchronized (finishScheduled) {
                finishScheduled.remove(record);
            }
        } else {
            synchronized (finishScheduled) {
                finishScheduled.remove(record);
            }
            if (!closed) Bukkit.getScheduler().runTaskLater(plugin, () -> scheduleFinish(record), 20L);
        }
    }

    private MatchPlayerStats registerPlayer(MatchRecord record, IArena arena, Player player) {
        return record.getStats().registerPlayer(player.getUniqueId(), player.getName(), teamId(arena, player.getUniqueId()));
    }

    private MatchRecord getRecord(IArena arena) {
        if (arena == null) return null;
        synchronized (records) {
            return records.get(arena);
        }
    }

    private void removeRecord(MatchRecord record) {
        synchronized (records) {
            records.entrySet().removeIf(entry -> entry.getValue() == record);
        }
    }

    private String resolveServerId() {
        String configured = BedWars.config.getYml().getString(
                ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID);
        if (configured == null || configured.isBlank()) {
            configured = Bukkit.getServer().getName();
        }
        return textOrFallback(configured, "unknown-server");
    }

    @Nullable
    static String playerName(UUID uuid) {
        Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
        return player == null ? null : player.getName();
    }

    @Nullable
    static String teamId(@Nullable ITeam team, IArena arena, @Nullable UUID playerUuid) {
        if (team != null) return team.getName();
        return playerUuid == null ? null : teamId(arena, playerUuid);
    }

    @Nullable
    static String teamId(IArena arena, UUID playerUuid) {
        ITeam team = arena.getExTeam(playerUuid);
        if (team == null) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) team = arena.getTeam(player);
        }
        return team == null ? null : team.getName();
    }

    private static boolean sameTeam(@Nullable String first, @Nullable String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private static String textOrFallback(@Nullable String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void enqueueEvent(MatchRecord record, String type, @Nullable UUID actor,
                              @Nullable UUID target, @Nullable String details) {
        String compactDetails = details == null || details.length() <= 255
                ? details : details.substring(0, 255);
        MatchEventSnapshot event = record.event(type, actor, target, compactDetails, Instant.now());
        if (event != null) store.enqueueEvent(event);
    }

    private static void setOutcomeIfUnknown(MatchPlayerStats stats, MatchPlayerOutcome outcome) {
        if (outcome == MatchPlayerOutcome.UNKNOWN) return;
        if (stats.snapshot().outcome() == MatchPlayerOutcome.UNKNOWN) stats.setOutcome(outcome);
    }

    /** Keep an earlier voluntary leave or disconnect visible in aggregates. */
    private static void preserveDepartureOutcome(MatchPlayerStats stats) {
        MatchPlayerOutcome current = stats.snapshot().outcome();
        if (current != MatchPlayerOutcome.ABANDONED && current != MatchPlayerOutcome.DISCONNECTED
                && current != MatchPlayerOutcome.AFK_REMOVED
                && current != MatchPlayerOutcome.VIOLATION_REMOVED) {
            stats.setOutcome(MatchPlayerOutcome.LOSS);
        }
    }

    private void recordPositiveViolation(IArena arena, UUID playerUuid, int amount,
                                         boolean killBoosting, String rule, @Nullable String details) {
        if (!violationTracking || arena == null || playerUuid == null || amount <= 0) return;
        MatchRecord record = getRecord(arena);
        if (!isOpenForViolation(arena, record)) return;
        MatchPlayerStats stats = record.getStats().registerPlayer(playerUuid,
                playerName(playerUuid), teamId(arena, playerUuid));
        if (killBoosting) stats.addKillBoostingVl(amount);
        else stats.addIllegalTeamVl(amount);
        MatchPlayerSnapshot snapshot = stats.snapshot();
        String eventDetails = "rule=" + rule + ";amount=" + amount + ";effective_vl="
                + snapshot.totalVl() + (details == null ? "" : ";" + details);
        enqueueEvent(record, killBoosting ? "VIOLATION_KILL_BOOSTING" : "VIOLATION_ILLEGAL_TEAM",
                playerUuid, null, eventDetails);
        violationDetector.scoreChanged(arena, playerUuid, snapshot.totalVl(), amount, rule);
    }

    /** Flush all currently active records before the shared MySQL pool closes. */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        if (reportTask != null) reportTask.cancel();
        violationDetector.close();

        List<MatchRecord> active;
        synchronized (records) {
            active = new ArrayList<>(records.values());
        }
        for (MatchRecord record : active) {
            MatchRecordSnapshot snapshot;
            if (record.isFinished()) {
                /* A previous enqueue may have been rejected because the
                 * bounded queue was full. Re-submit the immutable final
                 * snapshot before the writer is closed. */
                snapshot = record.getFinalSnapshot();
            } else {
                FinishRequest request = pendingFinishes.get(record);
                snapshot = record.finish(
                        request == null ? "ABORTED" : "FINISHED",
                        request == null ? null : request.winnerTeam(),
                        request == null ? "PLUGIN_DISABLE" : request.endReason(),
                        request == null ? Instant.now() : request.endedAt());
            }
            if (snapshot != null && !store.enqueueFinish(snapshot, punishedPlayers(record))) {
                plugin.getLogger().warning("插件关闭时无法排队对局最终统计：" + record.getMatchUuid());
            }
        }
        synchronized (records) {
            records.clear();
        }
        pendingFinishes.clear();
        gameEndAnnounced.clear();
        finishScheduled.clear();
        violationEjections.clear();
        punishmentResets.clear();
        store.close();
    }

    private Set<UUID> punishedPlayers(MatchRecord record) {
        synchronized (punishmentResets) {
            Set<UUID> players = punishmentResets.get(record);
            return players == null ? Set.of() : Set.copyOf(players);
        }
    }

    private record FinishRequest(@Nullable String winnerTeam, String endReason, Instant endedAt) {
    }

    private record ViolationEjectionKey(UUID matchUuid, UUID playerUuid) {
    }
}
