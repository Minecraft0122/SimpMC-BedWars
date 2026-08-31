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
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.events.player.PlayerReJoinEvent;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Evidence-based detector for cross-team cooperation and kill boosting.
 *
 * <p>This class deliberately does not punish from one raw event. It combines
 * short-lived, successful combat observations on the Bukkit thread and sends
 * only immutable VL updates to {@link MatchStatsRecorder}. The heuristics are
 * intentionally conservative: ordinary proximity, bridge crossings and
 * same-target attacks remain zero-weight evidence.</p>
 */
public final class IllegalTeamDetector implements Listener, AutoCloseable {

    private static final long COMBAT_WINDOW_MILLIS = 15_000L;
    private static final long PROXIMITY_REQUIRED_MILLIS = 8_000L;
    private static final long PROXIMITY_ATTACK_RESET_MILLIS = 8_000L;
    private static final long JOINT_ATTACK_WINDOW_MILLIS = 5_000L;
    private static final long RESCUE_WINDOW_MILLIS = 4_000L;
    private static final long RESOURCE_TRANSFER_WINDOW_MILLIS = 30_000L;
    private static final long FEED_WINDOW_MILLIS = 120_000L;
    private static final long CLEANUP_WINDOW_MILLIS = 60_000L;
    private static final double PROXIMITY_SQUARED = 36.0D;
    private static final double RESCUE_DISTANCE_SQUARED = 64.0D;

    private final BedWars plugin;
    private final MatchStatsRecorder recorder;
    private final boolean enabled;
    private final int matchLeaveVlThreshold;
    private final boolean crossTeamItemTransfer;
    private final Map<IArena, ArenaEvidence> arenas = new IdentityHashMap<>();
    private BukkitTask scanTask;
    private boolean closed;

    IllegalTeamDetector(BedWars plugin, MatchStatsRecorder recorder, boolean enabled,
                        int matchLeaveVlThreshold, boolean crossTeamItemTransfer) {
        this.plugin = plugin;
        this.recorder = recorder;
        this.enabled = enabled;
        this.matchLeaveVlThreshold = Math.max(0, matchLeaveVlThreshold);
        this.crossTeamItemTransfer = crossTeamItemTransfer;
    }

    void start() {
        if (!enabled || closed || scanTask != null) return;
        scanTask = Bukkit.getScheduler().runTaskTimer(plugin, this::scanProximity,
                20L, 20L);
    }

    void matchStarted(IArena arena, MatchRecord record) {
        if (!enabled || arena == null || record == null) return;
        arenas.put(arena, new ArenaEvidence(record));
    }

    void matchFinished(MatchRecord record) {
        if (record == null) return;
        arenas.entrySet().removeIf(entry -> entry.getValue().record == record);
    }

    /** Invoked after a positive VL update, including updates from API users. */
    void scoreChanged(IArena arena, UUID playerUuid, int currentVl, int increment, String rule) {
        if (!enabled || increment <= 0 || currentVl <= matchLeaveVlThreshold) return;
        recorder.ejectForViolation(arena, playerUuid, currentVl, rule);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled || event.getFinalDamage() <= 0.0D) return;
        Player attacker = responsiblePlayer(event.getDamager());
        if (attacker == null || !(event.getEntity() instanceof Player victim)) return;
        IArena arena = Arena.getArenaByPlayer(attacker);
        if (arena == null || Arena.getArenaByPlayer(victim) != arena) return;
        ArenaEvidence state = activeState(arena);
        if (state == null || !isActivePlayer(arena, attacker) || !isActivePlayer(arena, victim)) return;

        long now = System.currentTimeMillis();
        UUID attackerUuid = attacker.getUniqueId();
        UUID victimUuid = victim.getUniqueId();
        String attackerTeam = team(arena, attackerUuid);
        String victimTeam = team(arena, victimUuid);
        if (!enemyTeams(attackerTeam, victimTeam)) return;

        state.lastEnemyDamageAt.put(attackerUuid, now);
        state.lastEnemyDamageAt.put(victimUuid, now);
        state.lastDirectedDamage.put(new DirectedKey(attackerUuid, victimUuid), now);
        state.lastTargetByPlayer.put(attackerUuid, victimUuid);
        state.lastTargetAt.put(attackerUuid, now);
        state.targetAttackers.computeIfAbsent(victimUuid, ignored -> new HashMap<>())
                .put(attackerUuid, now);

