/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.andrei1058.bedwars.discipline;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.player.PlayerJoinArenaEvent;
import com.andrei1058.bedwars.api.events.player.PlayerReJoinEvent;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.util.AdventureText;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.ReJoin;
import com.andrei1058.bedwars.database.MySQL;
import com.andrei1058.bedwars.stats.match.MatchStatsRecorder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

/** Coordinates AFK/abandonment/violation punishment across lobby and arena nodes. */
public final class DisciplineService implements Listener, AutoCloseable {

    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private final BedWars plugin;
    private final DisciplineStore store;
    private final DisciplinePolicy policy;
    private final Map<UUID, DisciplineStore.Status> cache = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final boolean voluntaryLeaveEnabled;
    private final boolean disconnectTimeoutEnabled;
    private volatile boolean closed;

    public DisciplineService(BedWars plugin, MySQL database) {
        this.plugin = plugin;
        this.enabled = BedWars.config.getYml().getBoolean(ConfigPath.DISCIPLINE_ENABLED, true);
        this.voluntaryLeaveEnabled = BedWars.config.getYml().getBoolean(
                ConfigPath.DISCIPLINE_VOLUNTARY_LEAVE, true);
        this.disconnectTimeoutEnabled = BedWars.config.getYml().getBoolean(
                ConfigPath.DISCIPLINE_DISCONNECT_TIMEOUT, true);
        this.store = new DisciplineStore(database, configuredZone());
        DisciplinePolicy configuredPolicy;
        try {
            configuredPolicy = readPolicy();
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("纪律冷却配置无效，已回退为内置默认值：" + exception.getMessage());
            configuredPolicy = new DisciplinePolicy();
        }
        this.policy = configuredPolicy;
    }