        recordMutualDamageEvidence(state, arena, attackerUuid, victimUuid, now);
        recordJointAttackEvidence(state, arena, attackerUuid, victimUuid, now);
        recordRescueEvidence(state, arena, attackerUuid, victimUuid, now);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(PlayerKillEvent event) {
        if (!enabled) return;
        Player killer = event.getKiller();
        Player victim = event.getVictim();
        if (killer == null || victim == null || killer.getUniqueId().equals(victim.getUniqueId())) return;
        IArena arena = event.getArena();
        ArenaEvidence state = activeState(arena);
        if (state == null) return;
        UUID killerUuid = killer.getUniqueId();
        UUID victimUuid = victim.getUniqueId();
        String killerTeam = team(arena, killerUuid);
        String victimTeam = team(arena, victimUuid);
        if (!enemyTeams(killerTeam, victimTeam)) return;

        long now = System.currentTimeMillis();
        DirectedKey victimHitKiller = new DirectedKey(victimUuid, killerUuid);
        boolean resisted = recent(state.lastDirectedDamage.get(victimHitKiller), now, COMBAT_WINDOW_MILLIS);
        FeedKey feedKey = new FeedKey(killerUuid, victimUuid);
        FeedState feed = state.feedStates.computeIfAbsent(feedKey, ignored -> new FeedState());
        if (!resisted && recentOrZero(feed.lastSeenAt, now, FEED_WINDOW_MILLIS)) {
            feed.count++;
            if (feed.count == 2) {
                addBoth(state, arena, killerUuid, victimUuid, 3, true,
                        "DEFENSELESS_FEED", "repeat=2");
                feed.awarded = 3;
            } else if (feed.count == 4 && feed.awarded < 4) {
                addBoth(state, arena, killerUuid, victimUuid, 1, true,
                        "DEFENSELESS_FEED", "repeat=4");
                feed.awarded = 4;
            }
        }
        feed.lastSeenAt = now;

        DirectedKey reverseKill = new DirectedKey(victimUuid, killerUuid);
        if (recent(state.lastKillAt.get(reverseKill), now, FEED_WINDOW_MILLIS)
                && state.mutualKillEvidence.add(new PlayerPair(killerUuid, victimUuid))) {
            addEvidenceBoth(state, arena, killerUuid, victimUuid, -2,
                    "MUTUAL_KILLS", "window_ms=" + FEED_WINDOW_MILLIS);
        }
        state.lastKillAt.put(new DirectedKey(killerUuid, victimUuid), now);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRejoin(PlayerReJoinEvent event) {
        if (!enabled) return;
        clearPlayer(event.getArena(), event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeave(PlayerLeaveArenaEvent event) {
        if (!enabled) return;
        clearPlayer(event.getArena(), event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena != null) clearPlayer(arena, player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedBreak(PlayerBedBreakEvent event) {
        if (!enabled) return;
        IArena arena = event.getArena();
        ArenaEvidence state = activeState(arena);
        if (state == null || event.getPlayerTeam() == null || event.getVictimTeam() == null) return;
        String breaker = event.getPlayerTeam().getName();
        String victim = event.getVictimTeam().getName();
        if (breaker == null || victim == null || breaker.equalsIgnoreCase(victim)) return;
        TeamDirection direction = new TeamDirection(breaker, victim);
        TeamDirection reverse = new TeamDirection(victim, breaker);
        UUID currentBreaker = event.getPlayer().getUniqueId();
        UUID previousBreaker = state.bedBreakers.get(reverse);
        state.bedBreakers.put(direction, currentBreaker);
        if (previousBreaker != null && !previousBreaker.equals(currentBreaker)
                && state.mutualBedEvidence.add(direction.canonical())) {
            recorder.addDetectedEvidence(arena, previousBreaker, -4,
                    "MUTUAL_BED_ATTACK", "teams=" + direction.canonical());
            recorder.addDetectedEvidence(arena, currentBreaker, -4,
                    "MUTUAL_BED_ATTACK", "teams=" + direction.canonical());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled || !crossTeamItemTransfer) return;
        Player player = event.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        ArenaEvidence state = activeState(arena);
        if (state == null || !isActivePlayer(arena, player)) return;
        Item item = event.getItemDrop();
        ItemStack stack = item.getItemStack();
        if (!isTransferResource(stack)) return;
        state.drops.put(item.getUniqueId(), new DropInfo(player.getUniqueId(),
                team(arena, player.getUniqueId()), stack.getType(), stack.getAmount(),
                System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!enabled || !crossTeamItemTransfer || !(event.getEntity() instanceof Player player)) return;
        IArena arena = Arena.getArenaByPlayer(player);
        ArenaEvidence state = activeState(arena);
        if (state == null || !isActivePlayer(arena, player)) return;
        DropInfo drop = state.drops.remove(event.getItem().getUniqueId());
        if (drop == null || drop.ownerUuid.equals(player.getUniqueId())
                || !enemyTeams(drop.teamId, team(arena, player.getUniqueId()))) return;
        long now = System.currentTimeMillis();
        TransferState transfer = state.transfers.computeIfAbsent(
                new PlayerPair(drop.ownerUuid, player.getUniqueId()), ignored -> new TransferState());
        if (recentOrZero(transfer.lastSeenAt, now, RESOURCE_TRANSFER_WINDOW_MILLIS)) {
            transfer.count++;
            if (transfer.count == 2 && !transfer.awarded) {
                addBoth(state, arena, drop.ownerUuid, player.getUniqueId(), 5, false,
                        "REPEATED_RESOURCE_TRANSFER", "material=" + drop.material.name()
                                + ";repeat=2");
                transfer.awarded = true;
            }
        } else {
            transfer.count = 1;
            transfer.awarded = false;
        }
        transfer.lastSeenAt = now;
    }

    private void scanProximity() {
        if (closed || !enabled) return;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<IArena, ArenaEvidence>> iterator = arenas.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<IArena, ArenaEvidence> entry = iterator.next();
            IArena arena = entry.getKey();
            ArenaEvidence state = entry.getValue();
            MatchRecord current = recorder.recordForDetector(arena);
            if (current == null || current != state.record || current.isFinished()
                    || arena.getStatus() != GameState.playing) {
                iterator.remove();
                continue;
            }
            state.cleanup(now);
            List<Player> players = new ArrayList<>();
            for (Player player : arena.getPlayersSnapshot()) {
                if (isActivePlayer(arena, player)) players.add(player);
            }
            Set<PlayerPair> seen = new HashSet<>();
            for (int first = 0; first < players.size(); first++) {
                Player left = players.get(first);
                for (int second = first + 1; second < players.size(); second++) {
                    Player right = players.get(second);
                    UUID leftUuid = left.getUniqueId();
                    UUID rightUuid = right.getUniqueId();
                    if (!enemyTeams(team(arena, leftUuid), team(arena, rightUuid))) continue;
                    PlayerPair pair = new PlayerPair(leftUuid, rightUuid);
                    seen.add(pair);
                    boolean combat = recent(state.lastEnemyDamageAt.get(leftUuid), now, COMBAT_WINDOW_MILLIS)
                            && recent(state.lastEnemyDamageAt.get(rightUuid), now, COMBAT_WINDOW_MILLIS);
                    boolean close = sameWorld(left, right)
                            && left.getLocation().distanceSquared(right.getLocation()) <= PROXIMITY_SQUARED;
                    boolean mutualAttack = recent(state.lastDirectedDamage.get(new DirectedKey(leftUuid, rightUuid)),
                            now, PROXIMITY_ATTACK_RESET_MILLIS)
                            || recent(state.lastDirectedDamage.get(new DirectedKey(rightUuid, leftUuid)),
                            now, PROXIMITY_ATTACK_RESET_MILLIS);
                    if (combat && close && !mutualAttack) {
                        long since = state.nearSince.computeIfAbsent(pair, ignored -> now);
                        if (now - since >= PROXIMITY_REQUIRED_MILLIS
                                && state.nearAwarded.add(pair)) {
                            addBoth(state, arena, leftUuid, rightUuid, 2, false,
                                    "COMBAT_PROXIMITY_NO_ATTACK", "distance=6;duration_seconds=8");
                        }
                    } else {
                        state.nearSince.remove(pair);
                        state.nearAwarded.remove(pair);
                    }
                }
            }
            state.nearSince.keySet().removeIf(pair -> !seen.contains(pair));
            state.nearAwarded.retainAll(seen);
        }
    }

    private void recordMutualDamageEvidence(ArenaEvidence state, IArena arena,
                                            UUID attacker, UUID victim, long now) {
        DirectedKey reverse = new DirectedKey(victim, attacker);
        if (recent(state.lastDirectedDamage.get(reverse), now, COMBAT_WINDOW_MILLIS)) {
            PlayerPair pair = new PlayerPair(attacker, victim);
            Long previous = state.mutualDamageEvidence.get(pair);
            if (previous == null || now - previous > CLEANUP_WINDOW_MILLIS) {
                state.mutualDamageEvidence.put(pair, now);
                addEvidenceBoth(state, arena, attacker, victim, -3,
                        "MUTUAL_DAMAGE", "window_seconds=15");
            }
        }
    }

    private void recordJointAttackEvidence(ArenaEvidence state, IArena arena,
                                           UUID attacker, UUID victim, long now) {
        String victimTeam = team(arena, victim);
        String attackerTeam = team(arena, attacker);
        Map<UUID, Long> attackers = state.targetAttackers.get(victim);
        if (attackers == null) return;
        attackers.entrySet().removeIf(entry -> now - entry.getValue() > JOINT_ATTACK_WINDOW_MILLIS);
        for (Map.Entry<UUID, Long> entry : attackers.entrySet()) {
            UUID other = entry.getKey();
            if (other.equals(attacker) || !recent(entry.getValue(), now, JOINT_ATTACK_WINDOW_MILLIS)) continue;
            String otherTeam = team(arena, other);
            if (!enemyTeams(attackerTeam, otherTeam) || !enemyTeams(otherTeam, victimTeam)
                    || !enemyTeams(attackerTeam, victimTeam)) continue;
            JointKey key = new JointKey(attacker, other, victim);
            JointState joint = state.jointStates.computeIfAbsent(key, ignored -> new JointState());
            joint.lastSeenAt = now;
            joint.count++;
            int amount = joint.count == 2 || joint.count == 4 ? 1 : 0;
            if (amount > 0 && joint.awarded < 2) {
                addBoth(state, arena, attacker, other, amount, false,
                        "REPEATED_JOINT_ATTACK", "target=" + victim + ";repeat=" + joint.count);
                joint.awarded += amount;
            }
        }
    }

    private void recordRescueEvidence(ArenaEvidence state, IArena arena,
                                      UUID attacker, UUID victim, long now) {
        UUID pursuer = state.lastTargetByPlayer.get(victim);
        Long pursuedAt = state.lastTargetAt.get(victim);
        if (pursuer == null || pursuer.equals(attacker)
                || !recent(pursuedAt, now, RESCUE_WINDOW_MILLIS)) return;
        String attackerTeam = team(arena, attacker);
        String pursuerTeam = team(arena, pursuer);
        String victimTeam = team(arena, victim);
        if (!enemyTeams(attackerTeam, pursuerTeam) || !enemyTeams(victimTeam, pursuerTeam)
                || !enemyTeams(attackerTeam, victimTeam)) return;
        Player attackerPlayer = Bukkit.getPlayer(attacker);
        Player victimPlayer = Bukkit.getPlayer(victim);
        if (attackerPlayer == null || victimPlayer == null || !sameWorld(attackerPlayer, victimPlayer)
                || attackerPlayer.getLocation().distanceSquared(victimPlayer.getLocation()) > RESCUE_DISTANCE_SQUARED) return;
        RescueKey key = new RescueKey(attacker, pursuer);
        RescueState rescue = state.rescueStates.computeIfAbsent(key, ignored -> new RescueState());
        if (!recentOrZero(rescue.lastSeenAt, now, RESCUE_WINDOW_MILLIS)) rescue.count = 0;
        rescue.lastSeenAt = now;
        rescue.count++;
        if (rescue.count == 2) {
            addBoth(state, arena, attacker, pursuer, 2, false,
                    "REPEATED_RESCUE_INTERFERENCE", "target=" + victim + ";repeat=2");
        }
    }

    private void clearPlayer(IArena arena, UUID playerUuid) {
        if (arena == null || playerUuid == null) return;
        ArenaEvidence state = arenas.get(arena);
        if (state == null) return;
        state.lastEnemyDamageAt.remove(playerUuid);
        state.lastTargetByPlayer.remove(playerUuid);
        state.lastTargetAt.remove(playerUuid);
        state.lastKillAt.entrySet().removeIf(entry -> entry.getKey().contains(playerUuid));
        state.lastDirectedDamage.entrySet().removeIf(entry -> entry.getKey().contains(playerUuid));
        state.targetAttackers.values().forEach(map -> map.remove(playerUuid));
        state.targetAttackers.entrySet().removeIf(entry -> entry.getValue().isEmpty()
                || entry.getKey().equals(playerUuid));
        state.nearSince.keySet().removeIf(pair -> pair.contains(playerUuid));
        state.nearAwarded.removeIf(pair -> pair.contains(playerUuid));
        state.mutualDamageEvidence.keySet().removeIf(pair -> pair.contains(playerUuid));
        state.jointStates.keySet().removeIf(key -> key.attackers().contains(playerUuid)
                || key.target().equals(playerUuid));
        state.rescueStates.keySet().removeIf(key -> key.rescuer().equals(playerUuid)
                || key.pursuer().equals(playerUuid));
        state.feedStates.keySet().removeIf(key -> key.killer().equals(playerUuid)
                || key.victim().equals(playerUuid));
        state.mutualKillEvidence.removeIf(pair -> pair.contains(playerUuid));
        state.drops.entrySet().removeIf(entry -> entry.getValue().ownerUuid.equals(playerUuid));
        state.transfers.keySet().removeIf(pair -> pair.contains(playerUuid));
    }

    private void addBoth(ArenaEvidence state, IArena arena, UUID first, UUID second,
                         int amount, boolean killBoosting, String rule, String details) {
        recorder.addDetectedViolation(arena, first, amount, killBoosting, rule, details);
        recorder.addDetectedViolation(arena, second, amount, killBoosting, rule, details);
    }

    private void addEvidenceBoth(ArenaEvidence state, IArena arena, UUID first, UUID second,
                                 int amount, String rule, String details) {
        recorder.addDetectedEvidence(arena, first, amount, rule, details);
        recorder.addDetectedEvidence(arena, second, amount, rule, details);
    }

    @Nullable
    private ArenaEvidence activeState(IArena arena) {
        if (!enabled || arena == null || arena.getStatus() != GameState.playing) return null;
        MatchRecord record = recorder.recordForDetector(arena);
        if (record == null || record.isFinished()) return null;
        ArenaEvidence state = arenas.get(arena);
        if (state == null || state.record != record) {
            state = new ArenaEvidence(record);
            arenas.put(arena, state);
        }
        return state;
    }

    private boolean isActivePlayer(IArena arena, Player player) {
        return player != null && player.isOnline() && arena.isPlayer(player)
                && !arena.isSpectator(player) && !arena.isReSpawning(player.getUniqueId());
    }

    @Nullable
    private static Player responsiblePlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @Nullable
    private static String team(IArena arena, UUID playerUuid) {
        return MatchStatsRecorder.teamId(arena, playerUuid);
    }

    private static boolean enemyTeams(@Nullable String first, @Nullable String second) {
        return first != null && second != null && !first.equalsIgnoreCase(second);
    }

    private static boolean recent(@Nullable Long timestamp, long now, long window) {
        return timestamp != null && now >= timestamp && now - timestamp <= window;
    }

    private static boolean recentOrZero(long timestamp, long now, long window) {
        return timestamp == 0L || recent(timestamp, now, window);
    }

    private static boolean sameWorld(Player first, Player second) {
        return first.getWorld() == second.getWorld();
    }

    private static boolean isTransferResource(ItemStack stack) {
        Material material = stack.getType();
        return material == Material.IRON_INGOT || material == Material.GOLD_INGOT
                || material == Material.DIAMOND || material == Material.EMERALD;
    }

    @Override
    public void close() {
        closed = true;
        if (scanTask != null) scanTask.cancel();
        scanTask = null;
        arenas.clear();
    }

    private static final class ArenaEvidence {
        private final MatchRecord record;
        private final Map<UUID, Long> lastEnemyDamageAt = new HashMap<>();
        private final Map<DirectedKey, Long> lastDirectedDamage = new HashMap<>();
        private final Map<DirectedKey, Long> lastKillAt = new HashMap<>();
        private final Map<UUID, UUID> lastTargetByPlayer = new HashMap<>();
        private final Map<UUID, Long> lastTargetAt = new HashMap<>();
        private final Map<UUID, Map<UUID, Long>> targetAttackers = new HashMap<>();
        private final Map<PlayerPair, Long> nearSince = new HashMap<>();
        private final Set<PlayerPair> nearAwarded = new HashSet<>();
        private final Map<PlayerPair, Long> mutualDamageEvidence = new HashMap<>();
        private final Map<JointKey, JointState> jointStates = new HashMap<>();
        private final Map<RescueKey, RescueState> rescueStates = new HashMap<>();
        private final Map<FeedKey, FeedState> feedStates = new HashMap<>();
        private final Set<PlayerPair> mutualKillEvidence = new HashSet<>();
        private final Map<TeamDirection, UUID> bedBreakers = new HashMap<>();
        private final Set<TeamPair> mutualBedEvidence = new HashSet<>();
        private final Map<UUID, DropInfo> drops = new HashMap<>();
        private final Map<PlayerPair, TransferState> transfers = new HashMap<>();

        private ArenaEvidence(MatchRecord record) {
            this.record = record;
        }

        private void cleanup(long now) {
            lastEnemyDamageAt.entrySet().removeIf(entry -> now - entry.getValue() > CLEANUP_WINDOW_MILLIS);
            lastDirectedDamage.entrySet().removeIf(entry -> now - entry.getValue() > CLEANUP_WINDOW_MILLIS);
            lastKillAt.entrySet().removeIf(entry -> now - entry.getValue() > FEED_WINDOW_MILLIS);
            lastTargetAt.entrySet().removeIf(entry -> now - entry.getValue() > CLEANUP_WINDOW_MILLIS);
            lastTargetByPlayer.keySet().removeIf(uuid -> !lastTargetAt.containsKey(uuid));
            targetAttackers.values().forEach(map -> map.entrySet()
                    .removeIf(entry -> now - entry.getValue() > JOINT_ATTACK_WINDOW_MILLIS));
            targetAttackers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            mutualDamageEvidence.entrySet().removeIf(entry -> now - entry.getValue() > CLEANUP_WINDOW_MILLIS);
            jointStates.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAt > CLEANUP_WINDOW_MILLIS);
            rescueStates.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAt > CLEANUP_WINDOW_MILLIS);
            feedStates.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAt > FEED_WINDOW_MILLIS);
            drops.entrySet().removeIf(entry -> now - entry.getValue().droppedAt > RESOURCE_TRANSFER_WINDOW_MILLIS);
            transfers.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAt > RESOURCE_TRANSFER_WINDOW_MILLIS);
        }
    }

    private static final class JointState {
        private int count;
        private int awarded;
        private long lastSeenAt;
    }

    private static final class RescueState {
        private int count;
        private long lastSeenAt;
    }

    private static final class FeedState {
        private int count;
        private int awarded;
        private long lastSeenAt;
    }

    private static final class TransferState {
        private int count;
        private boolean awarded;
        private long lastSeenAt;
    }

    private record DropInfo(UUID ownerUuid, String teamId, Material material, int amount, long droppedAt) {
    }

    private record PlayerPair(UUID first, UUID second) {
        private PlayerPair {
            if (first.compareTo(second) > 0) {
                UUID swap = first;
                first = second;
                second = swap;
            }
        }

        private boolean contains(UUID uuid) {
            return first.equals(uuid) || second.equals(uuid);
        }
    }

    private record DirectedKey(UUID attacker, UUID victim) {
        private boolean contains(UUID uuid) {
            return attacker.equals(uuid) || victim.equals(uuid);
        }
    }

    private record FeedKey(UUID killer, UUID victim) {
    }

    private record RescueKey(UUID rescuer, UUID pursuer) {
    }

    private record JointKey(PlayerPair attackers, UUID target) {
        private JointKey(UUID first, UUID second, UUID target) {
            this(new PlayerPair(first, second), target);
        }

        private boolean contains(UUID uuid) {
            return attackers.contains(uuid) || target.equals(uuid);
        }
    }

    private record TeamPair(String first, String second) {
        private TeamPair {
            if (first.compareToIgnoreCase(second) > 0) {
                String swap = first;
                first = second;
                second = swap;
            }
        }

        private TeamPair canonical() {
            return new TeamPair(first.toLowerCase(java.util.Locale.ROOT),
                    second.toLowerCase(java.util.Locale.ROOT));
        }
    }

    private record TeamDirection(String from, String to) {
        private TeamDirection {
            from = from.toLowerCase(java.util.Locale.ROOT);
            to = to.toLowerCase(java.util.Locale.ROOT);
        }

        private TeamPair canonical() {
            return new TeamPair(from, to);
        }
    }
}