    public void start() {
        if (!enabled) return;
        store.start();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean afkEnabled() {
        return enabled && BedWars.config.getYml().getBoolean(ConfigPath.DISCIPLINE_AFK_ENABLED, true);
    }

    public long afkWarningSeconds() {
        return positive(ConfigPath.DISCIPLINE_AFK_WARNING_SECONDS, 60L);
    }

    public long afkFinalWarningSeconds() {
        return positive(ConfigPath.DISCIPLINE_AFK_FINAL_WARNING_SECONDS, 120L);
    }

    public long afkRemovalSeconds() {
        return positive(ConfigPath.DISCIPLINE_AFK_REMOVAL_SECONDS, 180L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled || closed) return;
        DisciplineStore.Status status = store.loadBlocking(event.getUniqueId(), 2_500L);
        cache.put(event.getUniqueId(), status);
    }

    /** Spectators may still watch a game while their match-entry cooldown is active. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onArenaJoin(PlayerJoinArenaEvent event) {
        if (event.isSpectator()) return;
        if (denyIfBlocked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArenaRejoin(PlayerReJoinEvent event) {
        if (denyIfBlocked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() != event.getTo().getWorld()
                || event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            touch(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) touch(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        touch(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) touch(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) touch(player);
        if (event.getEntity() instanceof Player player) touch(player);
    }

    private void touch(Player player) {
        if (player != null && Arena.getArenaByPlayer(player) != null) {
            Arena.markAfkActivity(player.getUniqueId());
            Arena.afkCheck.remove(player.getUniqueId());
        }
    }

    /** Mark a voluntary /bw leave while a player is an active participant. */
    public void markVoluntaryLeave(Player player, IArena arena) {
        if (!enabled || !voluntaryLeaveEnabled || player == null || arena == null
                || arena.getStatus() != GameState.playing || !arena.isPlayer(player)) return;
        markAbandonment(player.getUniqueId(), arena, "VOLUNTARY_LEAVE");
    }

    /** Mark a kicked player as an immediate abandonment. */
    public void markKicked(Player player, IArena arena) {
        if (!enabled || BedWars.isShuttingDown() || player == null || arena == null
                || arena.getStatus() != GameState.playing
                || !arena.isPlayer(player)) return;
        markAbandonment(player.getUniqueId(), arena, "KICKED");
    }

    /** Mark a reconnect lease that expired without a return. */
    public void markReconnectExpired(ReJoin reJoin) {
        if (!enabled || !disconnectTimeoutEnabled || reJoin == null) return;
        IArena arena = reJoin.getArena();
        if (arena == null || arena.getStatus() != GameState.playing) return;
        if (!reJoin.tryMarkAbandonmentPenaltyRecorded()) return;
        markAbandonment(reJoin.getPl(), arena, "DISCONNECT_TIMEOUT");
    }

    public void markAfk(Player player, IArena arena) {
        if (!enabled || player == null || arena == null) return;
        MatchStatsRecorder recorder = plugin.getMatchStatsRecorder();
        if (recorder != null) recorder.markAfkRemoved(arena, player.getUniqueId());
        record(player.getUniqueId(), arena, DisciplinePolicy.Category.AFK, "AFK_REMOVED", false);
    }

    public void markViolation(IArena arena, UUID playerUuid, String rule) {
        if (!enabled || arena == null || playerUuid == null) return;
        record(playerUuid, arena, DisciplinePolicy.Category.VIOLATION,
                rule == null ? "MATCH_VL" : rule, false);
    }

    public void markAbandonment(UUID playerUuid, IArena arena, String reason) {
        if (!enabled || playerUuid == null || arena == null) return;
        MatchStatsRecorder recorder = plugin.getMatchStatsRecorder();
        if (recorder != null) recorder.markAbandonment(arena, playerUuid, reason);
        record(playerUuid, arena, DisciplinePolicy.Category.ABANDONMENT,
                reason == null ? "ABANDONMENT" : reason, true);
    }

    public @Nullable DisciplineStore.Status cachedStatus(UUID playerUuid) {
        return playerUuid == null ? null : cache.get(playerUuid);
    }

    private void record(UUID playerUuid, IArena arena, DisciplinePolicy.Category category,
                        String reason, boolean notifyPlayer) {
        if (closed) return;
        UUID matchUuid = sourceMatchUuid(arena);
        Instant now = Instant.now();
        CompletableFuture<DisciplineStore.PenaltyResult> future = store.record(
                playerUuid, matchUuid, category, reason, policy, now);
        future.whenComplete((result, failure) -> {
            if (failure != null || result == null || !result.applied()) return;
            cache.put(playerUuid, result.status());
            if (result.decision().shouldPunish()) {
                plugin.getLogger().warning("[纪律] 玩家 " + playerUuid + " 的 " + category
                        + " 第 " + result.occurrence() + " 次记录，冷却 "
                        + result.decision().cooldownSeconds() + " 秒，原因：" + reason);
            }
            if (!notifyPlayer) return;
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (result.decision().shouldPunish()) {
                    AdventureText.send(player, getMsg(player, Messages.DISCIPLINE_ABANDONED)
                            .replace("{seconds}", String.valueOf(result.decision().cooldownSeconds())));
                }
            });
        });
    }

    private boolean denyIfBlocked(Player player) {
        if (!enabled || player == null) return false;
        DisciplineStore.Status status = cache.get(player.getUniqueId());
        if (status == null || !status.blockedAt(Instant.now())) return false;
        AdventureText.send(player, getMsg(player, Messages.DISCIPLINE_COOLDOWN)
                .replace("{seconds}", String.valueOf(status.remainingSeconds(Instant.now())))
                .replace("{reason}", status.cooldownReason() == null ? "纪律处罚" : status.cooldownReason()));
        return true;
    }

    private UUID sourceMatchUuid(IArena arena) {
        MatchStatsRecorder recorder = plugin.getMatchStatsRecorder();
        if (recorder != null) {
            UUID matchUuid = recorder.matchUuid(arena);
            if (matchUuid != null) return matchUuid;
        }
        if (arena.getStartTime() == null) return UUID.randomUUID();
        String seed = arena.getArenaName() + ":" + arena.getStartTime();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private ZoneId configuredZone() {
        String configured = BedWars.config.getYml().getString(ConfigPath.MATCH_STATISTICS_TIMEZONE,
                DEFAULT_TIMEZONE);
        try {
            return ZoneId.of(configured == null || configured.isBlank() ? DEFAULT_TIMEZONE : configured);
        } catch (DateTimeException exception) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    private long positive(String path, long fallback) {
        return Math.max(1L, BedWars.config.getYml().getLong(path, fallback));
    }

    private DisciplinePolicy readPolicy() {
        List<Integer> afk = readCooldowns(ConfigPath.DISCIPLINE_AFK_COOLDOWNS,
                Arrays.asList(0, 600, 3600, 86400));
        List<Integer> abandonment = readCooldowns(ConfigPath.DISCIPLINE_ABANDONMENT_COOLDOWNS,
                Arrays.asList(300, 900, 3600, 86400));
        List<Integer> violation = readCooldowns(ConfigPath.DISCIPLINE_VIOLATION_COOLDOWNS,
                Collections.singletonList(1800));
        return new DisciplinePolicy(
                new DisciplinePolicy.Rule(new int[]{1, 2, 3, 5}, afk.stream().mapToLong(Integer::longValue).toArray()),
                new DisciplinePolicy.Rule(new int[]{1, 2, 3, 5}, abandonment.stream().mapToLong(Integer::longValue).toArray()),
                new DisciplinePolicy.Rule(thresholdsFor(violation.size()), violation.stream().mapToLong(Integer::longValue).toArray()));
    }

    private List<Integer> readCooldowns(String path, List<Integer> fallback) {
        List<Integer> values = BedWars.config.getYml().getIntegerList(path);
        if (values.size() != 4 && path.equals(ConfigPath.DISCIPLINE_AFK_COOLDOWNS)) return fallback;
        if (values.size() != 4 && path.equals(ConfigPath.DISCIPLINE_ABANDONMENT_COOLDOWNS)) return fallback;
        if (values.isEmpty() || values.stream().anyMatch(value -> value < 0)) return fallback;
        return List.copyOf(values);
    }

    private static int[] thresholdsFor(int size) {
        int[] thresholds = new int[Math.max(1, size)];
        for (int index = 0; index < thresholds.length; index++) thresholds[index] = index + 1;
        return thresholds;
    }

    @Override
    public void close() {
        closed = true;
        cache.clear();
        store.close();
    }
}
